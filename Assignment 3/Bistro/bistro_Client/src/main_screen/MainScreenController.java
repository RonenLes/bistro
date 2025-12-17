package main_screen;

import controllers.ClientController;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainScreenController {

    @FXML
    private Label connectionStatusLabel;

    private ClientController clientController;
    private boolean connected;

    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;

        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(
                connected ? "Connection: Connected" : "Connection: Offline"
            );
        }
    }

    @FXML
    private void onRemoteClicked() {
    	System.out.println("Remote clicked!");// SANITY CHECK
    	/**
        if (clientController != null) {
            clientController.onSelectRemoteMode();
        }**/
    }

    @FXML
    private void onTerminalClicked() {
    	System.out.println("Terminal clicked!");// SANITY CHECK
        //if (clientController != null) {
        //    clientController.onSelectTerminalMode();
       // }
    } 
}
