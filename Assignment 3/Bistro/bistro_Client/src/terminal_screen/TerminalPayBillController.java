package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import requests.BillRequest.BillRequestType;

/**
 * JavaFX controller for a terminal screen that fetches and pays bills.
 * <p>
 * Workflow:
 * <ol>
 *   <li>Fetch the bill total by confirmation code (or resolve code from user ID, then fetch).</li>
 *   <li>Pay the bill (enabled only after a successful fetch).</li>
 * </ol>
 * </p>
 * <p>
 * The controller maintains a small state machine using {@link #billLoaded}, {@link #lastBaseTotal},
 * and {@link #lastConfirmationCode} to ensure the user fetches the bill before paying.
 * </p>
 * <p>
 * When connected, requests are sent via {@link ClientController}. When offline, the controller shows an
 * error status and does not send requests.
 * </p>
 */
public class TerminalPayBillController implements ClientControllerAware {

    @FXML private TextField reservationOrTableField;
    @FXML private ListView<String> billItemsList;
    @FXML private Button fetchBillBtn;
    @FXML private Button payBtn;
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * Indicates whether the bill was successfully fetched and is ready to be paid.
     */
    private boolean billLoaded = false;

    /**
     * Cached bill total from the last successful fetch step.
     */
    private Double lastBaseTotal;

