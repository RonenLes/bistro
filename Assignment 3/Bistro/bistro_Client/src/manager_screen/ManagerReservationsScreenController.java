package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.ReservationResponse;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JavaFX controller for the manager screen that displays reservations for a selected date.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Allows a manager to pick a {@link LocalDate} and request reservations for that date from the server.</li>
 *   <li>Displays the returned reservations in a {@link TableView}.</li>
 *   <li>Exposes a callback hook ({@link #setOnStartReservationFlow(Runnable)}) that lets a parent controller
 *       open the reservation creation flow (e.g., when pressing "Make Reservation").</li>
 * </ul>
 * <p>
 * Data flow:
 * <ol>
 *   <li>User selects a date and clicks "Load".</li>
 *   <li>{@link #onLoad()} sends {@link ManagerCommand#VIEW_RESERVATIONS} to the server.</li>
 *   <li>Parent UI receives a {@link ManagerResponse} and forwards it to {@link #handleManagerResponse(ManagerResponse)}.</li>
 *   <li>Reservations list is populated and the table updates automatically.</li>
 * </ol>
 */
public class ManagerReservationsScreenController implements ClientControllerAware {

    /** Date picker used to choose which day to load reservations for. */
    @FXML private DatePicker datePicker;

    /** Table that displays reservation rows returned by the server. */
    @FXML private TableView<ReservationResponse> reservationsTable;

    /** Column showing the reservation contact (guest contact). */
    @FXML private TableColumn<ReservationResponse, String> colContact;

    /** Column showing the reservation time. */
    @FXML private TableColumn<ReservationResponse, LocalTime> colTime;

    /** Column showing party size. */
    @FXML private TableColumn<ReservationResponse, Integer> colParty;

    /** Column showing confirmation code. */
    @FXML private TableColumn<ReservationResponse, Integer> colCode;

    /** Label used to display status messages and validation feedback. */
    @FXML private Label infoLabel;

    /** Reference to the shared client controller for server communication (injected by parent controller). */
    private ClientController clientController;

    /** Whether the client is currently connected to the server (injected by parent controller). */
    private boolean connected;

    /** Observable list backing {@link #reservationsTable}. */
    private final ObservableList<ReservationResponse> reservations = FXCollections.observableArrayList();

    /**
     * Callback invoked when the user wants to start the reservation creation flow.
     * <p>
     * This is typically provided by a parent controller (e.g., DesktopScreenController) to switch tabs/views.
     */
    private Runnable onStartReservationFlow;

    /**
     * Initializes the screen after FXML injection.
     * <p>
     * Wires table columns to {@link ReservationResponse} properties and binds the observable list to the table.
     */
    @FXML
    private void initialize() {
        if (colContact != null) {
            colContact.setCellValueFactory(new PropertyValueFactory<>("newGuestContact"));
        }

        if (colTime != null) {
            colTime.setCellValueFactory(new PropertyValueFactory<>("newTime"));
        }

        if (colParty != null) {
            colParty.setCellValueFactory(new PropertyValueFactory<>("newPartySize"));
        }

        if (colCode != null) {
            colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        }

        if (reservationsTable != null) {
            reservationsTable.setItems(reservations);
        }

        setInfo("Select a date to load reservations.");
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this controller.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * Button handler that triggers the reservation creation flow.
     * <p>
     * Delegates navigation to the callback provided via {@link #setOnStartReservationFlow(Runnable)}.
     */
    @FXML
    private void onOpenReservationFlow() {
        if (onStartReservationFlow != null) {
            onStartReservationFlow.run();
        }
    }

    /**
     * Button handler that loads reservations for the selected date.
     * <p>
     * Validates that a date was chosen and that the client is connected, then sends
     * {@link ManagerCommand#VIEW_RESERVATIONS} request to the server.
     */
    @FXML
    private void onLoad() {
        if (!readyForServer()) return;

        LocalDate date = (datePicker == null) ? null : datePicker.getValue();
        if (date == null) {
            setInfo("Please select a date.");
            return;
        }

        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_RESERVATIONS, date));
        setInfo("Loading reservations...");
    }

    /**
     * Populates the table with reservation data returned by the server.
     * <p>
     * Only handles {@link ManagerResponseCommand#RESERVATION_BY_DATE_RESPONSE}; other responses are ignored.
     *
     * @param response manager response forwarded from the parent controller
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;
        if (response.getResponseCommand() != ManagerResponseCommand.RESERVATION_BY_DATE_RESPONSE) return;

        reservations.clear();

        if (response.getTables() != null) {
            for (Object item : response.getTables()) {
                if (item instanceof ReservationResponse reservation) {
                    reservations.add(reservation);
                }
            }
        }
    }

    /**
     * Sets the callback that is triggered when the user clicks the UI action to start reservation creation.
     *
     * @param onStartReservationFlow callback to run (may be {@code null})
     */
    public void setOnStartReservationFlow(Runnable onStartReservationFlow) {
        this.onStartReservationFlow = onStartReservationFlow;
    }

    /**
     * Checks whether the controller is connected and ready to send requests to the server.
     *
     * @return {@code true} if connected and {@link #clientController} is set; otherwise {@code false}
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Updates the info label.
     *
     * @param msg message to display (null-safe)
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
