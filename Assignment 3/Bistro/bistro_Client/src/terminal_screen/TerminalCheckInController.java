package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class TerminalCheckInController implements ClientControllerAware {

    @FXML private TextField reservationNumberField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
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

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onEnter() {
        String txt = reservationNumberField == null ? "" : reservationNumberField.getText().trim();
        if (txt.isEmpty()) { setStatus("Enter reservation number."); return; }

        int reservationNumber = Integer.parseInt(txt);

        // Placeholder (no DB)
        if (!connected || clientController == null) {
            setStatus("✅ Demo: checked-in reservation " + reservationNumber + " (offline).");
            return;
        }

        // Later:
        // clientController.sendRequest(new TerminalCheckInRequest(reservationNumber));
        setStatus("✅ Sent check-in request (placeholder).");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
}
