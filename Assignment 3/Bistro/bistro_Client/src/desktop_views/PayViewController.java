package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import requests.BillRequest.BillRequestType;

/**
 * Controller for the "Pay" (billing/payment) desktop flow.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Accepts a reservation confirmation code and requests the bill total from the server.</li>
 *   <li>Displays a summary (base total, discount amount, and final total).</li>
 *   <li>Submits a payment request (cash or card) to the server.</li>
 * </ul>
 * <p>
 * Discount behavior:
 * <ul>
 *   <li>Guests: no discount</li>
 *   <li>Subscribers: 10% discount (computed client-side when bill total is loaded)</li>
 * </ul>
 * <p>
 * Note: bill line-items are planned for later; currently only totals are displayed.
 * <p>
 * Implements {@link ClientControllerAware} to receive a {@link ClientController} reference and connection status
 * from the parent desktop shell.
 */
public class PayViewController implements ClientControllerAware {

    /** Input field for the reservation confirmation code. */
    @FXML private TextField confirmationCodeField;
    /** Radio button for selecting card payment. */
    @FXML private RadioButton cardRadio;
    /** Radio button for selecting cash payment. */
    @FXML private RadioButton cashRadio;
    /** Toggle group binding the payment method radio buttons. */
    @FXML private ToggleGroup paymentToggleGroup;

    /** Button used to fetch/show the bill total. */
    @FXML private Button showBillButton;
    /** Button used to pay the bill once totals are loaded. */
    @FXML private Button payBillButton;

    /** Status label used for errors/success/info messages. */
    @FXML private Label statusLabel;

    /** Summary: selected payment method. */
    @FXML private Label paymentMethodValueLabel;
    /** Summary: base total returned by the server (before discount). */
    @FXML private Label baseTotalValueLabel;
    /** Summary: discount amount (if applicable). */
    @FXML private Label discountValueLabel;
    /** Summary: final total to pay after discount (if applicable). */
    @FXML private Label totalToPayValueLabel;

    /** Placeholder UI container for future bill line-items. */
    @FXML private javafx.scene.layout.VBox billPlaceholderContainer;

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;
    /** Indicates whether the client is currently connected to the server (injected by parent shell). */
    private boolean connected;

