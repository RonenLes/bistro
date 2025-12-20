package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

public class TerminalPayBillController implements ClientControllerAware {

    @FXML private TextField reservationNumberField;
    @FXML private TextArea billArea;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    private boolean billLoaded = false;
    private double demoTotal = 0;

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
        if (billArea != null) {
            billArea.setEditable(false);
            billArea.setText("Bill will appear here...");
        }
        setStatus("");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onFetchBill() {
        String txt = reservationNumberField == null ? "" : reservationNumberField.getText().trim();
        if (txt.isEmpty()) { setStatus("Enter reservation number."); return; }

        int reservationNumber = Integer.parseInt(txt);

        // Placeholder bill
        demoTotal = 142.50;
        billLoaded = true;

        if (billArea != null) {
            billArea.setText(
                "Reservation: " + reservationNumber + "\n" +
                "-------------------------\n" +
                "2x Pasta           78.00\n" +
                "1x Salad           29.50\n" +
                "1x Cola            12.00\n" +
                "Service            23.00\n" +
                "-------------------------\n" +
                "TOTAL:            " + demoTotal + "\n"
            );
        }
        //END PLACEHOLDER

        if (!connected || clientController == null) {
            setStatus("✅ Demo: bill loaded (offline).");
            return;
        }

        // Later:
        // clientController.sendRequest(new FetchBillRequest(reservationNumber));
        setStatus("✅ Sent fetch bill request (placeholder).");
    }

    @FXML
    private void onPay() {
        if (!billLoaded) { setStatus("Load the bill first."); return; }

        billLoaded = false;
        if (billArea != null) billArea.setText("Payment completed. תודה! ✅");
        setStatus("✅ Demo: payment succeeded (total: " + demoTotal + ").");

        // Later:
        // clientController.sendRequest(new PayBillRequest(...));
    }

    @FXML
    private void onClear() {
        if (reservationNumberField != null) reservationNumberField.clear();
        if (billArea != null) billArea.setText("Bill will appear here...");
        billLoaded = false;
        demoTotal = 0;
        setStatus("");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
}
