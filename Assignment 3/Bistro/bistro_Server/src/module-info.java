module bistro_Server {
	requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    requires java.sql;
    requires com.zaxxer.hikari;

    requires OCSF;
    requires bistro_Common;
	requires junit;
	

    // --- JavaFX FXML reflection access ---
    opens serverGUI to javafx.fxml;
    opens server to javafx.graphics;
  
}