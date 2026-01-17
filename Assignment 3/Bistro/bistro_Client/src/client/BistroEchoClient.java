package client;

import controllers.ClientController;
import ocsf.client.AbstractClient;

/**
 * BistroEchoClient
 *
 * Thin OCSF client responsible ONLY for network communication.
 * This class must remain logic-free and UI-free.
 */
// extends OCSF's AbstractClient to handle low-level socket communication
// delegates all message processing to ClientController
/**
 * 
 */
public class BistroEchoClient extends AbstractClient {

    // reference to the controller that processes incoming messages
    private ClientController controller;

    // initialize connection parameters (does not connect yet)
    public BistroEchoClient(String host, int port) {
        super(host, port);
    }

  
    
    /**
     * set up controller
     * dependency injection: allows ClientController to receive server messages
     * @param controller
     */
    /**
     * @param controller
     */
    public void setClientController(ClientController controller) {
        this.controller = controller;
    }

    /**
     * Called automatically by OCSF when a message arrives from the server.
     * Simply forwards the message to ClientController.
     */
    // Server -> BistroEchoClient -> ClientController -> ClientUIHandler -> show alert / swap screen / update UI
    @Override
    protected void handleMessageFromServer(Object msg) {
        if (controller != null) {
            controller.handleServerResponse(msg);
        } else {
            System.err.println("[BistroEchoClient] No ClientController set");
        }
    }

    // lifecycle hooks
    // these methods are called automatically by OCSF framework during connection lifecycle

    // called when TCP connection succeeds
    /**
     * 
     *called when TCP connection succeeds
     */
    @Override
    protected void connectionEstablished() {
        System.out.println("[BistroEchoClient] Connected to server");
    }

   
    /**
     *called when connection is intentionally closed
     */
    @Override
    protected void connectionClosed() {
        System.out.println("[BistroEchoClient] Connection closed");
        if (controller != null) {
            controller.handleConnectionLost("Connection to server was closed.");
        }
    }

    
    /**
     *called when connection fails or drops unexpectedly
     */
    @Override
    protected void connectionException(Exception exception) {
        System.err.println("[BistroEchoClient] Connection error: " + exception.getMessage());
        if (controller != null) {
            controller.handleConnectionLost("Connection error: " + exception.getMessage());
        }
    }
    
    
}
