package gui;

import application.BistroClient;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.net.Socket;

public class NetworkInfoController {

    @FXML
    private Label ipLabel;

    @FXML
    private Label hostLabel;

    // זה ה־CLIENT האמיתי שמחובר לשרת (OCSF)
    private BistroClient client;

    // נקרא מהקוד שפותח את ה־FXML
    public void setClient(BistroClient client) {
        this.client = client;
    }

    @FXML
    private void showConnectionInfo() {
        if (client == null) {
            ipLabel.setText("Client is null");
            hostLabel.setText("");
            return;
        }

        try {
            Socket socket = client.getSocket();   
            String clientIp   = socket.getLocalAddress().getHostAddress();
            String clientHost = socket.getLocalAddress().getHostName();
            ipLabel.setText("Client IP: " + clientIp);
            hostLabel.setText("Client Host: " + clientHost);

        } catch (Exception e) {
            ipLabel.setText("Client IP: error");
            hostLabel.setText("Client Host: error");
            e.printStackTrace();
        }
    }
}
