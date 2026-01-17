package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Desktop placeholder controller for waitlist-related UI.
 * <p>
 * This screen currently shows a simple status message and does not perform real waitlist actions.
 * The full waitlist flow is handled by other (terminal) screens in the application.
 * <p>
 * Implements {@link ClientControllerAware} so the parent desktop shell can inject the
 * {@link ClientController} reference and connection status.
 */
public class WaitlistViewController implements ClientControllerAware {

    /** Status label used to display placeholder messages to the user. */
    @FXML private Label statusLabel;

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;
    /** Whether the client is currently connected to the server (injected by parent shell). */
    private boolean connected;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     */
    @FXML
    private void initialize() {
        setStatus("Waitlist ready.");
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this view controller.
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
     * Button handler (placeholder): simulates adding the current user to the waitlist.
     * <p>
     * This method currently updates only the UI status label and does not send a server request.
     */
    @FXML
    private void onAddToWaitlist() {
        setStatus("Added to waitlist (placeholder).");
    }

    /**
     * Updates the status label text.
     *
     * @param msg message to display (may be {@code null})
     */
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }
}
