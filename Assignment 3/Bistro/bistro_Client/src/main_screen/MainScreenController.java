package main_screen;

import controllers.ClientController;
import controllers.ClientUIHandler;
import desktop_screen.DesktopScreenController;
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

    // Keep the already-loaded main root so we can go back without reloading
    private Parent mainRoot;

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

            Parent root = loader.load();         // injects @FXML fields into THIS instance
            this.mainRoot = root;                // <-- SAVE MAIN ROOT

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

    @FXML
    private void onRemoteClicked() {
        showLoginScreen();
    }

    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/LoginScreen.fxml"));
            Parent loginRoot = loader.load();

            LoginScreenController loginCtrl = loader.getController();
            loginCtrl.setClientController(controller, connected);

            // Back goes to main root (no reload)
            loginCtrl.setOnBackToMain(() -> {
                stage.getScene().setRoot(mainRoot);
                stage.setTitle("Bistro Client");
                stage.sizeToScene();
                stage.centerOnScreen();
                updateConnectionLabel();
            });

            // Temp “role select” login => open Desktop with role
            loginCtrl.setOnLoginAsRole(role -> showDesktopScreen(role, loginCtrl.getUsernameForWelcome()));

            stage.getScene().setRoot(loginRoot);
            stage.setTitle("Login");
            stage.sizeToScene();
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to open LoginScreen.\n" + e.getMessage());
        }
    }

    private void showDesktopScreen(desktop_screen.DesktopScreenController.Role role, String welcomeName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop_screen/DesktopScreen.fxml"));
            Parent desktopRoot = loader.load();

            DesktopScreenController desktopCtrl = loader.getController();
            desktopCtrl.setClientController(controller);     // your Desktop controller already has this
            desktopCtrl.setRole(role);
            desktopCtrl.setWelcomeName(welcomeName);

            // Logout goes back to main (you can change to showLoginScreen() if you prefer)
            desktopCtrl.setOnLogout(() -> {
                stage.getScene().setRoot(mainRoot);
                stage.setTitle("Bistro Client");
                stage.sizeToScene();
                stage.centerOnScreen();
                updateConnectionLabel();
            });

            stage.getScene().setRoot(desktopRoot);
            stage.setTitle("Desktop");
            stage.sizeToScene();
            stage.centerOnScreen();

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
            Parent terminalRoot = loader.load();

            // Inject controller dependencies
            TerminalScreenController ctrl = loader.getController();
            ctrl.setClientController(controller, connected);

            // ✅ Back callback: restore original main root (no re-load)
            ctrl.setOnBackToMain(() -> {
                stage.getScene().setRoot(mainRoot);
                stage.setTitle("Bistro Client");
                stage.sizeToScene();
                stage.centerOnScreen();
                updateConnectionLabel();
            });

            // Swap in same window
            stage.getScene().setRoot(terminalRoot);
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
