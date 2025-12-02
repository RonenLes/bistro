package application;

import controllers.ClientController;
import ocsf.client.AbstractClient;

public class BistroClient extends AbstractClient {

    private ClientController controller;

    public BistroClient(String host, int port, ClientController controller) {
        super(host, port);
        this.controller = controller;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        controller.handleServerResponse(msg);
    }
}