    /** Cached base total, used to decide whether the pay button can be re-enabled after an error. */
    private Double lastBaseTotal;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Resets status and summary, disables the pay button until totals are loaded,
     * and ensures the placeholder container is visible.
     */
    @FXML
    private void initialize() {
        setStatus("", false);
        clearSummary();

        if (payBillButton != null) {
            payBillButton.setDisable(true);
        }

        if (billPlaceholderContainer != null) {
            billPlaceholderContainer.setManaged(true);
            billPlaceholderContainer.setVisible(true);
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
     * Button handler: requests the bill total for the provided confirmation code.
     * <p>
     * Sends {@link BillRequestType#REQUEST_TO_SEE_BILL} to the server.
     * Disables the pay button until totals are returned.
     */
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

        clearSummary();
        lastBaseTotal = null;

        if (payBillButton != null) {
            payBillButton.setDisable(true);
        }

        boolean isCash = isCashSelected();
        setStatus("Fetching bill total...", false);

        clientController.requestBillAction(
                BillRequestType.REQUEST_TO_SEE_BILL,
                code,
                isCash
        );
    }

    /**
     * Button handler: submits a payment request for the provided confirmation code.
     * <p>
     * Sends {@link BillRequestType#PAY_BILL} to the server.
     * Disables the pay button while processing.
     */
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

        if (payBillButton != null) {
            payBillButton.setDisable(true);
        }

        clientController.requestBillAction(
                BillRequestType.PAY_BILL,
                code,
                isCash
        );
    }

    /**
     * Called by {@code DesktopScreenController} when a bill total is returned.
     * <p>
     * Computes the discount client-side:
     * <ul>
     *   <li>Subscribers: 10%</li>
     *   <li>Non-subscribers: 0%</li>
     * </ul>
     * Updates the summary labels and enables the pay button.
     *
     * @param baseTotal      the bill total returned by the server (before discount)
     * @param isCashPayment  whether the chosen payment method is cash
     * @param isSubscriber   whether the current user is a subscriber (affects discount)
     */
    public void onBillTotalLoaded(double baseTotal, boolean isCashPayment, boolean isSubscriber) {
        lastBaseTotal = baseTotal;

        double discountRate = isSubscriber ? 0.10 : 0.0;
        double discountAmount = roundMoney(baseTotal * discountRate);
        double finalTotal = roundMoney(baseTotal - discountAmount);

        setSummary(isCashPayment, baseTotal, discountAmount, finalTotal);
        setStatus("Bill ready.", false);

        if (payBillButton != null) {
            payBillButton.setDisable(false);
        }
    }

    /**
     * Called by {@code DesktopScreenController} when payment is confirmed by the server.
     * <p>
     * Updates the status message and disables the pay button.
     *
     * @param tableNumber the table number that becomes available after payment; may be {@code null}
     */
    public void onBillPaid(Integer tableNumber) {
        String msg = "Payment completed.";
        if (tableNumber != null) {
            msg += " Table " + tableNumber + " is now available.";
        }
        setStatus(msg, false);

        lastBaseTotal = null;

        if (payBillButton != null) {
            payBillButton.setDisable(true);
        }
    }

    /**
     * Called by {@code DesktopScreenController} when a billing operation fails.
     * <p>
     * Displays an error message and re-enables the pay button only if totals were previously loaded.
     *
     * @param message the error message to display; may be {@code null}
     */
    public void onBillingError(String message) {
        setStatus(message == null ? "Billing failed." : message, true);

        if (payBillButton != null) {
            payBillButton.setDisable(lastBaseTotal == null);
        }
    }

    /**
     * Programmatically loads a bill for the given confirmation code (e.g., invoked from subscriber home screen).
     * <p>
     * Writes the code into the input field and triggers the same logic as {@link #onShowBillClicked()}.
     *
     * @param confirmationCode confirmation code to load
     */
    public void loadBillForConfirmationCode(int confirmationCode) {
        if (confirmationCodeField == null) {
            setStatus("Confirmation code field is not available.", true);
            return;
        }
        confirmationCodeField.setText(String.valueOf(confirmationCode));
        onShowBillClicked();
    }

    /**
     * Parses and validates the confirmation code from {@link #confirmationCodeField}.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Field must exist (non-null)</li>
     *   <li>Value must be non-empty</li>
     *   <li>Value must be numeric</li>
     *   <li>Value must be a positive integer</li>
     * </ul>
     *
     * @return parsed confirmation code, or {@code null} if validation fails
     */
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

    /**
     * Determines whether cash payment is selected.
     *
     * @return {@code true} if cash is selected; otherwise {@code false}
     */
    private boolean isCashSelected() {
        if (cashRadio != null && cashRadio.isSelected()) {
            return true;
        }
        return false;
    }

    /**
     * Clears all summary labels back to default placeholder values.
     */
    private void clearSummary() {
        if (paymentMethodValueLabel != null) paymentMethodValueLabel.setText("-");
        if (baseTotalValueLabel != null) baseTotalValueLabel.setText("-");
        if (discountValueLabel != null) discountValueLabel.setText("-");
        if (totalToPayValueLabel != null) totalToPayValueLabel.setText("-");
    }

    /**
     * Sets all summary labels based on current payment method and computed totals.
     *
     * @param isCashPayment  whether payment method is cash
     * @param baseTotal      base total before discount
     * @param discountAmount discount amount (0 if none)
     * @param finalTotal     total to pay after discount
     */
    private void setSummary(boolean isCashPayment, double baseTotal, double discountAmount, double finalTotal) {
        if (paymentMethodValueLabel != null) {
            paymentMethodValueLabel.setText(isCashPayment ? "cash" : "card");
        }
        if (baseTotalValueLabel != null) {
            baseTotalValueLabel.setText(formatMoney(baseTotal));
        }
        if (discountValueLabel != null) {
            discountValueLabel.setText(discountAmount <= 0 ? "-" : ("-" + formatMoney(discountAmount)));
        }
        if (totalToPayValueLabel != null) {
            totalToPayValueLabel.setText(formatMoney(finalTotal));
        }
    }

    /**
     * Formats a monetary value to two decimals (no currency symbol).
     *
     * @param value the value to format
     * @return formatted string with two decimals
     */
    private String formatMoney(double value) {
        return String.format("%.2f", value);
    }

    /**
     * Rounds a monetary value to two decimal places.
     *
     * @param value value to round
     * @return rounded value (two decimals)
     */
    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Sets the status label text and formatting.
     *
     * @param message message to display; may be {@code null}
     * @param error   whether to render the message as an error
     */
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
