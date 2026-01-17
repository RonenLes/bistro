package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import desktop_screen.DesktopScreenController;

/**
 * JavaFX controller for the "Add Subscriber" (add new user) manager screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Collects new user details (username, password, phone, email, role).</li>
 *   <li>Applies basic client-side validation (required fields, password length, role selection).</li>
 *   <li>Restricts role options based on the role of the requester (representative vs manager).</li>
 *   <li>Sends {@link ManagerRequest} with {@link ManagerCommand#ADD_NEW_USER} to the server.</li>
 *   <li>Receives and renders server results via {@link #handleManagerResponse(ManagerResponse)}.</li>
 * </ul>
 */
public class AddSubscriberScreenController implements ClientControllerAware {

    /** Username field for the new user. */
    @FXML private TextField usernameField;
    /** Password field for the new user. */
    @FXML private TextField passwordField;
    /** Phone field for the new user (optional depending on server rules). */
    @FXML private TextField phoneField;
    /** Email field for the new user (optional depending on server rules). */
    @FXML private TextField emailField;
    /** Status/info label for validation errors and server responses. */
    @FXML private Label infoLabel;
    /** Role selection for the new user (restricted by requester role). */
    @FXML private ComboBox<String> roleComboBox;

    /** Reference to the shared client controller used for server communication (injected by parent controller). */
    private ClientController clientController;

    /** Role of the user who opened this screen; used to determine permissions and allowed roles. */
    private DesktopScreenController.Role requesterRole;

    /** Whether the client is currently connected to the server (injected by parent controller). */
    private boolean connected;

    /**
     * Initializes the screen after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     */
    @FXML
    private void initialize() {

    }

    /**
     * Injects the {@link ClientController} reference and connection status into this controller.
     * <p>
     * Called by the parent UI (typically {@code DesktopScreenController}) after loading the FXML.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * Sets the role of the requester (the user who opened this screen) and configures role options accordingly.
     * <p>
     * Representatives are restricted to adding only subscribers.
     * Managers can add subscribers, representatives, and managers.
     *
     * @param requesterRole role of the requester opening this screen
     */
    public void setRequesterRole(DesktopScreenController.Role requesterRole) {
        this.requesterRole = requesterRole;
        configureRoleOptions();
    }

    /**
     * Button handler: validates input fields and sends a request to add a new user.
     * <p>
     * Sends {@link ManagerCommand#ADD_NEW_USER} to the server.
     */
    @FXML
    private void onSave() {
        if (!readyForServer()) return;

        String username = getValue(usernameField);
        String password = getValue(passwordField);
        String phone = getValue(phoneField);
        String email = getValue(emailField);
        String role = roleComboBox == null ? "" : String.valueOf(roleComboBox.getValue());

        if (password.length() < 6 || password.length() > 10) {
            setInfo("password musn be 6-10 chars long.");
            return;
        }

        if (username.isEmpty() || password.isEmpty()) {
            setInfo("Username and password are required.");
            return;
        }
        if (role == null || role.isBlank()) {
            setInfo("Role is required.");
            return;
        }

        ManagerRequest request = new ManagerRequest(
                ManagerCommand.ADD_NEW_USER,
                username,
                password,
                phone,
                email,
                role
        );

        clientController.requestManagerAction(request);
        setInfo("Submitting new subscriber...");

    }

    /**
     * Button handler: clears all input fields and removes any status message.
     */
    @FXML
    private void onClear() {
        clearFields();
        setInfo("");
    }

    /**
     * Handles server responses related to the add-user flow.
     * <p>
     * When receiving {@link ManagerResponseCommand#NEW_USER_RESPONSE}, displays a success message
     * and clears the input fields.
     *
     * @param response the server response to handle; ignored if {@code null}
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        if (response.getResponseCommand() == ManagerResponseCommand.NEW_USER_RESPONSE) {
            String newId = response.getUserID();
            setInfo(newId == null ? "Subscriber added." : "Subscriber added. ID: " + newId);

            clearFields();
        }
    }

    /**
     * Clears all input fields and resets the role selection to {@code SUBSCRIBER}.
     */
    private void clearFields() {
        if (usernameField != null) usernameField.clear();
        if (passwordField != null) passwordField.clear();
        if (phoneField != null) phoneField.clear();
        if (emailField != null) emailField.clear();
        if (roleComboBox != null) roleComboBox.getSelectionModel().select("SUBSCRIBER");
    }

    /**
     * Configures the available role options based on {@link #requesterRole}.
     * <p>
     * If requester is a representative, only {@code SUBSCRIBER} is allowed (and selection is locked).
     * Otherwise, allows {@code SUBSCRIBER}, {@code REPRESENTATIVE}, and {@code MANAGER}.
     */
    private void configureRoleOptions() {
        if (roleComboBox == null) return;

        boolean subscriberOnly = requesterRole == DesktopScreenController.Role.REP;
        if (subscriberOnly) {
            roleComboBox.setItems(FXCollections.observableArrayList("SUBSCRIBER"));
            roleComboBox.getSelectionModel().select("SUBSCRIBER");
            roleComboBox.setDisable(true);
        } else {
            roleComboBox.setItems(FXCollections.observableArrayList("SUBSCRIBER", "REPRESENTATIVE", "MANAGER"));
            roleComboBox.getSelectionModel().select("SUBSCRIBER");
            roleComboBox.setDisable(false);
        }
    }

    /**
     * Safely extracts and trims the text from a {@link TextField}.
     *
     * @param field the field to read from
     * @return trimmed text, or empty string if field/text is {@code null}
     */
    private String getValue(TextField field) {
        if (field == null || field.getText() == null) return "";
        return field.getText().trim();
    }

    /**
     * Checks whether the controller is connected and ready to send requests to the server.
     *
     * @return {@code true} if connected and {@link #clientController} is set; otherwise {@code false}
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Updates the on-screen info label.
     *
     * @param msg message to display (null-safe)
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
