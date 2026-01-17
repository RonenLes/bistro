package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.LoginResponse;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.UserHistoryResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JavaFX controller for the manager "Subscribers" screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Display all subscribers in a table.</li>
 *   <li>Allow the manager to request and view reservation history for a selected subscriber.</li>
 *   <li>Render history rows with friendly formatting (date/time/currency) and an inferred status label.</li>
 * </ul>
 * </p>
 * <p>
 * The controller expects a {@link ClientController} to be injected via
 * {@link #setClientController(ClientController, boolean)}.
 * </p>
 */
public class SubscribersScreenController implements ClientControllerAware {

    @FXML private TableView<LoginResponse> subscribersTable;
    @FXML private TableColumn<LoginResponse, String> colSubscriberId;
    @FXML private TableColumn<LoginResponse, String> colSubscriberName;
    @FXML private TableColumn<LoginResponse, String> colSubscriberPhone;
    @FXML private TableColumn<LoginResponse, String> colSubscriberEmail;
    @FXML private TableColumn<LoginResponse, Void> colSubscriberHistory;

    @FXML private TableView<UserHistoryResponse> historyTable;
    @FXML private TableColumn<UserHistoryResponse, String> colDate;
    @FXML private TableColumn<UserHistoryResponse, String> colReservedFor;
    @FXML private TableColumn<UserHistoryResponse, String> colCheckIn;
    @FXML private TableColumn<UserHistoryResponse, String> colCheckOut;
    @FXML private TableColumn<UserHistoryResponse, String> colTable;
    @FXML private TableColumn<UserHistoryResponse, String> colParty;
    @FXML private TableColumn<UserHistoryResponse, String> colTotal;
    @FXML private TableColumn<UserHistoryResponse, String> colStatus;

    @FXML private Label historyTargetLabel;
    @FXML private Label infoLabel;

    /**
     * Backing list for the subscribers table (reactive UI updates).
     */
    private final ObservableList<LoginResponse> subscriberItems = FXCollections.observableArrayList();

    /**
     * Backing list for the subscriber history table (reactive UI updates).
     */
    private final ObservableList<UserHistoryResponse> historyItems = FXCollections.observableArrayList();

    private ClientController clientController;
    private boolean connected;

    /**
     * The username for which history was last requested (used for UI messages and header text).
     */
    private String requestedUsername;

    /**
     * Date format used when rendering reservation dates in the history table.
     */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Time format used when rendering times in the history table.
     */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Initializes:
     * <ul>
     *   <li>Subscribers table columns and data source</li>
     *   <li>"View History" action button column</li>
     *   <li>History table formatting columns and data source</li>
     * </ul>
     * </p>
     */
    @FXML
    private void initialize() {
        if (colSubscriberId != null) colSubscriberId.setCellValueFactory(new PropertyValueFactory<>("userID"));
        if (colSubscriberName != null) colSubscriberName.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colSubscriberPhone != null) colSubscriberPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        if (colSubscriberEmail != null) colSubscriberEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        if (subscribersTable != null) subscribersTable.setItems(subscriberItems);

        if (colSubscriberHistory != null) {
            colSubscriberHistory.setCellFactory(col -> new TableCell<>() {
                private final Button button = new Button("View History");

                {
                    button.getStyleClass().add("ghost");
                    button.setOnAction(event -> {
                        LoginResponse subscriber = getTableView().getItems().get(getIndex());
                        requestHistory(subscriber);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : button);
                }
            });
        }

        initHistoryColumns();
        if (historyTable != null) historyTable.setItems(historyItems);

        setHistoryTarget(null);
        setInfo("Load subscribers to view history.");
    }

    /**
     * Injects the {@link ClientController} used to communicate with the server and sets the current
     * connection state.
     * <p>
     * If the client is already connected, subscribers are requested immediately so the screen will
     * be populated without extra user actions.
     * </p>
     *
     * @param controller the application-level client controller used for server communication
     * @param connected  {@code true} if connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        if (connected) {
            requestSubscribers();
        }
    }

    /**
     * UI handler invoked by a Refresh action in the FXML.
     * <p>
     * Requests the latest subscribers list from the server.
     * </p>
     */
    @FXML
    private void onRefresh() {
        requestSubscribers();
    }

    /**
     * Requests initial data when the screen/tab is opened.
     * <p>
     * Intended to be called by a parent controller when the view becomes visible.
     * </p>
     */
    public void requestInitialData() {
        requestSubscribers();
    }

    /**
     * Handles {@link ManagerResponse} messages relevant to this screen.
     * <p>
     * Currently supports:
     * <ul>
     *   <li>{@link ManagerResponseCommand#ALL_SUBSCRIBERS_RESPONSE} - updates the subscribers table</li>
     * </ul>
     * </p>
     *
     * @param response response received from the server; may be {@code null}
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        if (response.getResponseCommand() == ManagerResponseCommand.ALL_SUBSCRIBERS_RESPONSE) {
            subscriberItems.clear();
            if (response.getTables() != null) {
                for (Object item : response.getTables()) {
                    if (item instanceof LoginResponse subscriber) {
                        subscriberItems.add(subscriber);
                    }
                }
            }
            setInfo("Subscribers updated.");
        }
    }

    /**
     * Renders subscriber history rows into the history table.
     *
     * @param rows history rows to display; may be {@code null}
     */
    public void renderHistory(List<UserHistoryResponse> rows) {
        historyItems.clear();
        if (rows != null) {
            historyItems.addAll(rows);
        }
        String name = requestedUsername == null ? "subscriber" : requestedUsername;
        setInfo(historyItems.isEmpty() ? "No history found for " + name + "." : "History loaded for " + name + ".");
    }

    /**
     * Displays an error state related to loading subscriber history.
     * <p>
     * Clears any previous history rows and shows the provided message (or a default message).
     * </p>
     *
     * @param message error message to display; if {@code null} or blank, a default message is shown
     */
    public void showHistoryError(String message) {
        historyItems.clear();
        setInfo(message == null || message.isBlank() ? "Failed to load history." : message);
    }

    /**
     * Sends a request to the server to retrieve all subscribers.
     * <p>
     * If the client is not connected, no request is sent and the UI is updated accordingly.
     * </p>
     */
    private void requestSubscribers() {
        if (!readyForServer()) return;
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_SUBSCRIBERS));
        setInfo("Refreshing subscribers...");
    }

    /**
     * Sends a request to load history for a selected subscriber.
     * <p>
     * Updates UI state to indicate which subscriber's history is being loaded, clears the current history table,
     * and then delegates the request to {@link ClientController}.
     * </p>
     *
     * @param subscriber selected subscriber row from {@link #subscribersTable}; may be {@code null}
     */
    private void requestHistory(LoginResponse subscriber) {
        if (subscriber == null) {
            setInfo("Select a subscriber first.");
            return;
        }

        String username = subscriber.getUsername();
        username = username == null ? null : username.trim();
        if (username == null || username.isEmpty()) {
            setInfo("Subscriber username is missing.");
            return;
        }

        requestedUsername = username;
        setHistoryTarget(username);
        historyItems.clear();
        setInfo("Loading history for " + username + "...");

        if (!readyForServer()) return;
        clientController.requestUserHistoryForSubscriber(username);
    }

    /**
     * Validates that the controller is ready to communicate with the server.
     *
     * @return {@code true} if connected and a {@link ClientController} instance is available; {@code false} otherwise
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Updates the history header label to reflect the currently selected subscriber.
     *
     * @param username subscriber username; if {@code null} or blank, a default prompt is shown
     */
    private void setHistoryTarget(String username) {
        if (historyTargetLabel != null) {
            if (username == null || username.isBlank()) {
                historyTargetLabel.setText("Select a subscriber");
            } else {
                historyTargetLabel.setText("for " + username);
            }
        }
    }

    /**
     * Initializes history table columns with formatted values.
     * <p>
     * This method formats raw values coming from {@link UserHistoryResponse} into strings for display:
     * dates, times, numeric values, total price, and a human-readable status.
     * </p>
     */
    private void initHistoryColumns() {
        if (colDate != null) {
            colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatDate(cd.getValue() == null ? null : cd.getValue().getReservationDate())
            ));
        }
        if (colReservedFor != null) {
            colReservedFor.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getReservedForTime())
            ));
        }
        if (colCheckIn != null) {
            colCheckIn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getCheckInTime())
            ));
        }
        if (colCheckOut != null) {
            colCheckOut.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getCheckOutTime())
            ));
        }
        if (colTable != null) {
            colTable.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getTableNumber())
            ));
        }
        if (colParty != null) {
            colParty.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getPartySize())
            ));
        }
        if (colTotal != null) {
            colTotal.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTotal(cd.getValue() == null ? null : cd.getValue().getTotalPrice())
            ));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(cd -> new ReadOnlyStringWrapper(inferStatus(cd.getValue())));
        }
    }

    /**
     * Formats a {@link LocalDate} using {@link #DATE_FMT}.
     *
     * @param date date to format; may be {@code null}
     * @return formatted date, or "-" if {@code date} is {@code null}
     */
    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    /**
     * Formats a {@link LocalTime} using {@link #TIME_FMT}.
     *
     * @param time time to format; may be {@code null}
     * @return formatted time, or "-" if {@code time} is {@code null}
     */
    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FMT);
    }

    /**
     * Formats a positive integer value for display.
     *
     * @param value numeric value (e.g., party size or table number)
     * @return the numeric value as a string, or "-" if {@code value <= 0}
     */
    private String formatInt(int value) {
        return value <= 0 ? "-" : String.valueOf(value);
    }

    /**
     * Formats the total price field for display as currency.
     *
     * @param total total price; may be {@code null}
     * @return currency string (e.g., "₪12.50"), or "-" if {@code total} is {@code null}
     */
    private String formatTotal(Double total) {
        if (total == null) return "-";
        return String.format("₪%.2f", total);
    }

    /**
     * Infers a user-friendly status label for a history row based on its fields.
     * <p>
     * Logic:
     * <ul>
     *   <li>If {@code status} equals "CANCELLED" (case-insensitive) -&gt; "Cancelled"</li>
     *   <li>If both check-in and check-out are missing -&gt; "Reserved"</li>
     *   <li>If check-in exists and check-out is missing -&gt; "Seated"</li>
     *   <li>If both check-in and check-out exist -&gt; "Completed"</li>
     * </ul>
     * </p>
     *
     * @param response history row; may be {@code null}
     * @return inferred status label, or "-" if it cannot be inferred
     */
    private String inferStatus(UserHistoryResponse response) {
        if (response == null) return "-";
        String status = response.getStatus();
        if (status != null && status.equalsIgnoreCase("CANCELLED")) return "Cancelled";
        if (response.getCheckInTime() == null && response.getCheckOutTime() == null) return "Reserved";
        if (response.getCheckInTime() != null && response.getCheckOutTime() == null) return "Seated";
        if (response.getCheckInTime() != null && response.getCheckOutTime() != null) return "Completed";
        return "-";
    }

    /**
     * Updates the info label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
