package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import responses.WaitingListResponse;

public class TerminalCancelWaitingListController implements ClientControllerAware {

    @FXML private TextField confirmationCodeField;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
        setStatus("");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

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

    public void handleWaitingListResponse(WaitingListResponse response) {
        if (response == null) {
            setStatus("No response received. Please try again.");
            return;
        }
        if (response.getHasBeenCancelled()) {
            setStatus("Waiting list entry cancelled.");
        } else {
            setStatus("Unable to cancel waiting list entry.");
        }
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message == null ? "" : message);
        }
    }
}