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

/**
 * JavaFX controller for the login screen.
 * <p>
 * Supports two login flows:
 * <ul>
 *   <li><b>Subscriber login</b>: username + password sent to the server.</li>
 *   <li><b>Guest mode</b>: validates a contact value (email or phone) locally and starts a guest session.</li>
 * </ul>
 * <p>
 * This controller does not perform navigation directly; instead it exposes callbacks that are set by
 * {@link AppNavigator} (back navigation and login success routing).
 */
public class LoginScreenController {

    /** Username input field (subscriber mode). */
    @FXML private TextField usernameField;
    /** Password input field (subscriber mode). */
    @FXML private PasswordField passwordField;
    /** Guest contact input field (guest mode). */
    @FXML private TextField guestContactField;

    /** Role selection (may be used by UI; actual role is typically determined by the server login response). */
    @FXML private ChoiceBox<DesktopScreenController.Role> roleChoiceBox;

    /** Main login button (submits login or continues as guest depending on mode). */
    @FXML private Button loginButton;

    /** Label for status/error messages. */
    @FXML private Label statusLabel;
    /** Button that toggles between guest and member login UI modes. */
    @FXML private Button guestButton;
    /** Back button that returns to the main menu (or closes the stage as fallback). */
    @FXML private Button backButton;

    /** Shared controller used for server communication and session state. */
    private ClientController clientController;
    /** Whether the client is currently connected to the server. */
    private boolean connected;

    /** Callback set by {@link AppNavigator} for returning to the main menu screen. */
    private Runnable onBackToMain;
    /** Callback set by {@link AppNavigator} for routing to the desktop after a successful login choice. */
    private Consumer<DesktopScreenController.Role> onLoginAsRole;

    /** Whether the UI is currently in guest login mode. */
    private boolean guestMode;

    /** Regex pattern for validating an email address in guest mode. */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    /** Regex pattern for validating a phone number in guest mode. */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9]{7,15}$");

    /**
     * Injects the shared {@link ClientController} and connection status into this screen.
     * <p>
     * Called by {@link AppNavigator} right after loading the FXML.
     *
     * @param controller the shared client controller used for requests/session state
     * @param connected  whether the client is currently connected to the server
     */
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        updateConnectionStateUI();
    }

    /**
     * Sets a callback that is invoked when the user clicks "Back".
     *
     * @param cb callback that returns to the main menu screen
     */
    public void setOnBackToMain(Runnable cb) {
        this.onBackToMain = cb;
    }

    /**
     * Sets a callback invoked when login flow should continue to desktop with the chosen role.
     * <p>
     * In guest mode, the controller triggers this immediately after starting a guest session.
     * In subscriber mode, navigation is typically triggered later when the server confirms login.
     *
     * @param cb callback receiving the role to open the desktop with
     */
    public void setOnLoginAsRole(Consumer<DesktopScreenController.Role> cb) {
        this.onLoginAsRole = cb;
    }

    /**
     * Returns the username string used for the desktop welcome message.
     *
     * @return trimmed username text, or empty string if not available
     */
    public String getUsernameForWelcome() {
        if (usernameField == null) return "";
        return usernameField.getText() == null ? "" : usernameField.getText().trim();
    }

    /**
     * Initializes the screen after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Sets default UI state and enables Enter-to-submit on the password field.
     */
    @FXML
    private void initialize() {
        if (statusLabel != null) statusLabel.setText("");

        if (passwordField != null) {
            passwordField.setOnAction(e -> onLoginClicked());
        }

        setGuestMode(false);
    }

    /**
     * Button handler for "Login" / "Continue as guest".
     * <p>
     * Behavior depends on current mode:
     * <ul>
     *   <li><b>Guest mode</b>: validates contact (email or phone), starts guest session, routes to desktop.</li>
     *   <li><b>Subscriber mode</b>: sends username/password to server via {@link ClientController#requestLogin}.</li>
     * </ul>
     */
    @FXML
    private void onLoginClicked() {

        if (guestMode) {
            String contact = guestContactField == null ? "" : guestContactField.getText();
            contact = contact == null ? "" : contact.trim();

            if (!isValidContact(contact)) {
                setStatus("Enter a valid email or phone number.", true);
                return;
            }

            if (clientController != null) {
                clientController.startGuestSession(contact);
            }

            setStatus("Continuing as guest...", false);

            if (onLoginAsRole != null) {
                onLoginAsRole.accept(DesktopScreenController.Role.GUEST);
            }
            return;
        }

        String username = usernameField == null ? "" : usernameField.getText();
        String password = passwordField == null ? "" : passwordField.getText();

        if (clientController == null) {
            setStatus("Client controller not ready.", true);
            return;
        }

        clientController.requestLogin(username, password);
        setStatus("Logging in...", false);
    }

    /**
     * Button handler that toggles between subscriber login mode and guest mode.
     */
    @FXML
    private void onContinueAsGuestClicked() {
        setGuestMode(!guestMode);
    }

    /**
     * Button handler for "Back".
     * <p>
     * If {@link #onBackToMain} is provided by {@link AppNavigator}, it is invoked.
     * Otherwise, the method falls back to closing the current {@link Stage}.
     *
     * @param event the originating UI event
     */
    @FXML
    private void onBackClicked(ActionEvent event) {
        if (onBackToMain != null) {
            onBackToMain.run();
            return;
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    /**
     * Updates UI state based on current connection status.
     * <p>
     * Currently enables the login button and displays a warning if not connected.
     */
    private void updateConnectionStateUI() {
        if (loginButton != null) loginButton.setDisable(false);

        if (statusLabel != null && !connected) {
            setStatus("No server connection", true);
        }
    }

    /**
     * Updates the on-screen status label and sets its style based on severity.
     *
     * @param message text to display
     * @param error   whether this status is an error (affects styling)
     */
    private void setStatus(String message, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(message);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    /**
     * Switches UI between subscriber mode and guest mode.
     * <p>
     * Shows/hides relevant input fields and updates button labels accordingly.
     *
     * @param guest {@code true} to enable guest mode; {@code false} for subscriber login mode
     */
    private void setGuestMode(boolean guest) {
        this.guestMode = guest;

        if (usernameField != null) {
            usernameField.setVisible(!guest);
            usernameField.setManaged(!guest);
        }
        if (passwordField != null) {
            passwordField.setVisible(!guest);
            passwordField.setManaged(!guest);
        }

        if (guestContactField != null) {
            guestContactField.setVisible(guest);
            guestContactField.setManaged(guest);
            if (!guest) {
                guestContactField.clear();
            }
        }

        if (loginButton != null) {
            loginButton.setText(guest ? "Continue as guest" : "Login");
        }
        if (guestButton != null) {
            guestButton.setText(guest ? "Back to member login" : "Continue as guest");
        }

        setStatus("", false);
    }

    /**
     * Validates a guest contact string as either an email address or phone number.
     *
     * @param value raw input value
     * @return {@code true} if value matches email or phone pattern; otherwise {@code false}
     */
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
