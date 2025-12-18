package main_screen;

import controllers.ClientController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.TextFormatter;

public class TerminalScreenController {

    @FXML private TextField reservationNumberField;

    private ClientController clientController;
    private boolean connected;

    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void initialize() {
        // Digits only (and optional length limit)
        reservationNumberField.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (!newText.matches("\\d*")) return null;   // numbers only
            if (newText.length() > 10) return null;      // optional limit (change/remove)
            return change;
        }));
    }

    @FXML
    private void onBackClicked(ActionEvent event) {
        Stage s = (Stage) ((Node) event.getSource()).getScene().getWindow();
        s.close();
    }

    @FXML
    private void onEnterClicked(ActionEvent event) {
        String txt = reservationNumberField.getText() == null ? "" : reservationNumberField.getText().trim();
        if (txt.isEmpty()) return; // you can show alert via UIHandler if you want

        int reservationNumber = Integer.parseInt(txt);

        try {
            // Load the next screen (a completely new UI)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/TerminalResultScreen.fxml"));
            Parent nextRoot = loader.load();

            // If next screen also needs ClientController:
            //TerminalResultScreenController next = loader.getController();
           // next.setClientController(clientController, connected);
            //next.setReservationNumber(reservationNumber);

            // Replace content in the SAME window
            Stage s = (Stage) ((Node) event.getSource()).getScene().getWindow();
            s.getScene().setRoot(nextRoot);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
