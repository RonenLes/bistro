package application;

import controllers.ClientController;
import ocsf.client.AbstractClient;

public class BistroClient extends AbstractClient {

    private ClientController controller;

    public BistroClient(String host, int port) {
        super(host, port);
    }

    public void setClientController(ClientController controller) {
        this.controller = controller;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (controller != null) {
            controller.handleServerResponse(msg);
        } else {
            System.err.println("No ClientController set to handle server messages.");
        }
    }
}
