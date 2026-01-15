package controllers;

import desktop_screen.DesktopScreenController;
import responses.ManagerResponse;
import responses.ReportResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;
import responses.UserHistoryResponse;
import responses.WaitingListResponse;
/**
 * No-op UI handler used when no UI is attached.
 *
 * Provides safe console logging and avoids {@code NullPointerException} when
 * UI callbacks are invoked before a real handler is registered.

 */
// default implementation that safely handles callbacks before UI initializes
// prevents crashes by logging to console instead of throwing NullPointerException
public class NullClientUIHandler implements ClientUIHandler {
    // log info messages to console
    @Override
    public void showInfo(String title, String message) {
        System.out.println("[INFO] " + title + ": " + message);
    }

    // log warnings to console
    @Override
    public void showWarning(String title, String message) {
        System.out.println("[WARN] " + title + ": " + message);
    }

    // log errors to stderr
    @Override
    public void showError(String title, String message) {
        System.err.println("[ERROR] " + title + ": " + message);
    }

    // log generic payload to console
    @Override
    public void showPayload(Object payload) {
        System.out.println("[PAYLOAD] " + payload);
    }

    // log routing request to console
    @Override
    public void routeToDesktop(DesktopScreenController.Role role, String username) {
        System.out.println("[ROUTE] role=" + role + " username=" + username);
    }

    // all operation callbacks are no-ops in this handler
    // real implementations will process these responses
    @Override
    public void onReservationResponse(ReservationResponse response) {}

    @Override
    public void onSeatingResponse(SeatingResponse response) {}

    @Override
    public void onUserHistoryResponse(java.util.List<UserHistoryResponse> rows) {}

    @Override
    public void onUserHistoryError(String message) {}

    @Override
    public void onReportResponse(ReportResponse reportResponse) {}

    @Override
    public void onUpcomingReservationsResponse(java.util.List<ReservationResponse> rows) {}

    @Override
    public void onUpcomingReservationsError(String message) {}

    @Override
    public void onManagerResponse(ManagerResponse response) {}

    @Override
    public void onBillTotal(double baseTotal, boolean isCash) {}

    @Override
    public void onBillPaid(Integer tableNumber) {}

    @Override
    public void onBillError(String message) {}

    @Override
    public void onUserDetailsResponse(String email, String phone) {}
    
    @Override
    public void onWaitingListCancellation(WaitingListResponse response) {}
}