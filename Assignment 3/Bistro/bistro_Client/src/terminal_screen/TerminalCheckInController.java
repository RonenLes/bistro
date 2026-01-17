package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;

/**
 * JavaFX controller for a terminal screen that performs reservation check-in.
 * <p>
 * Supports two check-in flows:
 * <ul>
 *   <li>Check-in by confirmation code (entered directly)</li>
 *   <li>Check-in by subscriber user ID (resolves the confirmation code from the server, then checks in)</li>
 * </ul>
 * </p>
 * <p>
 * When connected, the controller sends requests through {@link ClientController}.
 * When offline (not connected or missing controller), it shows an "Offline Mode" message instead of sending requests.
 * </p>
 */
public class TerminalCheckInController implements ClientControllerAware {

    @FXML private TextField reservationNumberField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Applies a {@link TextFormatter} to restrict input to digits only and limits the input length.
     * Initializes the status label.
     * </p>
     */
    @FXML
    private void initialize() {
        // restrict input to numeric only
        if (reservationNumberField != null) {
            reservationNumberField.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                if (!newText.matches("\\d*")) return null;
                if (newText.length() > 10) return null;
                return change;
            }));
        }
        setStatus("");
    }

    /**
     * Injects the {@link ClientController} used to communicate with the server and sets the current
     * connection state.
     *
     * @param controller the application-level client controller used for server communication
     * @param connected  {@code true} if connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * UI handler that sends a check-in request using the confirmation code entered in {@link #reservationNumberField}.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Value must not be blank</li>
     *   <li>Value is expected to be numeric (enforced by {@link TextFormatter})</li>
     * </ul>
     * </p>
     * <p>
     * If the client is not connected (or controller is missing), the method displays an offline message and returns.
     * </p>
     */
    @FXML
    private void onEnter() {
        String txt = reservationNumberField == null ? "" : reservationNumberField.getText().trim();
        if (txt.isEmpty()) {
            setStatus("Enter reservation number.");
            return;
        }

        int confirmationCode = Integer.parseInt(txt);

        if (!connected || clientController == null) {
            setStatus("Offline Mode: cannot check-in without server connection.");
            return;
        }

        setStatus("Checking in...");
        clientController.requestSeatingCheckInByConfirmationCode(confirmationCode);
    }

    /**
     * UI handler for an alternative flow: check-in using subscriber user ID.
     * <p>
     * The method:
     * <ol>
     *   <li>Prompts the user to enter a subscriber user ID.</li>
     *   <li>Requests the confirmation code from the server (lost-code flow).</li>
     *   <li>Registers a callback listener to receive the resolved confirmation code.</li>
     *   <li>When the code is received, fills the confirmation code field and triggers {@link #onEnter()}.</li>
     * </ol>
     * </p>
     * <p>
     * If not connected, the method displays an offline message and does not attempt resolution.
     * </p>
     */
    @FXML
    private void onUseUserId() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Use user ID");
        dialog.setHeaderText("Enter subscriber user ID");
        dialog.setContentText("User ID:");

        dialog.showAndWait().ifPresent(input -> {
            String userId = input == null ? "" : input.trim();
            if (userId.isEmpty()) {
                setStatus("Enter user ID.");
                return;
            }
            if (!connected || clientController == null) {
                setStatus("Offline Mode: cannot check-in without server connection.");
                return;
            }

            // set callback to receive resolved code from server
            clientController.setLostCodeListener(code -> {
                clientController.clearLostCodeListener();
                if (code == null || code <= 0) {
                    setStatus("Failed to resolve confirmation code.");
                    return;
                }
                // populate field and trigger check-in
                if (reservationNumberField != null) {
                    reservationNumberField.setText(String.valueOf(code));
                }
                onEnter();
            });
            setStatus("Resolving confirmation code...");
            clientController.requestLostCode(userId);
        });
    }

    /**
     * Handles a seating response returned from the server after a check-in request.
     * <p>
     * Expected behaviors (based on {@code response.getType()}):
     * <ul>
     *   <li>{@code CUSTOMER_CHECKED_IN}: shows a success message, including table number/capacity if provided</li>
     *   <li>{@code CUSTOMER_IN_WAITINGLIST}: indicates no table was available and the customer was added to waiting list</li>
     * </ul>
     * For any other type, an "Unknown seating response" message is shown.
     * </p>
     *
     * @param response seating response received from the server; may be {@code null}
     */
    public void handleSeatingResponse(responses.SeatingResponse response) {
        if (response == null) {
            setStatus("Unexpected: empty seating response.");
            return;
        }

        switch (response.getType()) {
            case CUSTOMER_CHECKED_IN -> {
                Integer tableNum = response.getTableNumberl(); // do not unbox
                Integer cap = response.getTableCapacity();

                String msg = "Checked-in successfully."
                        + (tableNum != null ? (" Table: " + tableNum) : "")
                        + (cap != null ? (" (Capacity: " + cap + ")") : "");

                setStatus(msg);
            }
            case CUSTOMER_IN_WAITINGLIST -> {
                setStatus("No table available. You were added to the waiting list.");
            }
            default -> setStatus("Unknown seating response.");
        }
    }

    /**
     * Updates the status label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
}
