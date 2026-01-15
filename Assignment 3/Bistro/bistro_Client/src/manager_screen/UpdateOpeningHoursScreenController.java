package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import responses.CurrentOpeningHoursResponse;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

// manager view for updating restaurant opening hours
// allows editing opening/closing times and occasion type for each date
// provides custom time adjustment UI with +/- buttons
public class UpdateOpeningHoursScreenController implements ClientControllerAware {
	

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
   
    // available occasion types for special days
    private static final List<String> OCCASIONS = List.of("REGULAR", "HOLIDAY", "WAR", "STRIKE");
    // converter for displaying occasion dropdown
    private static final StringConverter<String> OCCASION_CONVERTER = new StringConverter<>() {
        @Override
        public String toString(String value) {
            if (value == null || value.isBlank()) return "";
            String lower = value.toLowerCase();
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }

        @Override
        public String fromString(String value) {
            if (value == null) return null;
            return value.trim().toUpperCase();
        }
    };
   

    @FXML private DatePicker datePicker;
    @FXML private TableView<OpeningHoursRow> hoursTable;
    @FXML private TableColumn<OpeningHoursRow, LocalDate> colDate;
    @FXML private TableColumn<OpeningHoursRow, LocalTime> colOpen;
    @FXML private TableColumn<OpeningHoursRow, LocalTime> colClose;
    @FXML private TableColumn<OpeningHoursRow, String> colOccasion;
    @FXML private TableColumn<OpeningHoursRow, Void> colSave;
    @FXML private Label infoLabel;

    private ClientController clientController;
    private boolean connected;
    // observable list for reactive UI updates
    private final ObservableList<OpeningHoursRow> rows = FXCollections.observableArrayList();

    @FXML
    // sets up table columns with custom cell factories
    // open/close columns have +/- buttons for time adjustment
    private void initialize() {
        if (colDate != null) colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
                    
        // opening time column with custom time adjustment UI
        if (colOpen != null) {
        	colOpen.setEditable(true);
            colOpen.setCellValueFactory(new PropertyValueFactory<>("open"));
            colOpen.setCellFactory(column -> createTimeAdjustCell(true));
        }
        // closing time column with custom time adjustment UI
        if (colClose != null) {
        	colClose.setEditable(true);
            colClose.setCellValueFactory(new PropertyValueFactory<>("close"));
            colClose.setCellFactory(column -> createTimeAdjustCell(false));
        }
        // occasion column with dropdown
        if (colOccasion != null) {
            colOccasion.setCellValueFactory(new PropertyValueFactory<>("occasion"));
            colOccasion.setCellFactory(ComboBoxTableCell.forTableColumn(OCCASION_CONVERTER, FXCollections.observableArrayList(OCCASIONS)));
            colOccasion.setOnEditCommit(event -> {
                OpeningHoursRow row = event.getRowValue();
                if (row != null) {
                    row.setOccasion(event.getNewValue());
                }
            });
        }
        // save button column
        if (colSave != null) {
            colSave.setCellFactory(column -> new TableCell<>() {
                private final Button button = new Button("Save");

                {
                    button.getStyleClass().add("primary");
                    button.setOnAction(event -> {
                        OpeningHoursRow row = getTableView().getItems().get(getIndex());
                        if (row != null) {
                            sendEditRequest(row);
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : button);
                }
            });
        }
        if (hoursTable != null) {
            hoursTable.setEditable(true);
            hoursTable.setItems(rows);
        }
        setInfo("Pick a date and load opening hours.");
    }

    @Override
    // dependency injection from DesktopScreenController
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    // loads opening hours for selected date
    private void onLoad() {
        if (!readyForServer()) return;
        LocalDate date = datePicker == null ? null : datePicker.getValue();
        if (date == null) {
            setInfo("Please select a date.");
            return;
        }
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_ALL_OPENING_HOURS, date));
        setInfo("Loading opening hours...");
    }

    @FXML
    // saves all rows at once
    private void onSaveAll() {
        if (!readyForServer()) return;
        for (OpeningHoursRow row : rows) {
            sendEditRequest(row);
        }
    }

