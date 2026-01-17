package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.CurrentSeatingResponse;
import responses.LoginResponse;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.WaitingListResponse;

import java.time.LocalDateTime;

/**
 * JavaFX controller for the manager "Show Data" screen.
 * <p>
 * Displays real-time restaurant data for managers, including:
 * <ul>
 *   <li>The current waiting list</li>
 *   <li>The current seating status (active seatings)</li>
 * </ul>
 * Data is fetched from the server on demand (e.g., via refresh) using {@link ManagerRequest} commands.
 * </p>
 * <p>
 * This controller is expected to receive a {@link ClientController} instance via
 * {@link #setClientController(ClientController, boolean)} (dependency injection).
 * </p>
 */
public class ShowDataScreenController implements ClientControllerAware {

    @FXML private TableView<WaitingListResponse> waitingTable;
    @FXML private TableColumn<WaitingListResponse, String> colWaitingPriority;
    @FXML private TableColumn<WaitingListResponse, LocalDateTime> colWaitingTime;
    @FXML private TableColumn<WaitingListResponse, String> colWaitingContact;

    @FXML private TableView<SeatingRow> seatingTable;
    @FXML private TableColumn<SeatingRow, Integer> colSeatingTable;
    @FXML private TableColumn<SeatingRow, Integer> colSeatingParty;
    @FXML private TableColumn<SeatingRow, LocalDateTime> colSeatingCheckIn;
    @FXML private TableColumn<SeatingRow, Integer> colSeatingCode;
    @FXML private TableColumn<SeatingRow, String> colSeatingType;

    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * Backing list for the waiting list table. Changes are reflected in the UI automatically.
     */
    private final javafx.collections.ObservableList<WaitingListResponse> waitingItems =
            javafx.collections.FXCollections.observableArrayList();

    /**
     * Backing list for the seating table. Changes are reflected in the UI automatically.
     */
    private final javafx.collections.ObservableList<SeatingRow> seatingItems =
            javafx.collections.FXCollections.observableArrayList();

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Configures the table columns (property bindings) and assigns the backing {@link ObservableList}s
     * to their respective {@link TableView}s.
     * </p>
     */
    @FXML
    private void initialize() {

        // waiting list table columns
        if (colWaitingPriority != null) colWaitingPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        if (colWaitingTime != null) colWaitingTime.setCellValueFactory(new PropertyValueFactory<>("dateTime"));
        if (colWaitingContact != null) colWaitingContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        if (waitingTable != null) waitingTable.setItems(waitingItems);

        // seating table columns
        if (colSeatingTable != null) colSeatingTable.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        if (colSeatingParty != null) colSeatingParty.setCellValueFactory(new PropertyValueFactory<>("partySize"));
        if (colSeatingCheckIn != null) colSeatingCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkInTime"));
        if (colSeatingCode != null) colSeatingCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        if (colSeatingType != null) colSeatingType.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (seatingTable != null) seatingTable.setItems(seatingItems);

        setInfo("Load manager data to view waiting list and seating.");
    }

    /**
     * Injects the {@link ClientController} used for sending manager requests to the server and
     * indicates whether the client is currently connected.
     *
     * @param controller the application-level client controller used to communicate with the server
     * @param connected  {@code true} if the client is connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * UI handler invoked by the Refresh action in the FXML.
     * <p>
     * Requests both waiting list and current seating data from the server.
     * </p>
     */
    @FXML
    private void onRefresh() {
        requestAll();
    }

    /**
     * Requests initial data when the screen/tab is first opened.
     * <p>
     * Intended to be called by the parent controller (e.g., DesktopScreenController) when the view becomes visible.
     * </p>
     */
    public void requestInitialData() {
        requestAll();
    }

