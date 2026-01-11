package subscriber_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import controllers.ClientUIHandler;
import desktop_screen.DesktopScreenController;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import responses.ManagerResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;
import responses.UserHistoryResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SubscriberMainScreenController implements ClientControllerAware, ClientUIHandler {

    @FXML private TableView<UserHistoryResponse> reservationsTable;
    @FXML private TableColumn<UserHistoryResponse, String> colDate;
    @FXML private TableColumn<UserHistoryResponse, String> colStartTime;
    @FXML private TableColumn<UserHistoryResponse, String> colPartySize;
    @FXML private TableColumn<UserHistoryResponse, String> colConfirmationCode;
    @FXML private Label infoLabel;

    private final ObservableList<UserHistoryResponse> reservations = FXCollections.observableArrayList();

    private ClientController clientController;
    private boolean connected;
    private Runnable onLogout;
    private boolean requested;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        if (colDate != null) {
            colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatDate(cd.getValue() == null ? null : cd.getValue().getReservationDate())
            ));
        }
        if (colStartTime != null) {
            colStartTime.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getCheckInTime())
            ));
        }
        if (colPartySize != null) {
            colPartySize.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getPartySize())
            ));
        }
        if (colConfirmationCode != null) {
            colConfirmationCode.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getConfirmationCode())
            ));
        }
        if (reservationsTable != null) {
            reservationsTable.setItems(reservations);
        }
        setInfo("Loading upcoming reservations...");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        if (connected && !requested) {
            requested = true;
            requestUpcomingReservations();
        }
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    @FXML
    private void onRefresh() {
        requestUpcomingReservations();
    }

    @FXML
    private void onLogout() {
        if (clientController != null) {
            clientController.logout();
        }
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void requestUpcomingReservations() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return;
        }
        setInfo("Loading upcoming reservations...");
        clientController.requestUpcomingReservations();
    }

    @Override
    public void showInfo(String title, String message) {
        setInfo(message);
    }

    @Override
    public void showWarning(String title, String message) {
        setInfo(message);
    }

    @Override
    public void showError(String title, String message) {
        setInfo(message);
    }

    @Override
    public void showPayload(Object payload) {
        setInfo(String.valueOf(payload));
    }

    @Override
    public void routeToDesktop(DesktopScreenController.Role role, String username) {
        // already in subscriber screen
    }

    @Override
    public void onReservationResponse(ReservationResponse response) {
        // not used here
    }

    @Override
    public void onSeatingResponse(SeatingResponse response) {
        // not used here
    }

    @Override
    public void onUserHistoryResponse(List<responses.UserHistoryResponse> rows) {
        // not used here
    }

    @Override
    public void onUserHistoryError(String message) {
        // not used here
    }

    @Override
    public void onUpcomingReservationsResponse(List<UserHistoryResponse> rows) {
        reservations.clear();
        if (rows != null) {
            reservations.addAll(rows);
        }
        setInfo(reservations.isEmpty() ? "No upcoming reservations." : "Upcoming reservations loaded.");
    }

    @Override
    public void onUpcomingReservationsError(String message) {
        reservations.clear();
        setInfo(message == null || message.isBlank() ? "Failed to load upcoming reservations." : message);
    }

    @Override
    public void onManagerResponse(ManagerResponse response) {
        // not used here
    }

    @Override
    public void onBillTotal(double baseTotal, boolean isCash) {
        // not used here
    }

    @Override
    public void onBillPaid(Integer tableNumber) {
        // not used here
    }

    @Override
    public void onBillError(String message) {
        // not used here
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FMT);
    }

    private String formatTime(LocalTime time) {
        return time == null ? "-" : time.format(TIME_FMT);
    }

    private String formatInt(int value) {
        return value <= 0 ? "-" : String.valueOf(value);
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}