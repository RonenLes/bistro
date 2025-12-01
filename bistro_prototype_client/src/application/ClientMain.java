package application;

import controllers.ClientController;

public class ClientMain {

    public static void main(String[] args) {

        boolean connected = false;

        ClientController controller = new ClientController(null);
        BistroClient client = new BistroClient("localhost", 3000, controller);

        controller = new ClientController(client);

        try {
            client.openConnection();
            connected = true;
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }

        // Start UI and pass the controller and connection result
        ClientUI.startUI(controller, connected);
    }
}

