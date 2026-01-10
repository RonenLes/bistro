package controllers;

import desktop_screen.DesktopScreenController;
import responses.ReservationResponse;
import responses.SeatingResponse;

import responses.UserHistoryResponse;

import responses.ManagerResponse;


//Interface for desktopUI or terminalUI to implement
public interface ClientUIHandler {
    void showInfo(String title, String message);
    void showWarning(String title, String message);
    void showError(String title, String message);
    void showPayload(Object payload);
    void routeToDesktop(DesktopScreenController.Role role, String username);
    void onReservationResponse(ReservationResponse response);
    void onSeatingResponse(SeatingResponse response);
    void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows);
    void onUserHistoryError(String message);
    void onManagerResponse(ManagerResponse response);


}
