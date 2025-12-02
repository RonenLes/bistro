package serverGui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import server.BistroEchoServer;

public class NetworkInfoController {

    @FXML
    private Label hostLabel;    

    @FXML
    private Label ipLabel;     

    private BistroEchoServer server;
    
    @FXML
    private Button btnStart;

   
    public void setServer(BistroEchoServer server) {
        this.server = server;
    }

   
    @FXML
    private void initialize() {
    	this.btnStart.setText("Start Server");
    }
    
    @FXML
    private void showConnectionInfo() {

        if (server == null) {
            hostLabel.setText("Server is not running");
            ipLabel.setText("");
            return;
        }
        String serverHost = server.getServerHostName();
        String serverIp   = server.getServerIpAddress();
        String clientIp = server.getLastClientIp();
        this.btnStart.setText("Show info");
        hostLabel.setText("Server Host: " + serverHost + " (" + serverIp + ")");
        ipLabel.setText("Client IP: " + clientIp);
    }

}
