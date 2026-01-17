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

/**
 * JavaFX controller for the manager "Update Opening Hours" screen.
 * <p>
 * Enables managers to view and update opening hours for a specific date, including:
 * <ul>
 *   <li>Opening time</li>
 *   <li>Closing time</li>
 *   <li>Occasion type (e.g., regular day, holiday)</li>
 * </ul>
 * </p>
 * <p>
 * The screen supports:
 * <ul>
 *   <li>Loading opening hours for the date selected in {@link #datePicker}</li>
 *   <li>Editing open/close times using custom table cells with +/- hour buttons</li>
 *   <li>Editing occasion via a dropdown (combo box) cell</li>
 *   <li>Saving a single row (per-date) or saving all rows at once</li>
 * </ul>
 * </p>
 * <p>
 * Communication with the server is performed through {@link ClientController} using {@link ManagerRequest}.
 * </p>
 */
public class UpdateOpeningHoursScreenController implements ClientControllerAware {

    /**
     * Formatter used to render {@link LocalTime} values inside the time columns.
     */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Supported occasion values for the occasion dropdown column.
     */
    private static final List<String> OCCASIONS = List.of("REGULAR", "HOLIDAY", "WAR", "STRIKE");

    /**
     * Converts occasion values between internal representation (uppercase) and UI representation (capitalized).
     */
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

    /**
     * Backing list for the opening hours table (reactive UI updates).
     */
    private final ObservableList<OpeningHoursRow> rows = FXCollections.observableArrayList();

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Configures:
     * <ul>
     *   <li>Date column binding</li>
     *   <li>Open/close columns with custom +/- time adjustment cells</li>
     *   <li>Occasion column with a combo-box editor</li>
     *   <li>Save button column for per-row saving</li>
     * </ul>
     * </p>
     */
    @FXML
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
            colOccasion.setCellFactory(
                    ComboBoxTableCell.forTableColumn(OCCASION_CONVERTER, FXCollections.observableArrayList(OCCASIONS))
            );
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

    /**
     * Injects the {@link ClientController} used for sending manager requests to the server and
     * indicates whether the client is currently connected.
     *
     * @param controller the application-level client controller used to communicate with the server
     * @param connected  {@code true} if the client is connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * UI handler that loads opening hours for the date selected in {@link #datePicker}.
     * <p>
     * Sends {@link ManagerCommand#VIEW_ALL_OPENING_HOURS} with the selected date as a parameter.
     * </p>
     */
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

    /**
     * UI handler that saves all rows currently displayed in the table.
     * <p>
     * Iterates through the table rows and sends an edit request per row.
     * </p>
     */
    @FXML
    private void onSaveAll() {
        if (!readyForServer()) return;
        for (OpeningHoursRow row : rows) {
            sendEditRequest(row);
        }
    }

    /**
     * Handles {@link ManagerResponse} messages relevant to this screen.
     * <p>
     * Supported response commands:
     * <ul>
     *   <li>{@link ManagerResponseCommand#ALL_OPENING_HOURS_RESPONSE} - populates the table with opening hours rows</li>
     *   <li>{@link ManagerResponseCommand#EDIT_HOURS_RESPONSE} - indicates an edit request completed successfully</li>
     * </ul>
     * </p>
     *
     * @param response server response; may be {@code null}
     */
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

    /**
     * Validates and sends an edit request for a single opening-hours row.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Open and close times must not be {@code null}</li>
     *   <li>Open time must be strictly before close time</li>
     *   <li>If occasion is missing/blank, it defaults to "REGULAR"</li>
     * </ul>
     * </p>
     *
     * @param row the row to validate and save
     */
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

    /**
     * Checks whether the controller is ready to send requests to the server.
     *
     * @return {@code true} if connected and a {@link ClientController} is available; {@code false} otherwise
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Updates the info label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Builds a list of times from {@code start} to {@code end} (inclusive) with 1-hour steps.
     * <p>
     * Note: this helper is not used by the current UI, but kept as a utility.
     * </p>
     *
     * @param start start time (inclusive)
     * @param end   end time (inclusive)
     * @return list of hourly times between {@code start} and {@code end}
     */
    private static List<LocalTime> buildTimes(LocalTime start, LocalTime end) {
        List<LocalTime> times = new java.util.ArrayList<>();
        for (LocalTime t = start; !t.isAfter(end); t = t.plusHours(1)) {
            times.add(t);
        }
        return times;
    }

    /**
     * View-model row for the opening hours table.
     * <p>
     * Holds editable UI fields (open time, close time, occasion) for a specific {@link LocalDate}.
     * </p>
     */
    public static class OpeningHoursRow {
        private LocalDate date;
        private LocalTime open;
        private LocalTime close;
        private String occasion;

        /**
         * Creates a row representing opening hours for a single date.
         *
         * @param date     date for which the opening hours apply
         * @param open     opening time
         * @param close    closing time
         * @param occasion occasion type (e.g., REGULAR/HOLIDAY)
         */
        public OpeningHoursRow(LocalDate date, LocalTime open, LocalTime close, String occasion) {
            this.date = date;
            this.open = open;
            this.close = close;
            this.occasion = occasion;
        }

        /**
         * @return the date for this opening-hours row
         */
        public LocalDate getDate() {
            return date;
        }

        /**
         * @return opening time
         */
        public LocalTime getOpen() {
            return open;
        }

        /**
         * Updates the opening time value used by the UI.
         *
         * @param open new opening time
         */
        public void setOpen(LocalTime open) {
            this.open = open;
        }

        /**
         * @return closing time
         */
        public LocalTime getClose() {
            return close;
        }

        /**
         * Updates the closing time value used by the UI.
         *
         * @param close new closing time
         */
        public void setClose(LocalTime close) {
            this.close = close;
        }

        /**
         * @return occasion type (usually one of {@link #OCCASIONS})
         */
        public String getOccasion() {
            return occasion;
        }

        /**
         * Updates the occasion type value used by the UI.
         *
         * @param occasion new occasion type
         */
        public void setOccasion(String occasion) {
            this.occasion = occasion;
        }
    }

    /**
     * Creates a custom {@link TableCell} that displays a time value with "-" and "+" buttons.
     * <p>
     * Clicking "-" subtracts one hour; clicking "+" adds one hour.
     * The updated time is written back to the {@link OpeningHoursRow} model.
     * </p>
     *
     * @param isOpen {@code true} to control the "open" field; {@code false} to control the "close" field
     * @return a {@link TableCell} implementation for adjusting time values
     */
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

            /**
             * Adjusts the current time by the given amount of hours and updates the associated row model.
             *
             * @param deltaHours number of hours to add (positive) or subtract (negative)
             */
            private void adjustTime(int deltaHours) {
                OpeningHoursRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row == null) return;
                LocalTime current = isOpen ? row.getOpen() : row.getClose();
                if (current == null) current = LocalTime.of(0, 0);

                LocalTime updated = current.plusHours(deltaHours);
                if (isOpen) row.setOpen(updated);
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
