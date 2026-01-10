package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;

public class AddSubscriberScreenController implements ClientControllerAware {

    @FXML private TextField usernameField;
    @FXML private TextField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onSave() {
        if (!readyForServer()) return;

        String username = getValue(usernameField);
        String password = getValue(passwordField);
        String phone = getValue(phoneField);
        String email = getValue(emailField);

        if (username.isEmpty() || password.isEmpty()) {
            setInfo("Username and password are required.");
            return;
        }

        ManagerRequest request = new ManagerRequest(
                ManagerCommand.ADD_NEW_USER,
                username,
                password,
                phone,
                email
        );

        clientController.requestManagerAction(request);
        setInfo("Submitting new subscriber...");
    }

    @FXML
    private void onClear() {
        clearFields();
        setInfo("");
    }

    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        if (response.getResponseCommand() == ManagerResponseCommand.NEW_USER_RESPONSE) {
            String newId = response.getUserID();
            setInfo(newId == null ? "Subscriber added." : "Subscriber added. ID: " + newId);

            // clear input fields but keep success message visible
            clearFields();
        }
    }

    private void clearFields() {
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();
        if (phoneField != null) phoneField.clear();
        if (emailField != null) emailField.clear();
    }

    private String getValue(TextField field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().trim();
    }

    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
