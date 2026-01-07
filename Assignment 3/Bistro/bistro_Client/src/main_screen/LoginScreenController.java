package main_screen;

import controllers.ClientController;
import desktop_screen.DesktopScreenController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.util.function.Consumer;

public class LoginScreenController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML private ChoiceBox<DesktopScreenController.Role> roleChoiceBox;

    @FXML private Button loginButton;

    @FXML private Label statusLabel;
    @FXML private Button guestButton;
    @FXML private Button backButton;

    private ClientController clientController;
    private boolean connected;

    private Runnable onBackToMain;
    private Consumer<DesktopScreenController.Role> onLoginAsRole;

    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        updateConnectionStateUI();
    }

    public void setOnBackToMain(Runnable cb) {
        this.onBackToMain = cb;
    }

    public void setOnLoginAsRole(Consumer<DesktopScreenController.Role> cb) {
        this.onLoginAsRole = cb;
    }

    public String getUsernameForWelcome() {
        if (usernameField == null) return "";
        return usernameField.getText() == null ? "" : usernameField.getText().trim();
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) statusLabel.setText("");

        if (passwordField != null) {
            passwordField.setOnAction(e -> onLoginClicked());
        }
        
    }
    /**
     * Login button click
     * calls the client controller with a request
     */
    @FXML
    private void onLoginClicked() {

        String username = usernameField == null ? "" : usernameField.getText();
        String password = passwordField == null ? "" : passwordField.getText();

        if (clientController == null) {
            setStatus("Client controller not ready.", true);
            return;
        }

        // send login request via controller (with validation inside)
        clientController.requestLogin(username, password);

        //Just update status – No navigation here
        setStatus("Logging in...", false);
    }

    @FXML
    private void onContinueAsGuestClicked() {
        setStatus("Continuing as guest...", false);

        if (onLoginAsRole != null) {
            onLoginAsRole.accept(DesktopScreenController.Role.GUEST);
        }
    }

    @FXML
    private void onBackClicked(ActionEvent event) {
        if (onBackToMain != null) {
            onBackToMain.run();
            return;
        }
        // fallback (if opened as separate stage)
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void updateConnectionStateUI() {
        // In test mode you might NOT want to disable login when offline
        // so keep it enabled:
        if (loginButton != null) loginButton.setDisable(false);

        if (statusLabel != null && !connected) {
            setStatus("Disconnected (test login still works).", true);
        }
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }
}
