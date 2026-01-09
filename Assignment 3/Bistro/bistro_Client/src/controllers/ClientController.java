package controllers;

import java.time.LocalDate;
import java.time.LocalTime;

import client.BistroEchoClient;
import desktop_screen.DesktopScreenController;
import kryo.KryoUtil;
//requests
import requests.ManagerRequest;
import requests.BillRequest;
import requests.BillRequest.BillRequestType;
import requests.LoginRequest;
import requests.LoginRequest.UserCommand;
import requests.Request;
import requests.ReservationRequest;
import requests.ReservationRequest.ReservationRequestType;
import requests.SeatingRequest;
import requests.SeatingRequest.SeatingRequestType;
//responses
import responses.ManagerResponse;
import responses.SeatingResponse;
import responses.LoginResponse;
import responses.ReservationResponse;
import responses.Response;

/**
 * Central application controller for the Bistro Echo Client.
 * All JavaFX screen controllers call THIS class (UI -> Controller)
 * Only THIS class sends requests to server (Controller -> Network)
 * Only THIS class routes responses from server (Network -> Controller)
 *
 * Important: OCSF calls back on a non JavaFX thread
 * This controller must NOT directly touch JavaFX
 */
public class ClientController {

    private final BistroEchoClient client;
    private ClientUIHandler ui;
    private boolean connected;

    // Session identity state
    private boolean guestSession;
    private String guestContact;
    private String currentUsername;
    private String currentEmail;
    private String currentPhone;

    public ClientController(BistroEchoClient client) {
        this.client = client;
    }

    /** Called once during UI startup */
    public void setUIHandler(ClientUIHandler ui) {
        this.ui = ui;
    }
    
    //connected boolean
    public boolean isConnected() {
        return connected;
    }

    // UI - Controller API
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

