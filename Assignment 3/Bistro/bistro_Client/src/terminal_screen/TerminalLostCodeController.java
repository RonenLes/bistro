package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;

// terminal view for retrieving lost confirmation codes
// supports lookup by phone/email or user ID
// server sends code to customer's registered contact method
public class TerminalLostCodeController implements ClientControllerAware {

    @FXML private TextField phoneOrEmailField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    private static final int MAX_LEN = 40;

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

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    // sends lost code request with phone or email
    private void onSend() {
        String contact = phoneOrEmailField == null ? "" : phoneOrEmailField.getText().trim();
        submitContact(contact, true);
      
    }
    
    @FXML
    // alternative: sends lost code request with user ID
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

    @FXML
    private void onClear() {
        if (phoneOrEmailField != null) phoneOrEmailField.clear();
        setStatus("");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
    

    // validates and sends lost code request
    // validateContact determines whether to enforce phone/email format
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
