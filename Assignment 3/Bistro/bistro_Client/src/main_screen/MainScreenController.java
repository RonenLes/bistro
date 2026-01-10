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
<<<<<<< HEAD
=======
import responses.ManagerResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;
import terminal_screen.TerminalScreenController;
>>>>>>> refs/heads/ronen-database/entities

public class MainScreenController extends Application {

    // Static boot injection
    private static ClientController controller;
    private static boolean connected;

    public static void launchUI(ClientController c, boolean isConnected) {
        controller = c;
        connected = isConnected;
        launch();
    }

    private Stage stage;
    private AppNavigator navigator;

    // Keep the already-loaded main root so we can go back without reloading
    private Parent mainRoot;

    @FXML private Label connectionStatusLabel;

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

            stage.setScene(new Scene(root));
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

    private void updateConnectionLabel() {
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(connected ? "Connection: Connected" : "Connection: Offline");
        }
    }

    @FXML
    private void onRemoteClicked() {
        navigator.showLogin();
    }

    @FXML
    private void onTerminalClicked() {
        navigator.showTerminal();
    }

    static void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "" : msg);
            a.showAndWait();
        });
    }
<<<<<<< HEAD
=======

    @Override
    public void routeToDesktop(DesktopScreenController.Role role, String username) {
        Platform.runLater(() -> showDesktopScreen(role, username));
    }
    
    @Override
    public void onReservationResponse(ReservationResponse response) {
        /* * Since OCSF calls this from "Thread-0", we MUST wrap the logic 
         * in Platform.runLater to avoid UI threading exceptions.
         */
        javafx.application.Platform.runLater(() -> {
            // 1. Find the current active controller (your DesktopScreenController)
            // 2. Pass the response to it
            if (desktopController != null) {
                desktopController.onReservationResponse(response);
            }
        });
    }

    @Override
    public void onSeatingResponse(SeatingResponse response) {
        /* * Since OCSF calls this from "Thread-0", we MUST wrap the logic
         * in Platform.runLater to avoid UI threading exceptions.
         */
        javafx.application.Platform.runLater(() -> {
            if (terminalController != null) {
                terminalController.onSeatingResponse(response);
            }
        });
    }
    
    @Override
    public void onManagerResponse(ManagerResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (desktopController != null) {
                desktopController.onManagerResponse(response);
            }
        });
    }
>>>>>>> refs/heads/ronen-database/entities
}
