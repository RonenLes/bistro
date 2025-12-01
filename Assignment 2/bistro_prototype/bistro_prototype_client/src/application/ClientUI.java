package application;

import controllers.ClientController;
import entities.Reservation;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import requests.*;
import responses.*;

public class ClientUI extends Application {

    private static ClientController controller;
    private static boolean isConnected;
    private static ClientUI instance;

    private TextArea outputArea;

    // start UI from ClientMain (receives connection status)
    public static void startUI(ClientController c, boolean connected) {
        controller = c;
        isConnected = connected;
        launch();
    }

    @Override
    public void start(Stage stage) {

        instance = this;

        // CONNECTION STATUS ALERT
        if (isConnected) {
            showAlert("Connected", 
                    "Successfully connected to the Bistro server.",
                    Alert.AlertType.INFORMATION);
        } else {
            showAlert("Connection Failed",
                    "Could not connect to the Bistro server.\n"
                    + "The UI will still open, but actions may fail.",
                    Alert.AlertType.ERROR);
        }

        // UI ELEMENTS
        Button loadBtn = new Button("Load Reservations");
        loadBtn.setPrefWidth(180);
        loadBtn.setOnAction(e ->
                controller.handleMessageFromClientUI(
                        new ShowDataRequest(
                                CommandType.READ_ALL_EXISTING_RESERVATIONWS, 0)));

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setPrefWidth(180);
        refreshBtn.setOnAction(e ->
                controller.handleMessageFromClientUI(
                        new ShowDataRequest(
                                CommandType.READ_ALL_EXISTING_RESERVATIONWS, 0)));

        Button exitBtn = new Button("Exit");
        exitBtn.setPrefWidth(180);
        exitBtn.setOnAction(e -> Platform.exit());

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(300);

        VBox buttons = new VBox(10, loadBtn, refreshBtn, exitBtn);
        buttons.setAlignment(Pos.TOP_LEFT);
        buttons.setPadding(new Insets(10));

        BorderPane layout = new BorderPane();
        layout.setLeft(buttons);
        layout.setCenter(outputArea);
        layout.setPadding(new Insets(10));

        Scene scene = new Scene(layout, 600, 400);

        stage.setScene(scene);
        stage.setTitle("Bistro Client");
        stage.show();
    }

     //RESPONSE HANDLERS

    public static void handleError(ErrorResponse res) {
        showAlert("Error", res.toString(), Alert.AlertType.ERROR);
    }

    public static void handleShowData(ShowDataResponse res) {

        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();

            if (!res.getIsSuccess()) {
                sb.append("Server Error:\n").append(res.toString());
            }
            else if (res.getReservationList() == null || res.getReservationList().isEmpty()) {
                sb.append("No reservations found.");
            }
            else {
                for (Reservation r : res.getReservationList()) {
                    sb.append("Reservation ID: ").append(r.getReservationID())
                      .append("\nDate: ").append(r.getReservationDate())
                      .append("\nGuests: ").append(r.getNumberOfGuests())
                      .append("\nConfirmation Code: ").append(r.getConfirmationCode())
                      .append("\nSubscriber ID: ").append(r.getSubscriberId())
                      .append("\nPlaced On: ").append(r.getDateOfPlacingOrder())
                      .append("\n------------------------------\n");
                }
            }

            instance.outputArea.setText(sb.toString());
        });
    }

    public static void handleReservationResponse(ReservationResponse res) {
        String resStatus = res.getIsReservationSuccess() ? "Successful" : "Failed";

        showAlert("Reservation Status", resStatus, Alert.AlertType.INFORMATION);
    }

    // ALERT POPUP   
    private static void showAlert(String title, String msg, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }
}
