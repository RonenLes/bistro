package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
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

    private ClientController clientController;
    private boolean connected;

    private LocalDate currentDate;
    private int currentPartySize;
    

    private static final int MIN_PARTY = 1;
    private static final int MAX_PARTY = 20;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

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

        buildTimeSlots(date, partySize);
        switchToStep2();
    }

    private void buildTimeSlots(LocalDate date, int partySize) {
        if (slotHeaderLabel != null) {
            slotHeaderLabel.setText("Date: " + date + " • Party size: " + partySize);
        }
        if (slotInfoLabel != null) slotInfoLabel.setText("Pick one time block.");

        if (slotsTile != null) slotsTile.getChildren().clear();

        // demo: 18:00..22:00, 30 min steps. for big parties -> 60 min
        int stepMinutes = (partySize >= 8) ? 60 : 30;
        LocalTime start = LocalTime.of(18, 0);
        LocalTime end = LocalTime.of(22, 0);

        for (LocalTime t = start; t.isBefore(end.plusMinutes(1)); t = t.plusMinutes(stepMinutes)) {
            String timeText = t.format(TIME_FMT);
            slotsTile.getChildren().add(makeTimeBlockButton(timeText));
        }
    }
    

    private Button makeTimeBlockButton(String timeText) {
        Button b = new Button(timeText);
        b.getStyleClass().addAll("ghost", "slot-btn");
        b.setPrefWidth(200);
        b.setPrefHeight(70);
        b.setMaxWidth(Double.MAX_VALUE);

        b.setOnAction(e -> onTimeChosen(currentDate, timeText, currentPartySize));
        return b;
    }

    @FXML
    private void onBackToForm() {
        switchToStep1();
        setInfo("Pick party size + date, then choose a time block.");
    }

    private void onTimeChosen(LocalDate date, String time, int partySize) {
        setInfo("Selected: " + date + " at " + time + " for " + partySize + " people.");

        if (clientController == null) {
            setInfo("ClientController not set.");
            return;
        }

        // Send to server
        // TODO

        // Optionally disable UI / show "Sending..."
        setInfo("Sending reservation request...");
    }

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
