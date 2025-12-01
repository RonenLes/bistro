package controllers;

import application.BistroClient;
import application.ClientUI;
import responses.*;

public class ClientController {

    private BistroClient client;

    public ClientController(BistroClient client) {
        this.client = client;
    }
    public void setClient(BistroClient client) {
        this.client = client;
    }
    public void updateConnectionStatus(boolean connected) {
        String status = connected ? "not connected" : "connected";
        System.out.println("Connection Status: " + status);
    }
    
    //    SERVER to CLIENT (responses)
    public void handleServerResponse(Object msg) {

        if (msg instanceof ErrorResponse)
            ClientUI.handleError((ErrorResponse) msg);

        else if (msg instanceof ShowDataResponse)
            ClientUI.handleShowData((ShowDataResponse) msg);

        else if (msg instanceof ReservationResponse)
            ClientUI.handleReservationResponse((ReservationResponse) msg);

        else
            System.out.println("Unknown server message: " + msg);
    }

    //    CLIENT UI to SERVER (requests)
    public void handleMessageFromClientUI(Object request) {
        send(request);
    }

    private void send(Object msg) {
        try {
            client.sendToServer(msg);
        } catch (Exception e) {
            System.out.println("Send error: " + e.getMessage());
        }
    }
}
