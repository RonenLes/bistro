package controllers;

import application.BistroClient;
import application.ClientUI;
import gui.EditReservationFormController;
import gui.ShowAllReservationFormController;
import responses.*;

public class ClientController {

    private BistroClient client;

    public ClientController(BistroClient client) {
        this.client = client;
    }

    public void handleServerResponse(Object msg) {

    	try {
            if (msg instanceof ErrorResponse) {
                ClientUI.handleError((ErrorResponse) msg);
            }

            else if (msg instanceof ShowDataResponse) {
                ShowDataResponse res = (ShowDataResponse) msg;

                ShowAllReservationFormController showAllCtrl = ShowAllReservationFormController.getActiveInstance();
                if (showAllCtrl != null) {
                    showAllCtrl.handleShowDataFromServer(res);
                    return;
                }

                EditReservationFormController editCtrl = EditReservationFormController.getActiveInstance();
                if (editCtrl != null) {
                    editCtrl.handleShowDataFromServer(res);
                    return;
                }

                ClientUI.handleShowData(res);
            }

            else if (msg instanceof ReservationResponse) {
                ClientUI.handleReservationResponse((ReservationResponse) msg);
            }

            else {
                System.out.println("Unknown server message: " + msg);
            }
        } catch (Exception e) {
        	System.err.println("failed handleServerResponse");
            e.printStackTrace();
        }
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
    
    public void closeClient() {
    	try {
    		this.client.closeConnection();
    	}catch(Exception e) {
    		System.err.println("Failed to close client: " + e.getMessage());
    	}
    }
}
