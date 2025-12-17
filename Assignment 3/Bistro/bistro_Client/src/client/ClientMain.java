package client;

import controllers.ClientController;
import main_screen.DesktopUI;

public class ClientMain {

    public static void main(String[] args) {

        // config connection
        String host = "localhost";
        int port = 3000;

        // networking
        BistroEchoClient echoClient = new BistroEchoClient(host, port);

        //application controller
        ClientController controller = new ClientController(echoClient);
        echoClient.setClientController(controller);

        // connect (non-fatal)
        boolean connected = false;
        try {
            echoClient.openConnection();
            connected = true;
            System.out.println("[CLIENT] Connected to " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("[CLIENT] Connection failed: " + e.getMessage());
        }

        // launch JavaFX UI
        DesktopUI.launchUI(controller, connected);
    }
}