    /**
     * Cached confirmation code used in the last successful fetch step.
     */
    private Integer lastConfirmationCode;

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Initializes:
     * <ul>
     *   <li>Numeric-only input for {@link #reservationOrTableField}</li>
     *   <li>Default list message</li>
     *   <li>Pay button disabled state (until bill is fetched)</li>
     *   <li>Status label</li>
     * </ul>
     * </p>
     */
    @FXML
    private void initialize() {
        // restrict input to numeric only
        if (reservationOrTableField != null) {
            reservationOrTableField.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                if (!newText.matches("\\d*")) return null;
                if (newText.length() > 10) return null;
                return change;
            }));
        }

        if (billItemsList != null) billItemsList.getItems().setAll("Bill will appear here...");

        // pay button disabled until bill is fetched
        if (payBtn != null) payBtn.setDisable(true);

        setStatus("", false);
    }

    /**
     * Injects the {@link ClientController} used to communicate with the server and sets the current
     * connection state.
     *
     * @param controller the application-level client controller used for server communication
     * @param connected  {@code true} if connected to the server; {@code false} otherwise
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * UI handler for step 1: fetch the bill total from the server.
     * <p>
     * Parses and validates the confirmation code, resets the local bill state, disables the Pay button,
     * and sends {@link BillRequestType#REQUEST_TO_SEE_BILL}.
     * </p>
     */
    @FXML
    private void onFetchBill() {
        Integer code = parseConfirmationCode();
        if (code == null) return;

        if (!connected || clientController == null) {
            setStatus("Terminal is offline.", true);
            return;
        }

        // reset state for new fetch
        billLoaded = false;
        lastBaseTotal = null;
        lastConfirmationCode = code;

        if (payBtn != null) {
            payBtn.setDisable(true);
        }
        if (billItemsList != null) {
            billItemsList.getItems().setAll("Fetching bill...");
        }
        setStatus("Fetching bill total...", false);

        clientController.requestBillAction(BillRequestType.REQUEST_TO_SEE_BILL, code, true);
    }

    /**
     * UI handler for step 2: pay the bill after it was successfully fetched.
     * <p>
     * Requires {@link #billLoaded} to be {@code true}. If the bill was not fetched yet, shows an error.
     * Sends {@link BillRequestType#PAY_BILL} upon success.
     * </p>
     */
    @FXML
    private void onPay() {
        Integer code = lastConfirmationCode != null ? lastConfirmationCode : parseConfirmationCode();
        if (code == null) return;

        if (!billLoaded) {
            setStatus("Load the bill first.", true);
            return;
        }
        if (!connected || clientController == null) {
            setStatus("Terminal is offline.", true);
            return;
        }

        if (payBtn != null) payBtn.setDisable(true);
        setStatus("Processing payment...", false);

        clientController.requestBillAction(BillRequestType.PAY_BILL, code, true);
    }

    /**
     * UI handler that resets the screen to its initial state.
     * <p>
     * Clears input, clears cached values, resets the list view, disables Pay, and clears status.
     * </p>
     */
    @FXML
    private void onClear() {
        if (reservationOrTableField != null) reservationOrTableField.clear();
        if (billItemsList != null) billItemsList.getItems().setAll("Bill will appear here...");
        billLoaded = false;
        lastBaseTotal = null;
        lastConfirmationCode = null;
        if (payBtn != null) {
            payBtn.setDisable(true);
        }
        setStatus("", false);
    }

    /**
     * Callback invoked by the parent controller when the bill total is received successfully.
     * <p>
     * Updates cached values, renders the total in the list view, and enables the Pay button.
     * </p>
     *
     * @param baseTotal bill total returned by the server
     */
    public void onBillTotalLoaded(double baseTotal) {
        lastBaseTotal = baseTotal;
        billLoaded = true;
        if (billItemsList != null) {
            billItemsList.getItems().setAll("Total due: " + formatMoney(baseTotal));
        }
        // enable pay button after successful fetch
        if (payBtn != null) {
            payBtn.setDisable(false);
        }
        setStatus("Bill ready.", false);
    }

    /**
     * Callback invoked by the parent controller when payment succeeds.
     * <p>
     * Resets bill state, renders a completion message, and disables Pay.
     * </p>
     */
    public void onBillPaid() {
        billLoaded = false;
        lastBaseTotal = null;
        if (billItemsList != null) {
            billItemsList.getItems().setAll("Payment completed. תודה! ✅");
        }
        if (payBtn != null) {
            payBtn.setDisable(true);
        }
        setStatus("Payment completed.", false);
    }

    /**
     * Callback invoked by the parent controller when a billing operation fails.
     * <p>
     * Disables Pay and displays an error status message.
     * </p>
     *
     * @param message error message to show; if {@code null}, a default message is displayed
     */
    public void onBillingError(String message) {
        billLoaded = false;
        if (payBtn != null) {
            payBtn.setDisable(true);
        }
        setStatus(message == null ? "Billing failed." : message, true);
    }

    /**
     * Parses and validates the confirmation code from {@link #reservationOrTableField}.
     *
     * @return confirmation code as {@link Integer}, or {@code null} if validation fails
     */
    private Integer parseConfirmationCode() {
        String txt = reservationOrTableField == null ? "" : reservationOrTableField.getText().trim();
        if (txt.isEmpty()) {
            setStatus("Enter confirmation code.", true);
            return null;
        }
        if (!txt.matches("\\d{1,10}")) {
            setStatus("Confirmation code must be numeric.", true);
            return null;
        }
        try {
            int code = Integer.parseInt(txt);
            if (code <= 0) {
                setStatus("Confirmation code must be positive.", true);
                return null;
            }
            return code;
        } catch (NumberFormatException ex) {
            setStatus("Confirmation code must be numeric.", true);
            return null;
        }
    }

    /**
     * UI handler for an alternative flow: resolve confirmation code from subscriber user ID.
     * <p>
     * Prompts for user ID, requests code from the server, registers a callback to receive the code,
     * then triggers {@link #onFetchBill()} once the code is resolved.
     * </p>
     */
    @FXML
    private void onUseUserId() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Use user ID");
        dialog.setHeaderText("Enter subscriber user ID");
        dialog.setContentText("User ID:");

        dialog.showAndWait().ifPresent(input -> {
            String userId = input == null ? "" : input.trim();
            if (userId.isEmpty()) {
                setStatus("Enter user ID.", true);
                return;
            }
            if (!connected || clientController == null) {
                setStatus("Terminal is offline.", true);
                return;
            }

            // set callback to receive resolved code from server
            clientController.setLostCodeListener(code -> {
                clientController.clearLostCodeListener();
                if (code == null || code <= 0) {
                    setStatus("Failed to resolve confirmation code.", true);
                    return;
                }
                // populate field and trigger bill fetch
                if (reservationOrTableField != null) {
                    reservationOrTableField.setText(String.valueOf(code));
                }
                onFetchBill();
            });
            setStatus("Resolving confirmation code...", false);
            clientController.requestLostCode(userId);
        });
    }

    /**
     * Formats a monetary value for display with two decimal digits.
     *
     * @param value numeric bill amount
     * @return formatted string (e.g., "12.50")
     */
    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Updates the status label with styling based on whether it is an error state.
     *
     * @param msg   message to show; if {@code null}, an empty string is displayed
     * @param error {@code true} to show an error style; {@code false} to show a success/normal style
     */
    private void setStatus(String msg, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(msg == null ? "" : msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }
}
