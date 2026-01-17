package manager_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import requests.TableInfo;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/**
 * JavaFX controller for the manager "Edit Tables" screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Displays the restaurant tables list in a {@link TableView}.</li>
 *   <li>Allows inline editing of table capacity (seats) and sending updates to the server.</li>
 *   <li>Allows adding a new table by table number and capacity.</li>
 *   <li>Allows disabling (deleting) a table; server handles any related reservation cancellations.</li>
 *   <li>Receives server responses via {@link #handleManagerResponse(ManagerResponse)} and updates the UI.</li>
 * </ul>
 */
public class EditTableScreenController implements ClientControllerAware {

    /** Info label used for status messages and validation feedback. */
    @FXML private Label infoLabel;
    /** Input field for the new table number (add table flow). */
    @FXML private TextField newTableNumberField;
    /** Input field for the new table capacity (add table flow). */
    @FXML private TextField newCapacityField;

    /** Table view that displays existing tables. */
    @FXML private TableView<TableRow> tablesTable;
    /** Column showing table number. */
    @FXML private TableColumn<TableRow, Integer> colTableNo;
    /** Column showing table capacity (seats); editable inline. */
    @FXML private TableColumn<TableRow, Integer> colSeats;
    /** Column showing table status (e.g., Active). */
    @FXML private TableColumn<TableRow, String> colStatus;
    /** Column showing table notes (currently unused placeholder). */
    @FXML private TableColumn<TableRow, String> colNotes;
    /** Column providing a per-row "Save" action button. */
    @FXML private TableColumn<TableRow, Void> colEdit;
    /** Column providing a per-row "Disable" action button. */
    @FXML private TableColumn<TableRow, Void> colDisable;

    /** Reference to the shared client controller for server communication (injected by parent controller). */
    private ClientController clientController;
    /** Whether the client is currently connected to the server (injected by parent controller). */
    private boolean connected;

    /** Observable list backing {@link #tablesTable}. */
    private final ObservableList<TableRow> tableItems = FXCollections.observableArrayList();

    /**
     * Initializes the screen after FXML injection.
     * <p>
     * Sets up table columns, enables inline editing for capacity, adds action buttons,
     * and applies numeric-only input restrictions for add-table fields.
     */
    @FXML
    private void initialize() {
        if (colTableNo != null) {
            colTableNo.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        }

        if (colSeats != null) {
            colSeats.setEditable(true);
            colSeats.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            colSeats.setCellFactory(column -> new TextFieldTableCell<>(new IntegerStringConverter()) {
                @Override
                public void startEdit() {
                    super.startEdit();
                    if (getGraphic() instanceof TextField textField) {
                        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                            if (!newVal) {
                                commitEdit(getConverter().fromString(textField.getText()));
                            }
                        });
                    }
                }
            });

