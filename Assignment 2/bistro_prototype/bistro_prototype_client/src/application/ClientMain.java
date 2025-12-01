package application;

import controllers.ClientController;

public class ClientMain {

    public static void main(String[] args) {

        boolean connected = false;

        // 1. Create the client (no controller yet)
        BistroClient client = new BistroClient("localhost", 3000);

        // 2. Create the controller with the client
        ClientController controller = new ClientController(client);

        // 3. Inject controller back into the client
        client.setClientController(controller);

        // 4. Open connection
        try {
            client.openConnection();
            connected = true;
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }

        // 5. Start JavaFX UI with the controller & connection status
        ClientUI.startUI(controller, connected);
    }
}
