package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

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
    private void onSend() {
        String contact = phoneOrEmailField == null ? "" : phoneOrEmailField.getText().trim();
        if (contact.isEmpty()) { setStatus("Enter phone or email."); return; }

        // Double-check (in case text was set programmatically)
        if (contact.length() > MAX_LEN) {
            setStatus("Too long. Max " + MAX_LEN + " characters.");
            return;
        }

        boolean email = contact.contains("@") && contact.contains(".");
        boolean phone = contact.matches("[0-9+\\- ]{6,20}");
        if (!email && !phone) { setStatus("Enter a valid phone/email."); return; }

        if (!connected || clientController == null) {
            setStatus("Demo: recovery sent to " + contact + " (offline).");
            return;
        }

        clientController.requestLostCode(contact);
        setStatus("Requesting your code...");
      
    }

    @FXML
    private void onClear() {
        if (phoneOrEmailField != null) phoneOrEmailField.clear();
        setStatus("");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
}
