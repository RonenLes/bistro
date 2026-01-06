package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class ReportsViewController implements ClientControllerAware {

    @FXML private ComboBox<String> monthPicker;
    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
        if (monthPicker != null) {
            monthPicker.getItems().addAll(
                    "January", "February", "March", "April",
                    "May", "June", "July", "August",
                    "September", "October", "November", "December"
            );
        }
        setInfo("Select month and generate report (demo).");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onGenerate() {
        String month = monthPicker == null ? "" : monthPicker.getValue();
        if (month == null || month.isBlank()) {
            setInfo("Please select a month.");
            return;
        }
        setInfo("Generating report for " + month + " (placeholder).");
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg);
    }
}
