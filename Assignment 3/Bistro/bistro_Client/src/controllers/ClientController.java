package controllers;

import client.BistroEchoClient;
import desktop_screen.DesktopScreenController;
import kryo.KryoUtil;
import requests.LoginRequest;
import responses.LoginResponse;
import responses.Response;

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
    private boolean connected;


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
        if (!connected) {
            safeUiWarning("Offline Mode", "Not connected to server. This action is unavailable.");
            return;
        }

        try {
            // Convert to bytes using Kryo
            byte[] payload = KryoUtil.serialize(request);

            // Send the byte array using OCSF
            client.sendToServer(payload); 
        	}
         catch (Exception e) {
            safeUiError("Connection Error", "Failed to send request to server.\n" + e.getMessage());
        }
    }


    // Network -> Controller
    /** Called ONLY by BistroEchoClient.handleMessageFromServer(msg) */
    public void handleServerResponse(Object msg) {
        try {
            Object decoded = msg;
            if (msg instanceof byte[] bytes) {
                decoded = KryoUtil.deserialize(bytes);
            }

            if (!(decoded instanceof Response<?> response)) {
                safeUiInfo("Failed", "failed to deserialize response");
                return;
            }

            if (!response.isSuccess()) {
                safeUiError("Failed operation", response.getMessage());
                return;
            }

            Object responseData = response.getData();

            if (responseData instanceof LoginResponse loginResponse) {
                switch (loginResponse.getResponseCommand()) {
                    case LOGIN_RESPONSE -> {
                        DesktopScreenController.Role uiRole =
                                mapRoleFromServer(loginResponse.getRole());

                        // THIS is where we move to the next screen
                        if (ui != null) {
                        	//move on with the respected role, and username
                            ui.routeToDesktop(uiRole, loginResponse.getUsername());
                        } else {
                            System.out.println("[INFO] Login success for " +
                                    loginResponse.getUsername() + " as " + uiRole);
                        }
                    }
                    case EDIT_RESPONSE -> {
                        // handle other login-related responses if needed
                    }
                }
            }

            // handle other response types here (Reservations, etc.)

        } catch (Exception e) {
            safeUiError("Client Error", "Error handling server response:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Login and information verification
    public void requestLogin(String usernameRaw, String passwordRaw) {
        String username = usernameRaw == null ? "" : usernameRaw.trim();
        String password = passwordRaw == null ? "" : passwordRaw;

        String err = validateUsername(username);
        if (err != null) { safeUiWarning("Login", err); return; }

        err = validatePassword(password);
        if (err != null) { safeUiWarning("Login", err); return; }


        sendRequest(new LoginRequest(username, password, LoginRequest.UserCommand.LOGIN_REQUEST));
    }
    
    // helper function for requestLogin
    private String validateUsername(String u) {
        if (u.isEmpty()) return "Username is required.";
        if (u.length() < 3 || u.length() > 20) return "Username must be 3-20 characters.";
        if (u.contains(" ")) return "Username cannot contain spaces.";
        if (!u.matches("[A-Za-z0-9._-]+")) return "Username can only contain letters, numbers, . _ -";
        return null;
    }
    
    // helper function for requestLogin
    private String validatePassword(String p) {
        if (p.isEmpty()) return "Password is required.";
        if (p.length() < 6 || p.length() > 64) return "Password must be 6-64 characters.";
        //allow symbols/spaces on password
        return null;
    }
    // END
    
    private DesktopScreenController.Role mapRoleFromServer(String rawRole) {
        if (rawRole == null) {
            return DesktopScreenController.Role.GUEST;
        }

        String normalized = rawRole.trim().toUpperCase();

        // server sends exact enum names
        try {
            return DesktopScreenController.Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Fallback – don't crash UI if server sends unexpected role
            System.err.println("[WARN] Unknown role from server: " + rawRole);
            return DesktopScreenController.Role.GUEST;
        }
    }

    public void handleConnectionLost(String message) {
        if (ui != null) {
            ui.showError("Connection Lost", message);
        }
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
    
    // Setters
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
