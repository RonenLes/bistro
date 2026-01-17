package subscriber_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import controllers.ClientUIHandler;
import desktop_screen.DesktopScreenController;
import desktop_views.ReservationsViewController;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import responses.ManagerResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;
import responses.WaitingListResponse;
import requests.ReservationRequest.ReservationRequestType;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import requests.BillRequest.BillRequestType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * JavaFX controller for the subscriber main/home screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Display upcoming reservations for the currently logged-in subscriber.</li>
 *   <li>Provide actions per reservation: edit, cancel, and pay (pay is shown only when the reservation is seated).</li>
 *   <li>Display the subscriber's user ID as a simple "QR" popup (currently a dialog with the ID as text).</li>
 *   <li>Support logout and account details viewing/editing.</li>
 * </ul>
 * </p>
 * <p>
 * This controller receives server callbacks via {@link ClientUIHandler} and communicates with the server through
 * an injected {@link ClientController}.
 * </p>
 */
public class SubscriberMainScreenController implements ClientControllerAware, ClientUIHandler {

    @FXML private TableView<ReservationResponse> reservationsTable;
    @FXML private TableColumn<ReservationResponse, String> colDate;
    @FXML private TableColumn<ReservationResponse, String> colStartTime;
    @FXML private TableColumn<ReservationResponse, String> colPartySize;
    @FXML private TableColumn<ReservationResponse, String> colConfirmationCode;
    @FXML private TableColumn<ReservationResponse, Void> colEdit;
    @FXML private TableColumn<ReservationResponse, Void> colCancel;
    @FXML private TableColumn<ReservationResponse, Void> colPay;
    @FXML private Label infoLabel;

    /**
     * Backing list for upcoming reservations shown in {@link #reservationsTable}.
     */
    private final ObservableList<ReservationResponse> reservations = FXCollections.observableArrayList();

    private ClientController clientController;
    private boolean connected;

    /**
     * Callback to execute after logout completes (typically used by a parent controller to route screens).
     */
    private Runnable onLogout;

    /**
     * Guards sending the initial "upcoming reservations" request more than once after connecting.
     */
    private boolean requested;

    /**
     * Optional child controller used when the reservations view is loaded.
     */
    private ReservationsViewController reservationsViewController;

    /**
     * Optional external handler for edit reservation action (navigation responsibility can be delegated).
     */
    private Consumer<ReservationResponse> onEditReservation;

    /**
     * Optional external handler for pay reservation action (navigation responsibility can be delegated).
     */
    private Consumer<ReservationResponse> onPayReservation;

    @FXML private Label qrLabel;

