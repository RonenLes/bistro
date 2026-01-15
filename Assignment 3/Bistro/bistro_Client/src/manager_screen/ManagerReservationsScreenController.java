package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.ReservationResponse;

import java.time.LocalDate;
import java.time.LocalTime;

// manager view for viewing reservations by date
// displays reservation list and provides button to create new reservations
public class ManagerReservationsScreenController implements ClientControllerAware {

    @FXML private DatePicker datePicker;
    @FXML private TableView<ReservationResponse> reservationsTable;
    @FXML private TableColumn<ReservationResponse, String> colContact;
    @FXML private TableColumn<ReservationResponse, LocalTime> colTime;
    @FXML private TableColumn<ReservationResponse, Integer> colParty;
    @FXML private TableColumn<ReservationResponse, Integer> colCode;
    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;

    // observable list for reactive UI updates
    private final ObservableList<ReservationResponse> reservations = FXCollections.observableArrayList();
    // callback to trigger reservation flow (set by DesktopScreenController)
    private Runnable onStartReservationFlow;

    @FXML
    // sets up table columns to display reservation data
    private void initialize() {
        if (colContact != null) colContact.setCellValueFactory(new PropertyValueFactory<>("newGuestContact"));
                    
        if (colTime != null) colTime.setCellValueFactory(new PropertyValueFactory<>("newTime"));
                 
        if (colParty != null) colParty.setCellValueFactory(new PropertyValueFactory<>("newPartySize"));
                    
        if (colCode !=null) colCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));
        
        if (reservationsTable != null) {
            reservationsTable.setItems(reservations);
        }
        setInfo("Select a date to load reservations.");
    }

    @Override
    // dependency injection from DesktopScreenController
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }
    
    @FXML
    // triggers reservation creation flow via callback
    private void onOpenReservationFlow() {
        if (onStartReservationFlow != null) {
            onStartReservationFlow.run();
        }
    }


    @FXML
    // loads reservations for selected date
    private void onLoad() {
        if (!readyForServer()) return;
        LocalDate date = datePicker == null ? null : datePicker.getValue();
        if (date == null) {
            setInfo("Please select a date.");
            return;
        }
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_RESERVATIONS, date));
        setInfo("Loading reservations...");
    }

    // populates table with reservation data from server
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;
        if (response.getResponseCommand() != ManagerResponseCommand.RESERVATION_BY_DATE_RESPONSE) return;

        reservations.clear();
        if (response.getTables() != null) {
            for (Object item : response.getTables()) {
                if (item instanceof ReservationResponse reservation) {
                    reservations.add(reservation);
                }
            }
        }        
    }
    
    // sets callback for "Make Reservation" button
    public void setOnStartReservationFlow(Runnable onStartReservationFlow) {
        this.onStartReservationFlow = onStartReservationFlow;
    }

    // checks connection before sending requests
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    // updates info label
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}