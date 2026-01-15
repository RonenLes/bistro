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

// JavaFX application entry point and main menu screen
// displays connection status and navigation buttons
// delegates actual navigation to AppNavigator
public class MainScreenController extends Application {

    // Static boot injection
    // controller passed from ClientMain before JavaFX launch
    private static ClientController controller;
    private static boolean connected;

    // called by ClientMain to start JavaFX with controller
    public static void launchUI(ClientController c, boolean isConnected) {
        controller = c;
        connected = isConnected;
        launch();
    }

    private Stage stage;
    // handles all navigation between UI modes
    private AppNavigator navigator;

    // Keep the already-loaded main root so we can go back without reloading
    private Parent mainRoot;

    @FXML private Label connectionStatusLabel;

    // JavaFX lifecycle: called automatically after launch()
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        this.navigator = new AppNavigator(stage, controller, connected);
        stage.setResizable(false);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/MainScreen.fxml"));

            // custom factory to inject this instance as controller
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

            // set up controller with navigation handler
            if (controller != null) {
                controller.setUIHandler(navigator.getNavigationHandler());
                controller.setConnected(connected);
            }

            updateConnectionLabel();

            stage.setScene(new Scene(root));
            stage.setTitle("Bistro Client");
            stage.show();

            // warn user if server connection failed
            if (!connected) {
                showAlert("Offline Mode", "Server connection failed. UI is running offline.", Alert.AlertType.WARNING);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fatal Error", "Failed to load MainScreen.fxml:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // updates connection status label on main screen
    private void updateConnectionLabel() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(connected ? "Connection: Connected" : "Connection: Offline");
        }
    }

    @FXML
    // navigates to login screen (desktop mode)
    private void onRemoteClicked() {
        navigator.showLogin();
    }

    @FXML
    // navigates to terminal screen (walk-in customer mode)
    private void onTerminalClicked() {
        navigator.showTerminal();
    }

    // static helper for showing alerts from any context
    static void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "" : msg);
            a.showAndWait();
        });
    }
    
    @FXML
    // cleanly closes connection and exits application
    private void onExitClicked() {
        if (controller != null) {
            controller.closeConnectionForExit();
        }
        Platform.exit();
    }
}