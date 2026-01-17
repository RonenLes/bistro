package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Placeholder view controller for future analytics features.
 * <p>
 * Currently displays a demo message in the analytics screen.
 * Implements {@link ClientControllerAware} to receive a {@link ClientController}
 * reference and connection status from the parent shell.
 */
public class AnalyticsViewController implements ClientControllerAware {

    /** Label used to display the current analytics/demo information message. */
    @FXML private Label infoLabel;

    /** Reference to the client controller used for server communication (injected by the parent shell). */
    private ClientController clientController;
    /** Indicates whether the client is currently connected to the server (injected by the parent shell). */
    private boolean connected;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Sets the default demo message.
     */
    @FXML
    private void initialize() {
        setInfo("Analytics dashboard (demo).");
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
     * Sets the informational text shown on the screen.
     *
     * @param msg the message to display
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg);
    }
}
