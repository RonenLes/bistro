package main_screen;

import controllers.ClientController;
import desktop_screen.DesktopScreenController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.util.function.Consumer;
import java.util.regex.Pattern;

// handles login screen UI for both subscribers and guests
// toggles between member login (username/password) and guest mode (contact)
// validates credentials and triggers navigation to desktop
public class LoginScreenController {

    // input fields
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField guestContactField;

    @FXML private ChoiceBox<DesktopScreenController.Role> roleChoiceBox;

    @FXML private Button loginButton;

    @FXML private Label statusLabel;
    @FXML private Button guestButton;
    @FXML private Button backButton;

    private ClientController clientController;
    private boolean connected;

    // callbacks set by AppNavigator
    private Runnable onBackToMain;
    private Consumer<DesktopScreenController.Role> onLoginAsRole;

    // tracks whether UI is in guest mode or member mode
    private boolean guestMode;

    // validation patterns for guest contact
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{7,15}$");

    // dependency injection from AppNavigator
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        updateConnectionStateUI();
    }

    // sets callback for back button
    public void setOnBackToMain(Runnable cb) {
        this.onBackToMain = cb;
    }

    // sets callback for successful login
    public void setOnLoginAsRole(Consumer<DesktopScreenController.Role> cb) {
        this.onLoginAsRole = cb;
    }

    // retrieves username for desktop welcome message
    public String getUsernameForWelcome() {
        if (usernameField == null) return "";
        return usernameField.getText() == null ? "" : usernameField.getText().trim();
    }

    @FXML
    private void initialize() {
        if (statusLabel != null) statusLabel.setText("");

        // allow Enter key to submit login
        if (passwordField != null) {
            passwordField.setOnAction(e -> onLoginClicked());
        }

        // start in member mode
        setGuestMode(false);
    }

    /**
     * Login button click
     * calls the client controller with a request
     */
    // handles both guest and subscriber login flows
    @FXML
    private void onLoginClicked() {

        if (guestMode) {
            // guest login: validate contact (email or phone)
            String contact = guestContactField == null ? "" : guestContactField.getText();
            contact = contact == null ? "" : contact.trim();

            if (!isValidContact(contact)) {
                setStatus("Enter a valid email or phone number.", true);
                return;
            }

            // store contact in controller for this session
            if (clientController != null) {
                clientController.startGuestSession(contact);
            }

            setStatus("Continuing as guest...", false);

            // trigger navigation to desktop with guest role
            if (onLoginAsRole != null) {
                onLoginAsRole.accept(DesktopScreenController.Role.GUEST);
            }
            return;
        }

        // subscriber login: send credentials to server
        String username = usernameField == null ? "" : usernameField.getText();
        String password = passwordField == null ? "" : passwordField.getText();

        if (clientController == null) {
            setStatus("Client controller not ready.", true);
            return;
        }

        // server will respond with role via LoginResponse
        // response handled by AppNavigator's navigationHandler
        clientController.requestLogin(username, password);
        setStatus("Logging in...", false);
    }

    @FXML
    // toggles between member and guest UI modes
    private void onContinueAsGuestClicked() {
        // toggle mode
        setGuestMode(!guestMode);
    }

    @FXML
    // returns to main menu
    private void onBackClicked(ActionEvent event) {
        if (onBackToMain != null) {
            onBackToMain.run();
            return;
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void updateConnectionStateUI() {
        if (loginButton != null) loginButton.setDisable(false);

        if (statusLabel != null && !connected) {
            setStatus("No server connection", true);
        }
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    // switches UI between member and guest modes
    // shows/hides appropriate fields and updates button labels
    private void setGuestMode(boolean guest) {
        this.guestMode = guest;

        // show username/password for members, hide for guests
        if (usernameField != null) {
            usernameField.setVisible(!guest);
            usernameField.setManaged(!guest);
        }
        if (passwordField != null) {
            passwordField.setVisible(!guest);
            passwordField.setManaged(!guest);
        }
        // show contact field for guests, hide for members
        if (guestContactField != null) {
            guestContactField.setVisible(guest);
            guestContactField.setManaged(guest);
            if (!guest) {
                guestContactField.clear();
            }
        }

        // update button labels based on mode
        if (loginButton != null) {
            loginButton.setText(guest ? "Continue as guest" : "Login");
        }
        if (guestButton != null) {
            guestButton.setText(guest ? "Back to member login" : "Continue as guest");
        }

        // clear status when switching modes
        setStatus("", false);
    }

    // validates guest contact as either email or phone
    private boolean isValidContact(String value) {
        if (value == null) return false;
        String v = value.trim();
        if (v.isEmpty()) return false;
        if (EMAIL_PATTERN.matcher(v).matches()) {
            return true;
        }
        return PHONE_PATTERN.matcher(v).matches();
    }
}
