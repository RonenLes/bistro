package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TablesViewController implements ClientControllerAware {

    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
        setInfo("Tables overview (demo).");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onRefreshTables() {
        setInfo("Refreshing tables (placeholder).");
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg);
    }
}
