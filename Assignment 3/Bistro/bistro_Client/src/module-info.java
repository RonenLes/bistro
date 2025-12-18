module bistro_Client {
	exports customer_screen;
	exports subscriber_screen;
	exports manager_screen;
	exports client;
	exports main_screen;

	requires OCSF;
	requires javafx.graphics;
	requires javafx.fxml;
	requires javafx.controls;
	requires bistro_Common;
	
	opens main_screen to javafx.fxml;
	//for later
	opens customer_screen to javafx.fxml;
	opens manager_screen to javafx.fxml;
	opens subscriber_screen to javafx.fxml;
}