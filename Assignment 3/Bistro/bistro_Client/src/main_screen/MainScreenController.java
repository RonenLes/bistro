package main_screen;

import controllers.ClientController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * JavaFX application entry point and controller for the main menu screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Bootstraps the JavaFX UI with a {@link ClientController} provided by {@code ClientMain}.</li>
 *   <li>Loads {@code MainScreen.fxml} and shows the primary {@link Stage}.</li>
 *   <li>Displays connection status (connected/offline).</li>
 *   <li>Routes navigation actions to {@link AppNavigator} (login/desktop and terminal modes).</li>
 *   <li>Provides a static alert helper for showing popups from any thread.</li>
 * </ul>
 */
public class MainScreenController extends Application {

    /** Controller injected from {@code ClientMain} before JavaFX launch. */
    private static ClientController controller;
    /** Initial connection status injected from {@code ClientMain}. */
    private static boolean connected;

    /**
     * Launches the JavaFX UI with a pre-created {@link ClientController}.
     * <p>
     * Called by {@code ClientMain} before starting the JavaFX application thread.
     *
     * @param c           the shared client controller instance
     * @param isConnected whether the client is connected to the server
     */
    public static void launchUI(ClientController c, boolean isConnected) {
        controller = c;
        connected = isConnected;
        launch();
    }

    /** Primary JavaFX stage. */
    private Stage stage;
    /** Navigator responsible for switching between screens and setting the active UI handler. */
    private AppNavigator navigator;

    /** Cached main root node so it can be restored without reloading FXML. */
    private Parent mainRoot;

    /** Label that shows current connection status on the main menu screen. */
    @FXML private Label connectionStatusLabel;

    /**
     * JavaFX application lifecycle entry point.
     * <p>
     * Loads {@code MainScreen.fxml}, wires the {@link ClientController} UI handler,
     * and shows the main menu scene.
     *
     * @param stage the primary stage created by JavaFX
     */
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.navigator = new AppNavigator(stage, controller, connected);
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/MainScreen.fxml"));

            loader.setControllerFactory(type -> {
                if (type == MainScreenController.class) return this;
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            this.mainRoot = root;
            navigator.setMainRoot(this.mainRoot);

            if (controller != null) {
                controller.setUIHandler(navigator.getNavigationHandler());
                controller.setConnected(connected);
            }

            updateConnectionLabel();

            Scene scene = new Scene(root);

            var css = getClass().getResource("/styles/app.css");
            System.out.println("CSS url = " + css);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);

            stage.setTitle("Bistro Client");
            stage.show();

            if (!connected) {
                showAlert("Offline Mode", "Server connection failed. UI is running offline.", Alert.AlertType.WARNING);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fatal Error", "Failed to load MainScreen.fxml:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Updates the main screen connection label using the injected {@link #connected} flag.
     */
    private void updateConnectionLabel() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(connected ? "Connection: Connected" : "Connection: Offline");
        }
    }

    /**
     * Button handler: navigates to the login screen (desktop mode entry).
     */
    @FXML
    private void onRemoteClicked() {
        navigator.showLogin();
    }

    /**
     * Button handler: navigates to the terminal screen (walk-in customer mode).
     */
    @FXML
    private void onTerminalClicked() {
        navigator.showTerminal();
    }

    /**
     * Displays a JavaFX alert safely from any thread.
     * <p>
     * This method uses {@link Platform#runLater(Runnable)} to ensure the dialog is shown
     * on the JavaFX application thread.
     *
     * @param title dialog title
     * @param msg   dialog message (null-safe)
     * @param type  alert type (information/warning/error)
     */
    static void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "" : msg);
            a.showAndWait();
        });
    }

    /**
     * Button handler: closes the network connection (if available) and exits the application.
     */
    @FXML
    private void onExitClicked() {
        if (controller != null) {
            controller.closeConnectionForExit();
        }
        Platform.exit();
    }
}
