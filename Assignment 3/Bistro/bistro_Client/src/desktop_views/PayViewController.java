package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import requests.BillRequest;
import requests.BillRequest.BillRequestType;

import java.util.List;

public class PayViewController implements ClientControllerAware {

    @FXML private TextField confirmationCodeField;
    @FXML private RadioButton cardRadio;
    @FXML private RadioButton cashRadio;
    @FXML private ToggleGroup paymentToggleGroup;

    @FXML private Button showBillButton;
    @FXML private Button payBillButton;

    @FXML private Label statusLabel;
    @FXML private Label billSummaryLabel;

    @FXML private VBox billContainer;

    private ClientController clientController;
    private boolean connected;

    @FXML
    private void initialize() {
        if (statusLabel != null) {
            statusLabel.setText("");
        }
        if (billSummaryLabel != null) {
            billSummaryLabel.setText("");
        }
    }

    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void onShowBillClicked() {
        Integer code = parseConfirmationCode();
        if (code == null) {
            return;
        }
        if (clientController == null || !connected) {
            setStatus("Not connected to server.", true);
            return;
        }

        clearBillView();
        setStatus("Fetching bill...", false);

        boolean isCash = isCashSelected();

        //clientController.requestBillAction(
             //   BillRequestType.REQUEST_TO_SEE_BILL,
               // code,
              //  isCash
        //);
    }

    @FXML
    private void onPayBillClicked() {
        Integer code = parseConfirmationCode();
        if (code == null) {
            return;
        }
        if (clientController == null || !connected) {
            setStatus("Not connected to server.", true);
            return;
        }

        boolean isCash = isCashSelected();
        setStatus("Processing payment...", false);

        clientController.requestBillAction(
                BillRequestType.PAY_BILL,
                code,
                isCash
        );
    }

    
    /**
     * called by DesktopScreenController or ClientUI handler layer when a bill has been loaded
     * renders bill lines and summary into the scrollable container
     */
    public void renderBill(List<String> billLines, String summaryText) {
        clearBillView();

        if (billLines != null) {
            for (String line : billLines) {
                Label row = new Label(line);
                row.getStyleClass().add("body");
                billContainer.getChildren().add(row);
            }
        }
        if (billSummaryLabel != null) {
            billSummaryLabel.setText(summaryText == null ? "" : summaryText);
        }
    }

    private Integer parseConfirmationCode() {
        if (confirmationCodeField == null) {
            setStatus("Confirmation code field is not available.", true);
            return null;
        }
        String raw = confirmationCodeField.getText();
        if (raw == null || raw.trim().isEmpty()) {
            setStatus("Please enter a confirmation code.", true);
            return null;
        }
        try {
            int code = Integer.parseInt(raw.trim());
            if (code <= 0) {
                setStatus("Confirmation code must be a positive number.", true);
                return null;
            }
            return code;
        } catch (NumberFormatException ex) {
            setStatus("Confirmation code must be numeric.", true);
            return null;
        }
    }

    private boolean isCashSelected() {
        if (cashRadio != null && cashRadio.isSelected()) {
            return true;
        }
        return false;
    }

    private void clearBillView() {
        if (billContainer != null) {
            billContainer.getChildren().clear();
        }
        if (billSummaryLabel != null) {
            billSummaryLabel.setText("");
        }
    }

    private void setStatus(String message, boolean error) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setText(message == null ? "" : message);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }
}