            colSeats.setOnEditCommit(event -> {
                TableRow row = event.getRowValue();
                Integer newValue = event.getNewValue();
                if (row == null) return;

                if (newValue == null || newValue <= 0) {
                    setInfo("Seats must be a positive number.");
                    row.setCapacity(event.getOldValue());
                    tablesTable.refresh();
                    return;
                }
                row.setCapacity(newValue);
            });
        }

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        if (colNotes != null) {
            colNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));
        }

        if (colEdit != null) {
            colEdit.setCellFactory(column -> new TableCell<>() {
                private final Button button = new Button("Save");

                {
                    button.getStyleClass().add("primary");
                    button.setOnAction(event -> {
                        TableRow row = getTableView().getItems().get(getIndex());
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

        if (colDisable != null) {
            colDisable.setCellFactory(column -> new TableCell<>() {
                private final Button button = new Button("Disable");

                {
                    button.getStyleClass().add("ghost");
                    button.setOnAction(event -> {
                        TableRow row = getTableView().getItems().get(getIndex());
                        if (row != null) {
                            sendDisableRequest(row);
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

        if (tablesTable != null) {
            tablesTable.setItems(tableItems);
            tablesTable.setEditable(true);
        }

        initNumericField(newTableNumberField, 4);
        initNumericField(newCapacityField, 3);
        setInfo("Load tables to begin editing.");
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this controller.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * Button handler: reloads the tables list from the server.
     * <p>
     * Sends {@link ManagerCommand#VIEW_ALL_TABLES}.
     */
    @FXML
    private void onRefresh() {
        if (!readyForServer()) return;
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_ALL_TABLES));
        setInfo("Fetching tables...");
    }

    /**
     * Button handler: adds a new table using the provided number and capacity.
     * <p>
     * Sends {@link ManagerCommand#ADD_NEW_TABLE}.
     */
    @FXML
    private void onAddTable() {
        if (!readyForServer()) return;

        Integer tableNumber = parsePositiveInt(newTableNumberField, "Table number");
        if (tableNumber == null) return;

        Integer capacity = parsePositiveInt(newCapacityField, "Seats");
        if (capacity == null) return;

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.ADD_NEW_TABLE, tableNumber, capacity)
        );
        setInfo("Adding table " + tableNumber + "...");
        clearAddFields();
    }

    /**
     * Requests initial data when this tab/screen is opened by the parent controller.
     * <p>
     * Currently triggers {@link #onRefresh()}.
     */
    public void requestInitialData() {
        onRefresh();
    }

    /**
     * Handles server responses for table operations (view/add/edit/delete).
     *
     * @param response manager response received from the server; ignored if {@code null}
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null || response.getResponseCommand() == null) return;

        if (response.getResponseCommand() == ManagerResponseCommand.SHOW_ALL_TABLES_RESPONSE) {
            tableItems.clear();
            if (response.getTables() != null) {
                for (Object item : response.getTables()) {
                    if (item instanceof TableInfo info) {
                        tableItems.add(new TableRow(info.getTableNumber(), info.getCapacity()));
                    }
                }
            }
            setInfo("Tables updated.");
        } else if (response.getResponseCommand() == ManagerResponseCommand.EDIT_TABLE_RESPONSE) {
            setInfo("Table number " + response.getTable().getTableNumber()
                    + " was edited\n Nubmer of cancelled reservations " + response.getTables().size());
            onRefresh();
        } else if (response.getResponseCommand() == ManagerResponseCommand.NEW_TABLE_RESPONSE) {
            setInfo("Table " + response.getTable().getTableNumber() + " added successfully");
            onRefresh();
        } else if (response.getResponseCommand() == ManagerResponseCommand.DELETED_TABLE_RESPONSE) {
            setInfo("Table disabled, number of cancelled reservations: " + response.getTables().size());
        }
    }

    /**
     * Sends an edit-table request for the provided row (table number + capacity).
     * <p>
     * Sends {@link ManagerCommand#EDIT_TABLES}.
     *
     * @param row table row model containing the updated capacity
     */
    private void sendEditRequest(TableRow row) {
        if (!readyForServer()) return;

        if (row.getCapacity() <= 0) {
            setInfo("Seats must be a positive number.");
            return;
        }

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.EDIT_TABLES, row.getTableNumber(), row.getCapacity())
        );

        setInfo("Saving table " + row.getTableNumber() + "...");
    }

    /**
     * Sends a disable-table request for the provided row.
     * <p>
     * Sends {@link ManagerCommand#DELETE_TABLE}. The server is responsible for cancelling any affected reservations.
     *
     * @param row table row model identifying which table to disable
     */
    private void sendDisableRequest(TableRow row) {
        if (!readyForServer()) return;

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.DELETE_TABLE, row.getTableNumber())
        );
        setInfo("Disabling table " + row.getTableNumber() + "...");
    }

    /**
     * Checks whether the controller is connected and ready to send requests to the server.
     *
     * @return {@code true} if connected and {@link #clientController} is set; otherwise {@code false}
     */
    private boolean readyForServer() {
        if (!connected || clientController == null) {
            setInfo("Not connected to server.");
            return false;
        }
        return true;
    }

    /**
     * Restricts a {@link TextField} to numeric-only input and a maximum length.
     *
     * @param field     the field to restrict
     * @param maxLength maximum allowed number of characters
     */
    private void initNumericField(TextField field, int maxLength) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (!newText.matches("\\d*")) return null;
            if (newText.length() > maxLength) return null;
            return change;
        }));
    }

    /**
     * Parses a positive integer from a text field and validates it.
     *
     * @param field field containing the numeric text
     * @param label label used in error messages (e.g., "Seats")
     * @return parsed integer if valid; otherwise {@code null}
     * @throws NumberFormatException if the field contains non-numeric text (should be prevented by formatter)
     */
    private Integer parsePositiveInt(TextField field, String label) {
        if (field == null) {
            setInfo(label + " is missing.");
            return null;
        }
        String raw = field.getText() == null ? "" : field.getText().trim();
        if (raw.isEmpty()) {
            setInfo(label + " is required.");
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            setInfo(label + " must be a positive number.");
            return null;
        }
        return value;
    }

    /**
     * Clears the "add table" input fields.
     */
    private void clearAddFields() {
        if (newTableNumberField != null) newTableNumberField.clear();
        if (newCapacityField != null) newCapacityField.clear();
    }

    /**
     * Updates the info label.
     *
     * @param msg message to display (null-safe)
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Simple data model representing a row in {@link #tablesTable}.
     * <p>
     * Uses plain getters/setters so {@link PropertyValueFactory} can read properties.
     */
    public static class TableRow {
        private int tableNumber;
        private int capacity;
        private String status;
        private String notes;

        /**
         * Creates a new table row model.
         *
         * @param tableNumber table number
         * @param capacity    table capacity (seats)
         */
        public TableRow(int tableNumber, int capacity) {
            this.tableNumber = tableNumber;
            this.capacity = capacity;
            this.status = "Active";
            this.notes = "";
        }

        /**
         * @return table number displayed in the table
         */
        public int getTableNumber() {
            return tableNumber;
        }

        /**
         * @return current capacity (seats) displayed in the table
         */
        public int getCapacity() {
            return capacity;
        }

        /**
         * Sets the capacity (seats) value in the UI model.
         *
         * @param capacity new capacity value
         */
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        /**
         * @return status string (e.g., "Active")
         */
        public String getStatus() {
            return status;
        }

        /**
         * @return notes string (currently unused)
         */
        public String getNotes() {
            return notes;
        }
    }
}
