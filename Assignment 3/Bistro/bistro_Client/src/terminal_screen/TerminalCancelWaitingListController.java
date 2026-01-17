package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import responses.WaitingListResponse;
import javafx.scene.control.TextInputDialog;

/**
 * JavaFX controller for a terminal screen that cancels waiting-list entries.
 * <p>
 * Supports two cancellation flows:
 * <ul>
 *   <li>Cancel by confirmation code (entered directly by the user)</li>
 *   <li>Cancel by subscriber user ID (resolves the confirmation code from the server, then cancels)</li>
 * </ul>
 * </p>
 * <p>
 * Server communication is performed via an injected {@link ClientController}. If not connected, the controller
 * may display offline/demo messages instead of sending real requests.
 * </p>
 */
public class TerminalCancelWaitingListController implements ClientControllerAware {

    @FXML private TextField confirmationCodeField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * Initializes the status label.
     */
    @FXML
    private void initialize() {
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
     * UI handler that validates the confirmation code and sends a waiting-list cancellation request.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Confirmation code must be provided</li>
     *   <li>Confirmation code must be numeric</li>
     *   <li>Confirmation code must be positive</li>
     * </ul>
     * </p>
     * <p>
     * If the client is not connected (or controller is missing), the method shows a demo/offline message
     * instead of sending a real request.
     * </p>
     */
    @FXML
    private void onCancel() {
        String rawCode = confirmationCodeField == null ? "" : confirmationCodeField.getText().trim();
        if (rawCode.isEmpty()) {
            setStatus("Enter your confirmation code.");
            return;
        }

        int code;
        try {
            code = Integer.parseInt(rawCode);
        } catch (NumberFormatException ex) {
            setStatus("Confirmation code must be a number.");
            return;
        }

        if (code <= 0) {
            setStatus("Confirmation code must be a positive number.");
            return;
        }

        if (!connected || clientController == null) {
            setStatus("Demo: waiting list entry cancelled for code " + code + ".");
            return;
        }

        clientController.requestWaitingListCancellation(code);
        setStatus("Submitting cancel request...");
    }

    /**
     * Handles a {@link WaitingListResponse} returned from the server after a cancellation request.
     * <p>
     * If the cancellation succeeded, updates the status label and shows an informational alert.
     * Otherwise, updates the status label with a failure message.
     * </p>
     *
     * @param response server response object; may be {@code null}
     */
    public void handleWaitingListResponse(WaitingListResponse response) {
        if (response == null) {
            setStatus("No response received. Please try again.");
            return;
        }
        if (response.getHasBeenCancelled()) {
            setStatus("Waiting list entry cancelled.");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Waiting List");
            alert.setHeaderText(null);
            alert.setContentText("Waiting list entry cancelled.");
            alert.showAndWait();
        } else {
            setStatus("Unable to cancel waiting list entry.");
        }
    }

    /**
     * Updates the status label displayed on the screen.
     *
     * @param message message to show; if {@code null}, an empty string is displayed
     */
    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message == null ? "" : message);
        }
    }

    /**
     * UI handler for an alternative flow: cancel using subscriber user ID.
     * <p>
     * The method:
     * <ol>
     *   <li>Prompts the user to enter a subscriber user ID.</li>
     *   <li>Requests the confirmation code from the server (lost-code flow).</li>
     *   <li>Registers a callback listener to receive the resolved code.</li>
     *   <li>When the code is received, populates the confirmation code field and triggers {@link #onCancel()}.</li>
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
                setStatus("Offline Mode: cannot cancel without server connection.");
                return;
            }

            // set callback to receive resolved code from server
            clientController.setLostCodeListener(code -> {
                clientController.clearLostCodeListener();
                if (code == null || code <= 0) {
                    setStatus("Failed to resolve confirmation code.");
                    return;
                }
                // populate field and trigger cancellation
                if (confirmationCodeField != null) {
                    confirmationCodeField.setText(String.valueOf(code));
                }
                onCancel();
            });

            setStatus("Resolving confirmation code...");
            clientController.requestLostCode(userId);
        });
    }
}
