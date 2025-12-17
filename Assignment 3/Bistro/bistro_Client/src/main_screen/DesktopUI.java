package main_screen;

import controllers.ClientController;
import controllers.ClientUIHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class DesktopUI extends Application implements ClientUIHandler {

    private static ClientController controller;
    private static boolean connected;

    public static void launchUI(ClientController c, boolean isConnected) {
        controller = c;
        connected = isConnected;
        launch();
    }

    @Override
    public void start(Stage stage) {
        try {
            //Load FXML (make sure the path matches your resources layout)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/MainScreen.fxml"));
            Parent root = loader.load();

            //Inject controller into the screen controller
            MainScreenController screen = loader.getController();
            screen.setClientController(controller, connected);

            // Allow ClientController -> UI callbacks
            if (controller != null) {
                controller.setUIHandler(this);
            }

            //Show the window
            stage.setScene(new Scene(root));
            stage.setTitle("Bistro Client");
            stage.show();

            // show connection status popup
            if (!connected) {
                showWarning("Offline Mode", "Server connection failed. UI is running offline.");
            }

        } catch (Exception e) {
            e.printStackTrace(); // IMPORTANT: shows the real reason in console
            showError("Fatal Error", "Failed to load MainScreen.fxml:\n" + e.getMessage());
        }
    }

    @Override
    public void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }

    @Override
    public void showWarning(String title, String message) {
        showAlert(title, message, Alert.AlertType.WARNING);
    }

    @Override
    public void showError(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }

    @Override
    public void showPayload(Object payload) {
        showInfo("Server Message", String.valueOf(payload));
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}
