package main_screen;

import controllers.ClientController;
import controllers.ClientUIHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import terminal_screen.TerminalScreenController;

public class MainScreenController extends Application implements ClientUIHandler {

    // =========================
    // Static boot injection
    // =========================
    private static ClientController controller;
    private static boolean connected;

    public static void launchUI(ClientController c, boolean isConnected) {
        controller = c;
        connected = isConnected;
        launch();
    }

    // =========================
    // JavaFX state
    // =========================
    private Stage stage;

    // =========================
    // FXML fields
    // =========================
    @FXML private Label connectionStatusLabel;

    // =========================
    // Application entry point
    // =========================
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setResizable(false);


        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/MainScreen.fxml"));

            // CRITICAL: reuse THIS instance as the controller, so @FXML fields inject here
            loader.setControllerFactory(type -> {
                if (type == MainScreenController.class) return this;
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load(); // injects @FXML fields into THIS instance

            // Allow ClientController -> UI callbacks
            if (controller != null) {
                controller.setUIHandler(this);
                controller.setConnected(connected);
            }

            // Now that FXML injected, update label safely
            updateConnectionLabel();

            stage.setScene(new Scene(root));
            stage.setTitle("Bistro Client");
            stage.show();

            if (!connected) {
                showWarning("Offline Mode", "Server connection failed. UI is running offline.");
            }
            


        } catch (Exception e) {
            e.printStackTrace();
            showError("Fatal Error", "Failed to load MainScreen.fxml:\n" + e.getMessage());
        }
    }

    private void updateConnectionLabel() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(connected ? "Connection: Connected" : "Connection: Offline");
        }
    }

    // =========================
    // Button handlers (from FXML)
    // =========================
    @FXML
    private void onRemoteClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/DesktopScreen.fxml"));
            Parent root = loader.load();

            DesktopScreenController c = loader.getController();
            c.setClientController(controller, connected);

            Stage desktopStage = new Stage();
            desktopStage.setTitle("Desktop Login");
            desktopStage.setScene(new Scene(root));

            // IMPORTANT: bring main back when desktop closes
            desktopStage.setOnHidden(e -> this.stage.show());
            desktopStage.setOnCloseRequest(e -> this.stage.show());

            desktopStage.show();
            this.stage.hide();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to open DesktopScreen.\n" + e.getMessage());
        }
    }

    @FXML
    private void onTerminalClicked() {
        try {
            String fxml = "/terminal_screen/TerminalScreen.fxml";

            var url = getClass().getResource(fxml);
            if (url == null) {
                throw new IllegalArgumentException("FXML not found on classpath: " + fxml);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // Inject controller dependencies
            terminal_screen.TerminalScreenController ctrl = loader.getController();
            ctrl.setClientController(controller, connected);

            // Swap in same window
            stage.getScene().setRoot(root);
            stage.sizeToScene(); 
            stage.centerOnScreen();
            stage.setTitle("Terminal");

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to open Terminal.\n" + e.getMessage());
        }
    }

    // Optional helper for navigation later
    private void loadScreen(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            stage.getScene().setRoot(root);
            stage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to load: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    // =========================
    // ClientUIHandler impl
    // =========================
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
