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

public class EditTableScreenController implements ClientControllerAware {

    @FXML private Label infoLabel;
    @FXML private TableView<TableRow> tablesTable;
    @FXML private TableColumn<TableRow, Integer> colTableNo;
    @FXML private TableColumn<TableRow, Integer> colSeats;
    @FXML private TableColumn<TableRow, String> colStatus;
    @FXML private TableColumn<TableRow, String> colNotes;
    @FXML private TableColumn<TableRow, Void> colEdit;
    @FXML private TableColumn<TableRow, Void> colDisable;

    private ClientController clientController;
    private boolean connected;
    private final ObservableList<TableRow> tableItems = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (colTableNo != null) {
            colTableNo.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        }
        if (colSeats != null) {
            colSeats.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            colSeats.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            colSeats.setOnEditCommit(event -> {
                TableRow row = event.getRowValue();
                if (row != null) {
                    row.setCapacity(event.getNewValue());
                }
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
        setInfo("Load tables to begin editing.");
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onRefresh() {
        if (!readyForServer()) return;
        clientController.requestManagerAction(new ManagerRequest(ManagerCommand.VIEW_ALL_TABLES, 0));
        setInfo("Fetching tables...");
    }

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
        } else if (response.getResponseCommand() == ManagerResponseCommand.EDIT_TABLE_RESPONSE
                || response.getResponseCommand() == ManagerResponseCommand.DELETED_TABLE_RESPONSE
                || response.getResponseCommand() == ManagerResponseCommand.NEW_TABLE_RESPONSE) {
            onRefresh();
        }
    }

    private void sendEditRequest(TableRow row) {
        if (!readyForServer()) return;
        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.EDIT_TABLES, row.getTableNumber(), row.getCapacity())
        );
        setInfo("Saving table " + row.getTableNumber() + "...");
    }

    private void sendDisableRequest(TableRow row) {
        if (!readyForServer()) return;
        clientController.requestManagerAction(
                new ManagerRequest(ManagerCommand.DELETE_TABLE, row.getTableNumber())
        );
        setInfo("Disabling table " + row.getTableNumber() + "...");
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

    public static class TableRow {
        private int tableNumber;
        private int capacity;
        private String status;
        private String notes;

        public TableRow(int tableNumber, int capacity) {
            this.tableNumber = tableNumber;
            this.capacity = capacity;
            this.status = "Active";
            this.notes = "";
        }

        public int getTableNumber() {
            return tableNumber;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public String getStatus() {
            return status;
        }

        public String getNotes() {
            return notes;
        }
    }
}