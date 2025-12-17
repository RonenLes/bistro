package controllers;

import client.BistroEchoClient;

/**
 * Central application controller for the Bistro Echo Client.
 * - All JavaFX screen controllers call THIS class (UI -> Controller)
 * - Only THIS class sends requests to server (Controller -> Network)
 * - Only THIS class routes responses from server (Network -> Controller)
 *
 * Important: OCSF calls back on a non-JavaFX thread.
 * This controller must NOT directly touch JavaFX.
 */
public class ClientController {

    private final BistroEchoClient client;
    private ClientUIHandler ui;

    public ClientController(BistroEchoClient client) {
        this.client = client;
    }

    /** Called once during UI startup */
    public void setUIHandler(ClientUIHandler ui) {
        this.ui = ui;
    }


    // UI --> Controller API


    /**
     * Generic send method. Screens can call this with any request object
     * (e.g., LoginRequest, ReservationRequest, etc.) from your common package.
     */
    public void sendRequest(Object request) {
        if (request == null) {
            safeUiError("Client Error", "Tried to send a null request.");
            return;
        }

        try {
            client.sendToServer(request);
        } catch (Exception e) {
            safeUiError("Connection Error",
                    "Failed to send request to server.\n" + e.getMessage());
        }
    }

    // TODO: implement when request classe ready
    // public void requestLogin(LoginRequest req) { sendRequest(req); }
    // public void requestCreateReservation(ReservationRequest req) { sendRequest(req); }
    // public void requestJoinWaitingList(WaitingListRequest req) { sendRequest(req); }

    // =========================
    // Network -> Controller
    // =========================

    /** Called ONLY by BistroEchoClient.handleMessageFromServer(msg) */
    public void handleServerResponse(Object msg) {
        if (msg == null) {
            safeUiWarning("Server", "Received empty message.");
            return;
        }

        // If you have common response base types, route them here with instanceof
        // Examples:
        //
        // if (msg instanceof ErrorResponse err) { safeUiError("Error", err.toString()); return; }
        // if (msg instanceof ReservationResponse res) { safeUiInfo("Reservation", res.toString()); return; }
        // if (msg instanceof ShowDataResponse data) { ui.showPayload(data); return; }

        // For now (works immediately):
        uiPayload(msg);
    }

    // Safe UI calls

    private void safeUiInfo(String title, String message) {
        if (ui != null) ui.showInfo(title, message);
        else System.out.println("[INFO] " + title + ": " + message);
    }

    private void safeUiWarning(String title, String message) {
        if (ui != null) ui.showWarning(title, message);
        else System.out.println("[WARN] " + title + ": " + message);
    }

    private void safeUiError(String title, String message) {
        if (ui != null) ui.showError(title, message);
        else System.err.println("[ERROR] " + title + ": " + message);
    }
 
    private void uiPayload(Object payload) {
        if (ui != null) ui.showPayload(payload);
        else System.out.println("[PAYLOAD] " + payload);
    } 
}
