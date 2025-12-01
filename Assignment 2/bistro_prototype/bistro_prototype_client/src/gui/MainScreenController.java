package gui;

import javafx.application.Platform;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import controllers.ClientController;

public class MainScreenController {
	private ClientController clientController ;
			
	@FXML
	private Button btnMoveToRes;
	
	@FXML
	private Button btnMoveToEdit;
	
	@FXML
	private Button btnMoveShowAll;
	
	@FXML
	private Button btnExt;
	
	public void setClientController(ClientController clientController) {
	    this.clientController = clientController;
	}
	
	@FXML
	private void moveToReservation(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ReservationForm.fxml"));
			Parent root = loader.load();
			
			ReservationFormController controller = loader.getController();
	        controller.setClientController(clientController);
			
			Scene reservationScene = new Scene(root);
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			
			
			stage.setScene(reservationScene);
	        stage.setTitle("Bistro - New Reservation"); 
	        stage.show();
		}catch (Exception e) {
	        System.err.println("Failed to load reservation form: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	@FXML
	private void moveToEdit(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/EditReservationForm.fxml"));
			Parent root = loader.load();
			
			EditReservationFormController controller = loader.getController();
	        controller.setClientController(clientController);
			
			Scene reservationScene = new Scene(root);
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			
			
			stage.setScene(reservationScene);
	        stage.setTitle("Bistro - Edit Reservation"); 
	        stage.show();
		}catch (Exception e) {
	        System.err.println("Failed to load reservation form: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	@FXML
	private void moveToShowAll(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/ShowAllForm.fxml"));
			Parent root = loader.load();
			
			ShowAllReservationFormController controller = loader.getController();
	        controller.setClientController(clientController);
			
			Scene reservationScene = new Scene(root);
			Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
			
			
			stage.setScene(reservationScene);
	        stage.setTitle("Bistro - Show All Reservations"); 
	        stage.show();
		}catch (Exception e) {
	        System.err.println("Failed to load reservation form: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	
	@FXML
	private void exitMainScreen(ActionEvent event) {
		if(clientController != null) {
			clientController.closeClient();
		}
		
		Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
	    stage.close();
	    Platform.exit(); 
	    System.exit(0);
	}
	
	
}
