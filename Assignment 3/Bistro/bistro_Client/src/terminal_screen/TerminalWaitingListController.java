package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

public class TerminalWaitingListController implements ClientControllerAware {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private Spinner<Integer> partySizeField; // spinner
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
        if (partySizeField != null) {
            partySizeField.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2)
            );
            partySizeField.setEditable(true); // optional: allow typing
        }
        setStatus("");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onJoin() {
        String name = nameField == null ? "" : nameField.getText().trim();
        String phone = phoneField == null ? "" : phoneField.getText().trim();

        if (name.isEmpty()) { setStatus("Enter name."); return; }
        if (phone.isEmpty()) { setStatus("Enter phone."); return; }

        Integer sizeObj = (partySizeField == null) ? null : partySizeField.getValue();
        int size = (sizeObj == null) ? -1 : sizeObj;

        if (size < 1 || size > 20) { setStatus("Party size must be 1-20."); return; }

        if (!connected || clientController == null) {
            setStatus("Demo: added " + name + " (party of " + size + ") to waiting list.");
            return;
        }

        // Later:
        // clientController.sendRequest(new JoinWaitingListRequest(name, phone, size));
        setStatus("Sent waiting list request (placeholder).");
    }

    @FXML
    private void onClear() {
        if (nameField != null) nameField.clear();
        if (phoneField != null) phoneField.clear();
        if (partySizeField != null) partySizeField.getValueFactory().setValue(2);
        setStatus("");
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }
    
    public void handleSeatingResponse(responses.SeatingResponse response) {
        if (response == null) return;
        switch (response.getType()) {
            case CUSTOMER_CHECKED_IN -> {
                Integer table = response.getTableNumberl();
                setStatus("Table ready! You're seated" + (table != null ? " at table " + table + "." : "."));
            }
            case CUSTOMER_IN_WAITINGLIST -> setStatus("You're on the take-a-seat list. We'll notify you soon.");
        }
    }
}