    /**
     * Date format used to display reservation dates.
     */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Time format used to display reservation times.
     */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Configures table columns with formatted display values and initializes action columns (edit/cancel/pay).
     * </p>
     */
    @FXML
    private void initialize() {
        if (colDate != null)
            colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatDate(cd.getValue() == null ? null : cd.getValue().getNewDate())
            ));

        if (colStartTime != null)
            colStartTime.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getNewTime())
            ));

        if (colPartySize != null)
            colPartySize.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getNewPartySize())
            ));

        if (colConfirmationCode != null) {
            colConfirmationCode.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getConfirmationCode())
            ));
        }

        if (colEdit != null) {
            colEdit.setCellFactory(col -> new TableCell<>() {
                private final Button editButton = buildActionButton("Edit");

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : editButton);
                }

                private Button buildActionButton(String label) {
                    Button button = new Button(label);
                    button.getStyleClass().add("ghost");
                    button.setOnAction(event -> {
                        ReservationResponse row = getRowReservation();
                        if (row == null) return;
                        handleEditReservation(row);
                    });
                    return button;
                }

                private ReservationResponse getRowReservation() {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) return null;
                    return getTableView().getItems().get(index);
                }
            });
        }

        if (colCancel != null) {
            colCancel.setCellFactory(col -> new TableCell<>() {
                private final Button cancelButton = buildCancelButton();

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : cancelButton);
                }

                private Button buildCancelButton() {
                    Button button = new Button("Cancel");
                    button.getStyleClass().add("ghost");
                    button.setOnAction(event -> {
                        ReservationResponse row = getRowReservation();
                        if (row == null) return;
                        handleCancelReservation(row);
                    });
                    return button;
                }

                private ReservationResponse getRowReservation() {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) return null;
                    return getTableView().getItems().get(index);
                }
            });
        }

        if (colPay != null) {
            colPay.setCellFactory(col -> new TableCell<>() {
                private final Button payButton = buildPayButton();

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    ReservationResponse row = getRowReservation();
                    if (row == null || !isSeated(row)) {
                        setGraphic(null);
                        return;
                    }
                    setGraphic(payButton);
                }

                private Button buildPayButton() {
                    Button button = new Button("Pay");
                    button.getStyleClass().add("ghost");
                    button.setOnAction(event -> {
                        ReservationResponse row = getRowReservation();
                        if (row == null) return;
                        handlePayReservation(row);
                    });
                    return button;
                }

                private ReservationResponse getRowReservation() {
                    int index = getIndex();
                    if (index < 0 || index >= getTableView().getItems().size()) return null;
                    return getTableView().getItems().get(index);
                }
            });
        }

        if (reservationsTable != null) {
            reservationsTable.setItems(reservations);
        }

        setInfo("Loading upcoming reservations...");
    }

    /**
     * Injects the {@link ClientController} and sets the current connection state.
     * <p>
     * When connected for the first time, requests upcoming reservations automatically.
     * Also updates the QR label based on the current user ID.
     * </p>
     *
     * @param controller the application-level client controller used for server communication
     * @param connected  {@code true} if connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        updateQrLabel();
        if (connected && !requested) {
            requested = true;
            requestUpcomingReservations();
        }
    }

    /**
     * Sets an optional external handler for the Pay action.
     * <p>
     * If set, {@link #handlePayReservation(ReservationResponse)} delegates to this handler instead of sending
     * a bill request directly.
     * </p>
     *
     * @param onPayReservation handler invoked when user presses "Pay" on a seated reservation
     */
    public void setOnPayReservation(Consumer<ReservationResponse> onPayReservation) {
        this.onPayReservation = onPayReservation;
    }

    /**
     * Sets an optional callback invoked after logout completes.
     *
     * @param onLogout callback to run after logout
     */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /**
     * Sets an optional external handler for the Edit action.
     * <p>
     * If set, {@link #handleEditReservation(ReservationResponse)} delegates to this handler.
     * </p>
     *
     * @param onEditReservation handler invoked when user presses "Edit"
     */
    public void setOnEditReservation(Consumer<ReservationResponse> onEditReservation) {
        this.onEditReservation = onEditReservation;
    }

    /**
     * UI handler that refreshes the upcoming reservations table.
     */
    @FXML
    private void onRefresh() {
        requestUpcomingReservations();
    }

    /**
     * UI handler that updates the QR label and displays the QR popup dialog.
     * <p>
     * Note: currently the popup shows the user ID as large text (not a generated QR image).
     * </p>
     */
    @FXML
    private void onShowQr() {
        updateQrLabel();
        showQrPopup();
    }

    /**
     * UI handler that logs the user out and triggers {@link #onLogout} if set.
     */
    @FXML
    private void onLogout() {
        if (clientController != null) {
            clientController.logout();
        }
        if (onLogout != null) {
            onLogout.run();
        }
    }

    /**
     * UI handler that requests subscriber account details (email/phone) from the server.
     * <p>
     * On success, {@link #onUserDetailsResponse(String, String)} is expected to show an edit dialog.
     * </p>
     */
    @FXML
    private void onAccount() {
        if (clientController == null || !connected) {
            setInfo("Not connected to server.");
            return;
        }
        setInfo("Loading account details...");
        clientController.requestUserDetails();
    }

    /**
     * Requests upcoming reservations for the current subscriber.
     * <p>
     * Updates the info label if not connected.
     * </p>
     */
    private void requestUpcomingReservations() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return;
        }
        setInfo("Loading upcoming reservations...");
        clientController.requestUpcomingReservations();
    }

    /**
     * Loads the reservations view FXML and caches its controller.
     * <p>
     * If loading succeeds and a {@link ReservationsViewController} is found, it receives the current
     * {@link ClientController} instance.
     * </p>
     */
    private void loadReservationsView() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop_views/ReservationsView.fxml"));
            loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof ReservationsViewController rvc) {
                reservationsViewController = rvc;
                reservationsViewController.setClientController(clientController, connected);
            }
        } catch (Exception e) {
            setInfo("Failed to load reservations screen.");
            e.printStackTrace();
        }
    }

    /**
     * Updates the on-screen label that displays the current user ID (used as a "QR label").
     */
    private void updateQrLabel() {
        if (qrLabel == null) return;
        String userId = clientController == null ? null : clientController.getCurrentUserId();
        qrLabel.setText("User ID: " + (userId == null || userId.isBlank() ? "-" : userId));
    }

    /**
     * Displays a popup dialog containing the current user ID.
     * <p>
     * The dialog is titled "QR Code" but currently displays only the raw user ID as large text.
     * </p>
     */
    private void showQrPopup() {
        String userId = clientController == null ? null : clientController.getCurrentUserId();
        String displayId = userId == null || userId.isBlank() ? "-" : userId;

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("QR Code");
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        javafx.scene.control.Label label = new javafx.scene.control.Label(displayId);
        label.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12, label);
        content.setStyle("-fx-alignment: center; -fx-padding: 20;");
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    /**
     * Displays an informational message in the UI.
     *
     * @param title unused title (kept for {@link ClientUIHandler} compatibility)
     * @param message message to display
     */
    @Override
    public void showInfo(String title, String message) {
        setInfo(message);
    }

    /**
     * Displays a warning message in the UI.
     *
     * @param title unused title (kept for {@link ClientUIHandler} compatibility)
     * @param message message to display
     */
    @Override
    public void showWarning(String title, String message) {
        setInfo(message);
    }

    /**
     * Displays an error message in the UI.
     *
     * @param title unused title (kept for {@link ClientUIHandler} compatibility)
     * @param message message to display
     */
    @Override
    public void showError(String title, String message) {
        setInfo(message);
    }

    /**
     * Displays an arbitrary payload as text in the UI.
     *
     * @param payload payload object received from the server/client layer
     */
    @Override
    public void showPayload(Object payload) {
        setInfo(String.valueOf(payload));
    }

    /**
     * Routes to the desktop view based on role.
     * <p>
     * Not used here because this controller already represents the subscriber home screen.
     * </p>
     *
     * @param role subscriber role (unused)
     * @param username username (unused)
     */
    @Override
    public void routeToDesktop(DesktopScreenController.Role role, String username) {
        // already in subscriber screen
    }

    /**
     * Handles report responses.
     * <p>
     * Not used here because the subscriber home screen does not display reports.
     * </p>
     *
     * @param reportResponse report response received from the server (unused)
     */
    @Override
    public void onReportResponse(responses.ReportResponse reportResponse) {
        // Subscriber home screen doesn't display reports
    }

    /**
     * Handles reservation responses received from the server.
     * <p>
     * If a {@link ReservationsViewController} is loaded, forwards the response to it as well.
     * Then performs local handling (e.g., updating the table after cancel/edit actions).
     * </p>
     *
     * @param response reservation response received from the server
     */
    @Override
    public void onReservationResponse(ReservationResponse response) {
        if (reservationsViewController != null) {
            reservationsViewController.handleServerResponse(response);
        }
        if (response != null) {
            handleReservationResponse(response);
        }
    }

    /**
     * Callback invoked when subscriber account details are received.
     * <p>
     * Opens a dialog that allows viewing and editing email/phone values.
     * </p>
     *
     * @param email current email value (may be {@code null})
     * @param phone current phone value (may be {@code null})
     */
    @Override
    public void onUserDetailsResponse(String email, String phone) {
        showAccountDialog(email, phone);
    }

    /**
     * Seating responses are not used in this screen.
     *
     * @param response seating response (unused)
     */
    @Override
    public void onSeatingResponse(SeatingResponse response) {
        // not used here
    }

    /**
     * User history responses are not used in this screen.
     *
     * @param rows history rows (unused)
     */
    @Override
    public void onUserHistoryResponse(List<responses.UserHistoryResponse> rows) {
        // not used here
    }

    /**
     * User history errors are not used in this screen.
     *
     * @param message error message (unused)
     */
    @Override
    public void onUserHistoryError(String message) {
        // not used here
    }

    /**
     * Callback invoked when upcoming reservations are received successfully.
     *
     * @param rows upcoming reservation rows; may be {@code null}
     */
    @Override
    public void onUpcomingReservationsResponse(List<ReservationResponse> rows) {
        reservations.clear();
        if (rows != null) {
            reservations.addAll(rows);
        }
        setInfo(reservations.isEmpty() ? "No upcoming reservations." : "Upcoming reservations loaded.");
    }

    /**
     * Callback invoked when loading upcoming reservations failed.
     *
     * @param message error message; if {@code null} or blank, a default message is shown
     */
    @Override
    public void onUpcomingReservationsError(String message) {
        reservations.clear();
        setInfo(message == null || message.isBlank() ? "Failed to load upcoming reservations." : message);
    }

    /**
     * Manager responses are not used in this screen.
     *
     * @param response manager response (unused)
     */
    @Override
    public void onManagerResponse(ManagerResponse response) {
        // not used here
    }

    /**
     * Callback invoked when bill total is calculated.
     *
     * @param baseTotal calculated bill total
     * @param isCash    indicates whether the payment method is cash
     */
    @Override
    public void onBillTotal(double baseTotal, boolean isCash) {
        setInfo("Bill total: " + baseTotal);
    }

    /**
     * Callback invoked when the bill payment is completed.
     *
     * @param tableNumber table number related to the payment, if provided by the server
     */
    @Override
    public void onBillPaid(Integer tableNumber) {
        setInfo("Payment completed.");
    }

    /**
     * Callback invoked when a bill/payment error occurs.
     *
     * @param message error message; if {@code null} or blank, a default message is shown
     */
    @Override
    public void onBillError(String message) {
        setInfo(message == null || message.isBlank() ? "Payment failed." : message);
    }

    /**
     * Formats a {@link LocalDate} for display.
     *
     * @param date date to format; may be {@code null}
     * @return formatted date or "-" if {@code date} is {@code null}
     */
    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    /**
     * Formats a {@link LocalTime} for display.
     *
     * @param time time to format; may be {@code null}
     * @return formatted time or "-" if {@code time} is {@code null}
     */
    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FMT);
    }

    /**
     * Formats a positive integer for display.
     *
     * @param value numeric value
     * @return the number as a string, or "-" if {@code value <= 0}
     */
    private String formatInt(int value) {
        return value <= 0 ? "-" : String.valueOf(value);
    }

    /**
     * Updates the info label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Handles the "Pay" action for a reservation row.
     * <p>
     * If an external handler was provided via {@link #setOnPayReservation(Consumer)}, the action is delegated to it.
     * Otherwise, a bill payment request is sent directly using {@link BillRequestType#PAY_BILL}.
     * </p>
     *
     * @param row reservation row representing the seated reservation to pay for
     */
    private void handlePayReservation(ReservationResponse row) {
        if (row == null || clientController == null || !connected) {
            setInfo("Not connected to server.");
            return;
        }
        if (onPayReservation != null) {
            onPayReservation.accept(row);
            return;
        }
        int confirmationCode = row.getConfirmationCode() == null ? 0 : row.getConfirmationCode();
        if (confirmationCode <= 0) {
            setInfo("Missing confirmation code.");
            return;
        }
        setInfo("Submitting payment...");
        clientController.requestBillAction(BillRequestType.PAY_BILL, confirmationCode, false);
    }

    /**
     * Checks whether the reservation row is currently in "SEATED" status.
     * <p>
     * Used to decide whether to display the Pay button.
     * </p>
     *
     * @param row reservation row to check
     * @return {@code true} if status is "SEATED" (case-insensitive); {@code false} otherwise
     */
    private boolean isSeated(ReservationResponse row) {
        if (row == null) return false;
        String status = row.getStatus();
        return status != null && "SEATED".equalsIgnoreCase(status.trim());
    }

    /**
     * Handles the "Edit" action for a reservation row.
     * <p>
     * If an external handler was provided via {@link #setOnEditReservation(Consumer)}, the action is delegated to it.
     * Otherwise, shows an informational message.
     * </p>
     *
     * @param row reservation row to edit
     */
    private void handleEditReservation(ReservationResponse row) {
        if (row == null) return;
        if (onEditReservation != null) onEditReservation.accept(row);
        else setInfo("Edit screen unavailable.");
    }

    /**
     * Shows a dialog allowing the subscriber to view and edit account details (email and phone).
     * <p>
     * The dialog supports:
     * <ul>
     *   <li>Edit: enables the text fields</li>
     *   <li>Save: sends {@code requestEditDetails(email, phone)} and closes the dialog</li>
     *   <li>Close: closes without changes</li>
     * </ul>
     * </p>
     *
     * @param email current email; may be {@code null}
     * @param phone current phone; may be {@code null}
     */
    private void showAccountDialog(String email, String phone) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Subscriber Account");

        ButtonType editType = new ButtonType("Edit", ButtonBar.ButtonData.LEFT);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(editType, saveType, ButtonType.CLOSE);

        TextField emailField = new TextField(email == null ? "" : email);
        TextField phoneField = new TextField(phone == null ? "" : phone);
        emailField.setDisable(true);
        phoneField.setDisable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Email"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Phone"), 0, 1);
        grid.add(phoneField, 1, 1);

        Node editButton = dialog.getDialogPane().lookupButton(editType);
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.setDisable(true);

        editButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            emailField.setDisable(false);
            phoneField.setDisable(false);
            saveButton.setDisable(false);
            emailField.requestFocus();
        });

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            if (clientController == null || !connected) {
                setInfo("Not connected to server.");
                return;
            }
            clientController.requestEditDetails(emailField.getText(), phoneField.getText());
            dialog.close();
        });

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    /**
     * Handles the "Cancel" action for a reservation row by sending a cancel request to the server.
     *
     * @param row reservation row to cancel
     */
    private void handleCancelReservation(ReservationResponse row) {
        if (row == null) return;
        if (clientController == null || !connected) {
            setInfo("Not connected to server.");
            return;
        }
        Integer confirmationCode = row.getConfirmationCode();
        if (confirmationCode == null || confirmationCode <= 0) {
            setInfo("Missing confirmation code.");
            return;
        }
        setInfo("Cancelling reservation " + confirmationCode + "...");
        clientController.requestNewReservation(
                ReservationRequestType.CANCEL_RESERVATION,
                null,
                null,
                0,
                null,
                null,
                confirmationCode
        );
    }

    /**
     * Applies local UI updates based on a {@link ReservationResponse} type.
     * <p>
     * Handles:
     * <ul>
     *   <li>{@code CANCEL_RESERVATION}: removes the cancelled reservation from the table</li>
     *   <li>{@code EDIT_RESERVATION}: refreshes upcoming reservations</li>
     * </ul>
     * </p>
     *
     * @param response reservation response received from the server
     */
    public void handleReservationResponse(ReservationResponse response) {
        if (response == null) return;
        ReservationResponse.ReservationResponseType type = response.getType();
        if (type == null) return;
        switch (type) {
            case CANCEL_RESERVATION -> {
                Integer code = response.getConfirmationCode();
                if (code != null) {
                    reservations.removeIf(row -> code.equals(row.getConfirmationCode()));
                }
                setInfo("Reservation cancelled.");
            }
            case EDIT_RESERVATION -> {
                setInfo("Reservation updated.");
                requestUpcomingReservations();
            }
            default -> {
            }
        }
    }

    /**
     * Waiting list cancellation callback.
     * <p>
     * Currently not implemented in this screen.
     * </p>
     *
     * @param response waiting list response received from the server (unused)
     */
    @Override
    public void onWaitingListCancellation(WaitingListResponse response) {
        // TODO Auto-generated method stub
    }
}