                        // update session state for member login
                        this.guestSession = false;
                        this.guestContact = null;
                        this.currentUsername = loginResponse.getUsername();
                        this.currentEmail = loginResponse.getEmail();
                        this.currentPhone = loginResponse.getPhone();

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
                        //update local cache if server returns updated values
                        this.currentEmail = loginResponse.getEmail();
                        this.currentPhone = loginResponse.getPhone();
                        safeUiInfo("Subscriber Details", "Details updated successfully");
                    }
                }
            }

            else if (responseData instanceof ReservationResponse reservationResponse) {

                // if this is the final confirmation, show a global info message + confirmation code
                if (reservationResponse.getType() == ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED) {
                    String messege = "Reservation confirmed successfully. "
                               + "Confirmation code: " + reservationResponse.getConfirmationCode();
                    safeUiInfo("Reservation", messege);
                }

                //push to UI handler
                if (ui != null) {
                    ui.onReservationResponse(reservationResponse);
                } else {
                    uiPayload(reservationResponse);
                }
            }
            
            else if (responseData instanceof responses.SeatingResponse seatingResponse) {

                if (seatingResponse.getType() == responses.SeatingResponse.SeatingResponseType.CUSTOMER_CHECKED_IN) {
                    Integer tn = seatingResponse.getTableNumberl();
                    String seatingMessage = "Checked-in successfully."
                            + (tn != null ? (" Table: " + tn) : "");
                    safeUiInfo("Check-in", seatingMessage);
                } else if (seatingResponse.getType() == responses.SeatingResponse.SeatingResponseType.CUSTOMER_IN_WAITINGLIST) {
                    safeUiInfo("Check-in", "No table available. You were added to the waiting list.");
                }

                // push to UI handler
                if (ui != null) {
                    ui.onSeatingResponse(seatingResponse);
                } else {
                    uiPayload(seatingResponse);
                }
            }
            

            // handle other response types here (Reservations, etc)

        } catch (Exception e) {
            safeUiError("Client Error", "Error handling server response:\n" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Login and information verification
    public void requestLogin(String usernameRaw, String passwordRaw) {
        String username = usernameRaw == null ? "" : usernameRaw.trim();
        String password = passwordRaw == null ? "" : passwordRaw.trim();

        String err = validateUsername(username);
        if (err != null) { safeUiWarning("Login", err); return; }

        err = validatePassword(password);
        if (err != null) { safeUiWarning("Login", err); return; }
        LoginRequest loginRequest = new LoginRequest(username,password,UserCommand.LOGIN_REQUEST);       
        Request<LoginRequest> req = new Request<LoginRequest>(Request.Command.USER_REQUEST,loginRequest);
        sendRequest(req);
    }
    
    public void logout() {
        try {
            if (connected && client != null) {
                client.closeConnection();   // OCSF AbstractClient.closeConnection()
            }
        } catch (Exception e) {
            safeUiError("Logout", "Error while closing connection:\n" + e.getMessage());
        } finally {
            connected = false;
            

            // reset session identity
            guestSession = false;
            guestContact = null;
            currentUsername = null;
            currentEmail = null;
            currentPhone = null;

            // Optionally notify UI to go back to login screen
            if (ui != null) {
                ui.showInfo("Logout", "You have been logged out.");
                //ui.routeToLogin(); // <-- add this to your ClientUIHandler if you do not have it yet
            }
        }
    }

    public void requestBillAction(
            BillRequestType type,
            int confirmationCode,
            boolean isCashPayment
    ) {
        if (!connected) {
            safeUiWarning("Billing", "Not connected to server.");
            return;
        }

        BillRequest payload = new BillRequest(type, confirmationCode, isCashPayment);

        Request<BillRequest> req =
                new Request<>(Request.Command.BILLING_REQUEST, payload);

        sendRequest(req);
    }
    
    public void requestSeatingCheckInByConfirmationCode(int confirmationCode) {
        if (!connected) {
            safeUiWarning("Check-in", "Not connected to server.");
            return;
        }

        SeatingRequest payload = new SeatingRequest(
                SeatingRequestType.BY_CONFIRMATIONCODE,
                confirmationCode,
                null
        );

        Request<SeatingRequest> req =
                new Request<>(Request.Command.SEATING_REQUEST, payload);

        sendRequest(req);
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
    
    public void requestNewReservation(
            ReservationRequestType type,
            LocalDate reservationDate,
            LocalTime startTime,
            int partySize,
            String userID,
            String guestContact,
            int confirmationCode
    ) {
        //client side validation
    	
        if (!connected) {
            safeUiWarning("Reservations", "Not connected to server.");
            return;
        }

        ReservationRequest payload = new ReservationRequest(
                type,
                reservationDate,
                startTime,
                partySize,
                userID,
                guestContact,
                confirmationCode
        );

        Request<ReservationRequest> req = new Request<>(Request.Command.RESERVATION_REQUEST, payload);
        sendRequest(req);
    }
    //role mapper
    private DesktopScreenController.Role mapRoleFromServer(String rawRole) {
        if (rawRole == null) {
            return DesktopScreenController.Role.GUEST;
        }

        String normalized = rawRole.trim().toUpperCase();

        // server sends exact enum names
        try {
            return DesktopScreenController.Role.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // fallback - dont crash UI if server sends unexpected role
            System.err.println("[WARN] Unknown role from server: " + rawRole);
            return DesktopScreenController.Role.GUEST;
        }
    }

    public void handleConnectionLost(String message) {
        if (ui != null) {
            ui.showError("Connection Lost", message);
        }
    }
    
    // safe UI calls

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

    // session identity API (guest)
    public void startGuestSession(String contact) {
        this.guestSession = true;
        this.guestContact = contact;
        this.currentUsername = "guest";
    }
    

    public boolean isGuestSession() {
        return guestSession;
    }
    
    //Getters
    public String getGuestContact() {
        return guestContact;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
    
    public String getCurrentEmail() {
        return currentEmail;
    }

    public String getCurrentPhone() {
        return currentPhone;
    }

}
