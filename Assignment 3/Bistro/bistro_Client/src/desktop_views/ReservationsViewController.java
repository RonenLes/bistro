package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import requests.ReservationRequest;
import requests.ReservationRequest.ReservationRequestType;
import responses.ReservationResponse;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.DateCell;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReservationsViewController implements ClientControllerAware {

    // Step 1
    @FXML private Spinner<Integer> partySizeSpinner;
    @FXML private DatePicker datePicker;
    @FXML private Label infoLabel;

    // Step 2
    @FXML private VBox step1Pane;
    @FXML private VBox step2Pane;
    @FXML private TilePane slotsTile;
    @FXML private Label slotHeaderLabel;
    @FXML private Label slotInfoLabel;
    @FXML private VBox slotsContainer;

    private ClientController clientController;
    private boolean connected;

    private LocalDate currentDate;
    private int currentPartySize;
    private String userID;
    

    private static final int MIN_PARTY = 1;
    private static final int MAX_PARTY = 20;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    
    private static final double SLOT_WIDTH = 130.0;
    private static final double SLOT_HEIGHT = 55.0;

    @FXML
    private void initialize() {
        initPartySizeSpinner();
        initDatePickerLimit();
        switchToStep1();
        setInfo("Pick party size + date, then choose a time block.");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }
    
    private void initDatePickerLimit() {
        if (datePicker == null) return;

        LocalDate today = LocalDate.now();
        LocalDate max = today.plusDays(30);

        datePicker.setValue(today); // optional: default to today

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;

                boolean disabled = item.isBefore(today) || item.isAfter(max);
                setDisable(disabled);

                // optional: visually "gray out" disabled dates (nice UX)
                if (disabled) {
                    setStyle("-fx-opacity: 0.45;");}
                else setStyle("");
                
            }
        });

        //guard manual typing / programmatic set:
        datePicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if (newV.isBefore(today)) datePicker.setValue(today);
            else if (newV.isAfter(max)) datePicker.setValue(max);
        });
    }
    
    private void initPartySizeSpinner() {
        if (partySizeSpinner == null) return;

        // value factory IS required for a Spinner to behave correctly
        partySizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_PARTY, MAX_PARTY, 2)
        );
        partySizeSpinner.setEditable(true);

        // allow only digits in the editor
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("\\d{0,2}") ? change : null;
        });

        partySizeSpinner.getEditor().setTextFormatter(formatter);
        partySizeSpinner.getEditor().setOnAction(e -> clampPartySize());
        partySizeSpinner.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) clampPartySize();
        });
    }
    

    private void clampPartySize() {
        if (partySizeSpinner == null || partySizeSpinner.getValueFactory() == null) return;

        String txt = partySizeSpinner.getEditor() == null ? "" : partySizeSpinner.getEditor().getText();
        int v;
        try { v = Integer.parseInt(txt); }
        catch (Exception e) { v = MIN_PARTY; }

        if (v < MIN_PARTY) v = MIN_PARTY;
        if (v > MAX_PARTY) v = MAX_PARTY;

        partySizeSpinner.getValueFactory().setValue(v);
    }

    @FXML
    private void onFindTimes() {
        clampPartySize();

        Integer partySize = partySizeSpinner == null ? null : partySizeSpinner.getValue();
        LocalDate date = datePicker == null ? null : datePicker.getValue();

        if (partySize == null || partySize < MIN_PARTY) { setInfo("Party size must be at least 1."); return; }
        if (partySize > MAX_PARTY) { setInfo("Party size cannot exceed 20."); return; }
        if (date == null) { setInfo("Please choose a date."); return; }
        if (date.isBefore(LocalDate.now())) { setInfo("Date cannot be in the past."); return; }

        currentPartySize = partySize;
        currentDate = date;

        if (clientController == null) { setInfo("ClientController not set."); return; }

        setInfo("Fetching available times from server...");

        String userId = null;
        String guestContact = null;

        // FIRST PHASE: ask server for available times for date + partySize
        clientController.requestNewReservation(
                ReservationRequestType.FIRST_PHASE,
                date,
                null, // IMPORTANT: server should ignore or allow null here for phase 1
                partySize,
                userId,
                guestContact,
                0
        );
    }

    @FXML
    private void onBackToForm() {
        switchToStep1();
        setInfo("Pick party size + date, then choose a time block.");
    }

    // on chosen time, fetch the open slots
    private void onTimeChosen(LocalDate date, String time, int partySize) {
        setInfo("Selected: " + date + " at " + time + " for " + partySize + " people.");

        if (clientController == null) {
            setInfo("ClientController not set.");
            return;
        }

        // parse the HH:mm string back to LocalTime
        LocalTime chosenTime = LocalTime.parse(time, TIME_FMT);

        // For now, assume:
        // - logged-in subscribers: server can identify them from session / userID
        // - guests: we send null userID, and maybe guestContact later from a text field
        String userId = null;        // TODO: inject real user id from login
        String guestContact = null;  // TODO: bind to a text field for non-subscribers

        // FIRST PHASE
        clientController.requestNewReservation(
                ReservationRequest.ReservationRequestType.FIRST_PHASE,
                date,
                chosenTime,
                partySize,
                userId,
                guestContact,
                0 // confirmationCode is 0 or ignored for FIRST_PHASE
        );

    }
    /**
     * Called by DesktopScreenController
     * when the server replies to FIRST_PHASE
     */
    public void handleServerResponse(ReservationResponse resp) {
        if (resp == null) return;

        // clear the container first
        slotsContainer.getChildren().clear();
        
        // TODO REMOVE DEBUG
        //System.out.println(resp.getUserID()+ " " + resp.getTableNumber() + " " + resp.getNewDate());
        
        switch (resp.getType()) {
            case FIRST_PHASE_SHOW_AVAILABILITY -> {
                setInfo("Select a time for " + currentDate);
                
                // main times
                addSectionHeader("Available times for " + currentDate.format(DateTimeFormatter.ofPattern("dd/MM")));
                buildTimeSlots(currentDate, resp.getAvailableTimes());

                // 
                if (resp.getSuggestedDates() != null && !resp.getSuggestedDates().isEmpty()) {
                    addSectionHeader("Or choose a different date:");
                    buildSuggestions(resp.getSuggestedDates());
                }
                switchToStep2();
            }

            case FIRST_PHASE_SHOW_SUGGESTIONS -> {
                setInfo("No exact matches found.");
                addSectionHeader("Available dates in the coming week:");
                buildSuggestions(resp.getSuggestedDates());
                switchToStep2();
            }

            case SECOND_PHASE_CONFIRMED -> {
                // Show local info in the reservations screen
                setInfo("Reservation confirmed. Code: " + resp.getConfirmationCode());

                // clear fields
                if (slotsContainer != null) {
                    slotsContainer.getChildren().clear();
                }
                
                //back to step 1 pane
                switchToStep1();
            }
            
            case FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS -> {
                setInfo("Fully booked for the next 7 days");
                switchToStep1();
            }
        }
    }


    // ----- helper functions to build the time slots -----
    private void addSectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("h3");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
        label.setMaxWidth(Double.MAX_VALUE); // Ensures it doesn't get squashed
        slotsContainer.getChildren().add(label);
    }

    private void buildTimeSlots(LocalDate date, List<LocalTime> times) {
        TilePane grid = createNewGrid();
        for (LocalTime t : times) {
            grid.getChildren().add(createSmallSlotButton(date, t, "ghost"));
        }
        slotsContainer.getChildren().add(grid);
    }

    private void buildSuggestions(Map<LocalDate, List<LocalTime>> suggestions) {
        TilePane grid = createNewGrid();
        suggestions.keySet().stream().sorted().forEach(date -> {
            for (LocalTime t : suggestions.get(date)) {
                grid.getChildren().add(createSmallSlotButton(date, t, "primary"));
            }
        });
        slotsContainer.getChildren().add(grid);
    }

    private TilePane createNewGrid() {
        TilePane tp = new TilePane();
        tp.setHgap(10);
        tp.setVgap(10);
        tp.setPrefColumns(6); 
        tp.setTileAlignment(Pos.CENTER_LEFT);
        return tp;
    }

    private Button createSmallSlotButton(LocalDate date, LocalTime time, String style) {
        String text = time.format(TIME_FMT) + "\n" + date.format(DateTimeFormatter.ofPattern("dd/MM"));
        Button btn = new Button(text);
        
        btn.getStyleClass().addAll(style, "slot-btn");
        btn.setPrefSize(SLOT_WIDTH, SLOT_HEIGHT);
        btn.setStyle("-fx-text-alignment: center; -fx-font-size: 11px;");
        
        btn.setOnAction(e -> sendSecondPhaseRequest(date, time));
        return btn;
    }
    
    /**
     * SECOND PHASE: User selected a specific Slot
     */
    private void sendSecondPhaseRequest(LocalDate date, LocalTime time) {
        setInfo("Confirming reservation for " + date + " at " + time + "...");
        
        //reuse the requestNewReservation method in ClientController
        // but change Type to SECOND_PHASE
        clientController.requestNewReservation(
                ReservationRequestType.SECOND_PHASE,
                date,
                time,
                currentPartySize,
                null, // userID (get from session if needed)
                null, // guestContact (get from a text field)
                0     // confirmation code not generated yet
        );
    }

    // step switch for panes
    private void switchToStep1() {
        if (step1Pane != null) { step1Pane.setVisible(true); step1Pane.setManaged(true); }
        if (step2Pane != null) { step2Pane.setVisible(false); step2Pane.setManaged(false); }
    }

    private void switchToStep2() {
        if (step1Pane != null) { step1Pane.setVisible(false); step1Pane.setManaged(false); }
        if (step2Pane != null) { step2Pane.setVisible(true); step2Pane.setManaged(true); }
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
