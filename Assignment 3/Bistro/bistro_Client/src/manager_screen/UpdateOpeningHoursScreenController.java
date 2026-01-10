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
import java.time.format.DateTimeParseException;
import java.util.List;

public class UpdateOpeningHoursScreenController implements ClientControllerAware {
	

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> OCCASIONS = List.of("Regular", "Holiday", "Private Event", "Maintenance");
    private static final List<LocalTime> OPEN_TIMES = buildTimes(LocalTime.of(9, 0), LocalTime.of(15, 0));
    private static final List<LocalTime> CLOSE_TIMES = buildTimes(LocalTime.MIDNIGHT, LocalTime.of(23, 0));


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
    private final ObservableList<OpeningHoursRow> rows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (colDate != null) {
            colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        }
        if (colOpen != null) {
        	colOpen.setEditable(true);
            colOpen.setCellValueFactory(new PropertyValueFactory<>("open"));
            colOpen.setCellFactory(ComboBoxTableCell.forTableColumn(
                    new LocalTimeStringConverter(),
                    FXCollections.observableArrayList(OPEN_TIMES)
            ));
            colOpen.setOnEditCommit(event -> {
                OpeningHoursRow row = event.getRowValue();
                if (row == null) return;
                if (event.getNewValue() == null) {
                	 setInfo("Please select an open time.");
                    row.setOpen(event.getOldValue());
                    hoursTable.refresh();
                    return;
                }
                row.setOpen(event.getNewValue());
            });
        }
        if (colClose != null) {
        	colClose.setEditable(true);
            colClose.setCellValueFactory(new PropertyValueFactory<>("close"));
            colClose.setCellFactory(ComboBoxTableCell.forTableColumn(
                    new LocalTimeStringConverter(),
                    FXCollections.observableArrayList(CLOSE_TIMES)
            ));
            colClose.setOnEditCommit(event -> {
                OpeningHoursRow row = event.getRowValue();
                if (row == null) return;
                if (event.getNewValue() == null) {
                	 setInfo("Please select a close time.");
                    row.setClose(event.getOldValue());
                    hoursTable.refresh();
                    return;
                }
                row.setClose(event.getNewValue());
            });
        }
        if (colOccasion != null) {
            colOccasion.setCellValueFactory(new PropertyValueFactory<>("occasion"));
            colOccasion.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(OCCASIONS)));
            colOccasion.setOnEditCommit(event -> {
                OpeningHoursRow row = event.getRowValue();
                if (row != null) {
                    row.setOccasion(event.getNewValue());
                }
            });
        }
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
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
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
    private void onSaveAll() {
        if (!readyForServer()) return;
        for (OpeningHoursRow row : rows) {
            sendEditRequest(row);
        }
    }

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
        clientController.requestManagerAction(
                new ManagerRequest(
                        ManagerCommand.EDIT_OPENING_HOURS,
                        row.getDate(),
                        row.getOpen(),
                        row.getClose(),
                        row.getOccasion()
                )
        );
        setInfo("Saving opening hours for " + row.getDate() + "...");
    }

    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
    
    private static List<LocalTime> buildTimes(LocalTime start, LocalTime end) {
        List<LocalTime> times = new java.util.ArrayList<>();
        for (LocalTime t = start; !t.isAfter(end); t = t.plusHours(1)) {
            times.add(t);
        }
        return times;
    }

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

    private static class LocalTimeStringConverter extends StringConverter<LocalTime> {
        @Override
        public String toString(LocalTime time) {
            return time == null ? "" : TIME_FMT.format(time);
        }

        @Override
        public LocalTime fromString(String value) {
            if (value == null || value.isBlank()) return null;
            try {
                return LocalTime.parse(value.trim(), TIME_FMT);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }
}