package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;

/**
 * JavaFX controller for a terminal screen that helps users recover a lost confirmation code.
 * <p>
 * Supports two recovery flows:
 * <ul>
 *   <li>Recover by phone/email (basic format validation is applied)</li>
 *   <li>Recover by subscriber user ID (entered manually via a dialog, e.g., "scan QR")</li>
 * </ul>
 * </p>
 * <p>
 * When connected, the controller sends a lost-code request via {@link ClientController}.
 * The server is expected to send the confirmation code to the customer's registered contact method.
 * </p>
 */
public class TerminalLostCodeController implements ClientControllerAware {

    @FXML private TextField phoneOrEmailField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * Maximum allowed input length for the contact field.
     */
    private static final int MAX_LEN = 40;

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Applies a {@link TextFormatter} that limits the input length of {@link #phoneOrEmailField}
     * and initializes the status label.
     * </p>
     */
    @FXML
    private void initialize() {
        // Max length 40 (allows any characters)
        if (phoneOrEmailField != null) {
            phoneOrEmailField.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                return (newText.length() <= MAX_LEN) ? change : null;
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
     * UI handler that submits a lost-code recovery request using phone/email.
     * <p>
     * The value is validated for basic phone/email format before sending.
     * </p>
     */
    @FXML
    private void onSend() {
        String contact = phoneOrEmailField == null ? "" : phoneOrEmailField.getText().trim();
        submitContact(contact, true);
    }

    /**
     * UI handler for an alternative flow: recover using subscriber user ID (e.g., "scan QR code").
     * <p>
     * Prompts the terminal operator to enter a subscriber user ID and submits it without applying
     * phone/email format validation.
     * </p>
     */
    @FXML
    private void onScanQr() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Scan QR Code");
        dialog.setHeaderText("Enter subscriber user ID");
        dialog.setContentText("User ID:");

        dialog.showAndWait().ifPresent(input -> {
            String userId = input == null ? "" : input.trim();
            submitContact(userId, false);
        });
    }

    /**
     * UI handler that clears the input field and resets the status label.
     */
    @FXML
    private void onClear() {
        if (phoneOrEmailField != null) phoneOrEmailField.clear();
        setStatus("");
    }

    /**
     * Updates the status label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Validates and submits a lost-code recovery request.
     * <p>
     * When {@code validateContact} is {@code true}, basic phone/email validation is enforced:
     * <ul>
     *   <li>Email: must contain '@' and '.'</li>
     *   <li>Phone: must match the pattern {@code [0-9+\\- ]{6,20}}</li>
     * </ul>
     * </p>
     * <p>
     * When {@code validateContact} is {@code false}, the input is treated as a user ID and is not validated
     * as phone/email.
     * </p>
     * <p>
     * If the terminal is offline (not connected or controller missing), the request is not sent.
     * </p>
     *
     * @param contact         phone/email or user ID string to submit
     * @param validateContact {@code true} to validate as phone/email; {@code false} to treat input as user ID
     */
    private void submitContact(String contact, boolean validateContact) {
        if (contact.isEmpty()) {
            setStatus(validateContact ? "Enter phone or email." : "Enter user ID.");
            return;
        }

        if (contact.length() > MAX_LEN) {
            setStatus("Too long. Max " + MAX_LEN + " characters.");
            return;
        }

        if (validateContact) {
            // basic validation for phone/email format
            boolean email = contact.contains("@") && contact.contains(".");
            boolean phone = contact.matches("[0-9+\\- ]{6,20}");
            if (!email && !phone) {
                setStatus("Enter a valid phone/email.");
                return;
            }
        }

        if (!connected || clientController == null) {
            setStatus("Terminal is offline.");
            return;
        }

        // server will send code to customer via their registered contact method
        clientController.requestLostCode(contact);
        setStatus("Recovery request sent.");
    }
}
