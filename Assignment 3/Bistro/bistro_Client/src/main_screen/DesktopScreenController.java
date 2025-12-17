package main_screen;

import controllers.ClientController;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;


public class DesktopScreenController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private ClientController clientController;
    private boolean connected;

    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }
    
    // TEMP
    @FXML
    private void initialize() {
        System.out.println("DesktopScreenController INITIALIZED");
        
    }
    
    @FXML
    private void onLoginClicked() {
        System.out.println("LOGIN CLICKED");
        clientController.requestLogin(usernameField.getText(), passwordField.getText());
    }

    @FXML
    private void onContinueAsGuestClicked() {
        // change to your actual ClientController API:
        //clientController.requestContinueAsGuest();
    		return;
    }
    
    @FXML
    private void onBackClicked(ActionEvent event) {
        Stage s = (Stage) ((Node) event.getSource()).getScene().getWindow();
        s.close();
    }
}