    // populates table with opening hours data from server
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        if (response.getResponseCommand() == ManagerResponseCommand.ALL_OPENING_HOURS_RESPONSE) {
            rows.clear();
            if (response.getTables() != null) {
                for (Object item : response.getTables()) {
                    if (item instanceof CurrentOpeningHoursResponse opening) {
                        rows.add(new OpeningHoursRow(
                                opening.getDate(),
                                opening.getOpen(),
                                opening.getClose(),
                                opening.getOccasion()
                        ));
                    }
                }
            }
            setInfo("Opening hours updated.");
        } else if (response.getResponseCommand() == ManagerResponseCommand.EDIT_HOURS_RESPONSE) {
            setInfo("Opening hours saved.");
        }
    }

    // validates and sends edit request for a single row
    private void sendEditRequest(OpeningHoursRow row) {
        if (!readyForServer()) return;
        if (row.getOpen() == null || row.getClose() == null) {
            setInfo("Open/close time is required.");
            return;
        }
        if (!row.getOpen().isBefore(row.getClose())) {
            setInfo("Open time must be before close time.");
            return;
        }
        String occasion = row.getOccasion();
        if (occasion == null || occasion.isBlank()) {
            occasion = "REGULAR";
        }
        clientController.requestManagerAction(
                new ManagerRequest(
                        ManagerCommand.EDIT_OPENING_HOURS,
                        row.getDate(),
                        row.getOpen(),
                        row.getClose(),
                        occasion
                )
        );
        setInfo("Saving opening hours for " + row.getDate() + "...");
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
    
    // unused helper method for generating time lists
    private static List<LocalTime> buildTimes(LocalTime start, LocalTime end) {
        List<LocalTime> times = new java.util.ArrayList<>();
        for (LocalTime t = start; !t.isAfter(end); t = t.plusHours(1)) {
            times.add(t);
        }
        return times;
    }

    // data model for opening hours table rows
    public static class OpeningHoursRow {
        private LocalDate date;
        private LocalTime open;
        private LocalTime close;
        private String occasion;

        public OpeningHoursRow(LocalDate date, LocalTime open, LocalTime close, String occasion) {
            this.date = date;
            this.open = open;
            this.close = close;
            this.occasion = occasion;
        }

        // getters and setters for JavaFX property binding
        public LocalDate getDate() {
            return date;
        }

        public LocalTime getOpen() {
            return open;
        }

        public void setOpen(LocalTime open) {
            this.open = open;
        }

        public LocalTime getClose() {
            return close;
        }

        public void setClose(LocalTime close) {
            this.close = close;
        }

        public String getOccasion() {
            return occasion;
        }

        public void setOccasion(String occasion) {
            this.occasion = occasion;
        }
    }

    // creates custom table cell with +/- buttons for time adjustment
    // isOpen determines whether this controls opening or closing time
    private TableCell<OpeningHoursRow, LocalTime> createTimeAdjustCell(boolean isOpen) {
        return new TableCell<>() {
            private final Label timeLabel = new Label();
            private final Button minus = new Button("-");
            private final Button plus = new Button("+");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8, minus, timeLabel, plus);

            {
                minus.getStyleClass().add("ghost");
                plus.getStyleClass().add("ghost");
                // wire up buttons to adjust time by 1 hour
                minus.setOnAction(event -> adjustTime(-1));
                plus.setOnAction(event -> adjustTime(1));
            }

            // adjusts time by deltaHours and updates row
            private void adjustTime(int deltaHours) {
                OpeningHoursRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row == null) return;
                LocalTime current = isOpen ? row.getOpen() : row.getClose();
                if (current == null) current = LocalTime.of(0, 0);
                                                  
                LocalTime updated = current.plusHours(deltaHours);
                if (isOpen)  row.setOpen(updated);                  
                else row.setClose(updated);
                    
                // refresh display
                updateItem(updated, false);
            }

            @Override
            protected void updateItem(LocalTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);                    
                 else {
                    timeLabel.setText(item == null ? "" : TIME_FMT.format(item));
                    setGraphic(box);
                }
            }
        };
    }
}