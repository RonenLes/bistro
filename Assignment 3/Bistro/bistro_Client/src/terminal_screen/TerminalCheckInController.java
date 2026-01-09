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

        int confirmationCode = Integer.parseInt(txt);

        if (!connected || clientController == null) {
            setStatus("Offline Mode: cannot check-in without server connection.");
            return;
        }

        setStatus("Checking in...");

        clientController.requestSeatingCheckInByConfirmationCode(confirmationCode);
    }
    
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

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
}