    /**
     * Handles {@link ManagerResponse} messages relevant to this screen.
     * <p>
     * Supported response commands:
     * <ul>
     *   <li>{@link ManagerResponseCommand#CURRENT_WAITING_LIST_RESPONSE} - updates the waiting list table</li>
     *   <li>{@link ManagerResponseCommand#VIEW_CURRENT_SEATING_RESPONSE} - updates the seating table</li>
     * </ul>
     * </p>
     *
     * @param response the server response containing the requested manager data; may be {@code null}
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        ManagerResponseCommand command = response.getResponseCommand();

        if (command == ManagerResponseCommand.CURRENT_WAITING_LIST_RESPONSE) {
            // populate waiting list table
            waitingItems.clear();
            if (response.getTables() != null) {
                for (Object item : response.getTables()) {
                    if (item instanceof WaitingListResponse waiting) {
                        waitingItems.add(waiting);
                    }
                }
            }
            setInfo("Waiting list updated.");
        }

        else if (command == ManagerResponseCommand.VIEW_CURRENT_SEATING_RESPONSE) {
            // populate seating table
            seatingItems.clear();
            if (response.getTables() != null) {
                for (Object item : response.getTables()) {
                    if (item instanceof CurrentSeatingResponse seating) {
                        seatingItems.add(SeatingRow.from(seating));
                    }
                }
            }
            setInfo("Current seating updated.");
        }
    }

    /**
     * Sends both requests required to populate this screen:
     * <ul>
     *   <li>View waiting list</li>
     *   <li>View current seating</li>
     * </ul>
     * If the client is not connected, no requests are sent and the UI is updated with an error message.
     */
    private void requestAll() {
        if (!readyForServer()) return;

        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_WAITING_LIST));
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_CURRENT_SEATING));

        setInfo("Refreshing manager data...");
    }

    /**
     * Validates that the controller is ready to communicate with the server.
     *
     * @return {@code true} if the client is connected and a {@link ClientController} is available; {@code false} otherwise
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Updates the information label displayed on the screen.
     *
     * @param msg message to show to the user; if {@code null}, an empty string is displayed
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * View-model row for the seating table.
     * <p>
     * Converts {@link CurrentSeatingResponse} (server DTO) into display-friendly fields
     * required by the JavaFX {@link TableView}.
     * </p>
     */
    public static class SeatingRow {
        private final int tableNumber;
        private final int partySize;
        private final LocalDateTime checkInTime;
        private final int confirmationCode;
        private final String type;

        /**
         * Creates a row representing one active seating entry in the UI.
         *
         * @param tableNumber       restaurant table number (display value)
         * @param partySize         number of people currently seated
         * @param checkInTime       check-in timestamp for the seating
         * @param confirmationCode  reservation confirmation code associated with the seating
         * @param type              customer type label (e.g., guest/subscriber)
         */
        private SeatingRow(int tableNumber, int partySize, LocalDateTime checkInTime, int confirmationCode, String type) {
            this.tableNumber = tableNumber;
            this.partySize = partySize;
            this.checkInTime = checkInTime;
            this.confirmationCode = confirmationCode;
            this.type = type;
        }

        /**
         * Factory method that converts a {@link CurrentSeatingResponse} object into a {@link SeatingRow}.
         * <p>
         * The "type" is derived from whether a user ID exists:
         * blank/missing user ID is treated as a "Guest", otherwise "Subscriber".
         * </p>
         *
         * @param seating server response object representing one current seating entry
         * @return a {@link SeatingRow} instance suitable for displaying in the seating table
         * @throws NullPointerException if {@code seating} is {@code null}
         */
        public static SeatingRow from(CurrentSeatingResponse seating) {
            int tableNumber = seating.getTable() == null ? 0 : seating.getTable().getTableNumber();
            // determines if customer is guest or subscriber based on user ID presence
            String type = seating.getUserID() == null || seating.getUserID().isBlank() ? "Guest" : "Subscriber";
            return new SeatingRow(
                    tableNumber,
                    seating.getPartySize(),
                    seating.getCheckInTime(),
                    seating.getConfrimationCode(),
                    type
            );
        }

        /**
         * @return restaurant table number for this row
         */
        public int getTableNumber() {
            return tableNumber;
        }

        /**
         * @return party size for this seating
         */
        public int getPartySize() {
            return partySize;
        }

        /**
         * @return check-in time for this seating
         */
        public LocalDateTime getCheckInTime() {
            return checkInTime;
        }

        /**
         * @return reservation confirmation code associated with this seating
         */
        public int getConfirmationCode() {
            return confirmationCode;
        }

        /**
         * @return customer type label (e.g., guest/subscriber)
         */
        public String getType() {
            return type;
        }
    }
}
