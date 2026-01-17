package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import requests.ManagerRequest;
import requests.ManagerRequest.ManagerCommand;
import requests.TableInfo;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;

/**
 * Manager-facing view controller for table management operations.
 * <p>
 * Provides basic CRUD-style actions for restaurant tables:
 * <ul>
 *   <li>View all tables</li>
 *   <li>Add a new table</li>
 *   <li>Edit an existing table's capacity</li>
 *   <li>Remove a table</li>
 * </ul>
 * <p>
 * Displays tables in a {@link TableView} using {@link TableInfo} entries.
 * Implements {@link ClientControllerAware} to receive a {@link ClientController}
 * reference and connection status from the parent desktop shell.
 */
public class TablesViewController implements ClientControllerAware {

    /** Label used to display user-facing status and error messages. */
    @FXML private Label infoLabel;
    /** Table view listing all tables returned from the server. */
    @FXML private TableView<TableInfo> tablesTable;
    /** Column showing table number. */
    @FXML private TableColumn<TableInfo, Integer> colTableNo;
    /** Column showing table capacity (seats). */
    @FXML private TableColumn<TableInfo, Integer> colSeats;
    /** Text field input for table number (add/edit/remove). */
    @FXML private TextField tableNumberField;
    /** Text field input for capacity (add/edit). */
    @FXML private TextField capacityField;

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;
    /** Whether the client is currently connected to the server (injected by parent shell). */
    private boolean connected;

    /** Observable list backing the {@link #tablesTable} UI component. */
    private final ObservableList<TableInfo> tableItems = FXCollections.observableArrayList();

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Configures table columns and binds the table view to {@link #tableItems}.
     */
    @FXML
    private void initialize() {
        setInfo("Tables overview (demo).");
        setInfo("Tables overview.");
        if (colTableNo != null) {
            colTableNo.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        }
        if (colSeats != null) {
            colSeats.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        }
        if (tablesTable != null) {
            tablesTable.setItems(tableItems);
        }
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this view controller.
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
     * Button handler: requests a full refresh of the tables list from the server.
     * <p>
     * Sends {@link ManagerCommand#VIEW_ALL_TABLES} if the client is connected.
     */
    @FXML
    private void onRefreshTables() {
        setInfo("Refreshing tables (placeholder).");
        if (!readyForServer()) return;
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_ALL_TABLES, 0));
        setInfo("Fetching tables...");
    }

    /**
     * Button handler: sends an "add table" request to the server using values from the input fields.
     * <p>
     * Sends {@link ManagerCommand#ADD_NEW_TABLE}.
     */
    @FXML
    private void onAddTable() {
        Integer tableNumber = parseInt(tableNumberField, "Table number is required.");
        Integer capacity = parseInt(capacityField, "Capacity is required.");
        if (tableNumber == null || capacity == null) return;
        if (!readyForServer()) return;

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.ADD_NEW_TABLE, tableNumber, capacity)
        );
        setInfo("Adding table " + tableNumber + "...");
    }

    /**
     * Button handler: sends an "edit table" request to update capacity for an existing table.
     * <p>
     * Sends {@link ManagerCommand#EDIT_TABLES}.
     */
    @FXML
    private void onEditTable() {
        Integer tableNumber = parseInt(tableNumberField, "Table number is required.");
        Integer capacity = parseInt(capacityField, "New capacity is required.");
        if (tableNumber == null || capacity == null) return;
        if (!readyForServer()) return;

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.EDIT_TABLES, tableNumber, capacity)
        );
        setInfo("Updating table " + tableNumber + "...");
    }

    /**
     * Button handler: sends a "remove table" request for the specified table number.
     * <p>
     * Sends {@link ManagerCommand#DELETE_TABLE}.
     */
    @FXML
    private void onRemoveTable() {
        Integer tableNumber = parseInt(tableNumberField, "Table number is required.");
        if (tableNumber == null) return;
        if (!readyForServer()) return;

        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.DELETE_TABLE, tableNumber)
        );
        setInfo("Removing table " + tableNumber + "...");
    }

    /**
     * Handles manager responses routed by the desktop shell.
     * <p>
     * Typical behaviors:
     * <ul>
     *   <li>{@code SHOW_ALL_TABLES_RESPONSE}: updates {@link #tableItems} from the response payload.</li>
     *   <li>{@code NEW_TABLE_RESPONSE}/{@code EDIT_TABLE_RESPONSE}/{@code DELETED_TABLE_RESPONSE}: refreshes tables.</li>
     * </ul>
     *
     * @param response the manager response received from the server; ignored if {@code null}
     */
    public void handleManagerResponse(ManagerResponse response) {
        if (response == null) return;
        ManagerResponseCommand command = response.getResponseCommand();
        if (command == null) return;

        switch (command) {
            case SHOW_ALL_TABLES_RESPONSE -> {
                tableItems.clear();
                if (response.getTables() != null) {
                    for (Object item : response.getTables()) {
                        if (item instanceof TableInfo info) {
                            tableItems.add(info);
                        }
                    }
                }
                setInfo("Tables updated.");
            }
            case NEW_TABLE_RESPONSE, EDIT_TABLE_RESPONSE, DELETED_TABLE_RESPONSE -> {
                onRefreshTables();
            }
            default -> {
            }
        }
    }

    /**
     * Parses an integer value from a {@link TextField}.
     *
     * @param field        the text field to read from
     * @param emptyMessage message to display if the field is empty
     * @return parsed integer value, or {@code null} if the input is missing/invalid
     */
    private Integer parseInt(TextField field, String emptyMessage) {
        if (field == null) return null;
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.isEmpty()) {
            setInfo(emptyMessage);
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            setInfo("Must be a number.");
            return null;
        }
    }

    /**
     * Checks whether this controller is ready to send requests to the server.
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
     * Updates the on-screen informational label.
     *
     * @param msg message to display (may be {@code null})
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg);
    }
}
