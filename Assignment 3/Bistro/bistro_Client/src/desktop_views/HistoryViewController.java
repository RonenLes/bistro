package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import desktop_screen.DesktopScreenController;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import responses.UserHistoryResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller responsible for displaying a user's reservation/billing history.
 * <p>
 * Features:
 * <ul>
 *   <li>Renders {@link UserHistoryResponse} rows in a {@link TableView}.</li>
 *   <li>Shows a discount banner for subscribers and applies the discount to displayed totals.</li>
 *   <li>Provides a month filter control (currently a placeholder; does not filter {@code items} yet).</li>
 * </ul>
 * <p>
 * Implements {@link ClientControllerAware} to receive a {@link ClientController} reference and connection status
 * from the parent desktop shell.
 */
public class HistoryViewController implements ClientControllerAware {

    /** Status/info label displayed above or near the history table. */
    @FXML private Label infoLabel;

    /** Discount banner container (visible only for active subscriber discounts). */
    @FXML private VBox discountBanner;
    /** Label within the discount banner showing the discount text/value. */
    @FXML private Label discountValueLabel;

    /** Month filter combo box (currently informational/placeholder). */
    @FXML private ComboBox<String> monthFilter;

    /** Table displaying history records. */
    @FXML private TableView<UserHistoryResponse> historyTable;
    /** Column: reservation date. */
    @FXML private TableColumn<UserHistoryResponse, String> colDate;
    /** Column: reserved-for time (scheduled reservation time). */
    @FXML private TableColumn<UserHistoryResponse, String> colReservedFor;
    /** Column: check-in time. */
    @FXML private TableColumn<UserHistoryResponse, String> colCheckIn;
    /** Column: check-out time. */
    @FXML private TableColumn<UserHistoryResponse, String> colCheckOut;
    /** Column: table number. */
    @FXML private TableColumn<UserHistoryResponse, String> colTable;
    /** Column: party size. */
    @FXML private TableColumn<UserHistoryResponse, String> colParty;
    /** Column: total price (discounted for subscribers when applicable). */
    @FXML private TableColumn<UserHistoryResponse, String> colTotal;
    /** Column: inferred status (Completed/Seated/No Show/Cancelled). */
    @FXML private TableColumn<UserHistoryResponse, String> colStatus;

    /** Observable list that backs {@link #historyTable}. */
    private final ObservableList<UserHistoryResponse> items = FXCollections.observableArrayList();

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;
    /** Indicates whether the client is currently connected to the server (injected by parent shell). */
    private boolean connected;

    /** Guards against duplicate auto-requests for history data. */
    private boolean historyRequested;

    /** Current user role (provided by {@link DesktopScreenController}); default is guest. */
    private DesktopScreenController.Role role = DesktopScreenController.Role.GUEST;
    /** Discount information to apply/display (provided by {@link DesktopScreenController}). */
    private DiscountInfo discountInfo = DiscountInfo.none();

    /** Date format used for displaying reservation dates. */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Time format used for displaying times. */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Sets up table column renderers, binds the table to {@link #items}, initializes month filter choices,
     * and applies initial discount banner visibility.
     */
    @FXML
    private void initialize() {
        initTableColumns();

        if (historyTable != null) {
            historyTable.setItems(items);
        }

        initMonthFilter();
        applyDiscountVisibility();
        setInfo("Loading history...");
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this view controller.
     * <p>
     * Triggers an automatic history request if possible (only once) via {@link #requestHistoryIfPossible()}.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
        requestHistoryIfPossible();
    }

    /**
     * Sets role and discount information used by this screen.
     * <p>
     * Intended to be called by {@link DesktopScreenController} so the view can:
     * <ul>
     *   <li>Show/hide the discount banner</li>
     *   <li>Apply the discount when displaying totals</li>
     * </ul>
     *
     * @param role         the current user role; if {@code null}, defaults to {@link DesktopScreenController.Role#GUEST}
     * @param discountInfo discount information; if {@code null}, defaults to {@link DiscountInfo#none()}
     */
    public void setUserContext(DesktopScreenController.Role role, DiscountInfo discountInfo) {
        this.role = role == null ? DesktopScreenController.Role.GUEST : role;
        this.discountInfo = discountInfo == null ? DiscountInfo.none() : discountInfo;
        applyDiscountVisibility();
    }

    /**
     * Renders history rows into the table.
     * <p>
     * Intended to be called by {@link DesktopScreenController} when server data arrives.
     *
     * @param rows history rows to render; may be {@code null}
     */
    public void renderHistory(List<UserHistoryResponse> rows) {
        items.clear();
        if (rows != null) {
            items.addAll(rows);
        }
        setInfo(items.isEmpty() ? "No history found." : "History loaded.");
    }

    /**
     * Attempts to request user history from the server automatically.
     * <p>
     * Sends a request only once, and only if a {@link ClientController} is set and {@code connected == true}.
     */
    private void requestHistoryIfPossible() {
        if (historyRequested) return;
        if (clientController == null) return;
        if (!connected) return;

        historyRequested = true;
        setInfo("loading history...");
        clientController.requestUserHistory();
    }

    /**
     * Displays a user-facing error message for history loading failures.
     * <p>
     * Clears the current table rows and sets the info label to the provided message (or a default message).
     *
     * @param message error message to display; may be {@code null} or blank
     */
    public void showHistoryError(String message) {
        items.clear();
        setInfo(message == null || message.isBlank() ? "failed to load history." : message);
    }

    /**
     * Initializes month filter options.
     * <p>
     * Current behavior: updates the info label to reflect selected filter choice.
     * A TODO indicates that actual filtering on {@link #items} should be implemented later.
     */
    private void initMonthFilter() {
        if (monthFilter == null) return;

        monthFilter.getItems().setAll(
                "All",
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        );
        monthFilter.getSelectionModel().select("All");

        monthFilter.valueProperty().addListener((obs, oldV, newV) -> {
            setInfo("Filter: " + (newV == null ? "All" : newV));
            // TODO: apply filtering on 'items' when you have real data
        });
    }

    /**
     * Initializes table column cell value factories to format {@link UserHistoryResponse} fields as strings.
     * <p>
     * Columns include date/time formatting, integer formatting, discounted total calculation,
     * and inferred status computation.
     */
    private void initTableColumns() {
        if (colDate != null) {
            colDate.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatDate(cd.getValue() == null ? null : cd.getValue().getReservationDate())
            ));
        }
        if (colReservedFor != null) {
            colReservedFor.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getReservedForTime())
            ));
        }
        if (colCheckIn != null) {
            colCheckIn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getCheckInTime())
            ));
        }
        if (colCheckOut != null) {
            colCheckOut.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTime(cd.getValue() == null ? null : cd.getValue().getCheckOutTime())
            ));
        }
        if (colTable != null) {
            colTable.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getTableNumber())
            ));
        }
        if (colParty != null) {
            colParty.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatInt(cd.getValue() == null ? 0 : cd.getValue().getPartySize())
            ));
        }
        if (colTotal != null) {
            colTotal.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                    formatTotal(cd.getValue() == null ? null : cd.getValue().getTotalPrice())
            ));
        }
        if (colStatus != null) {
            colStatus.setCellValueFactory(cd -> new ReadOnlyStringWrapper(inferStatus(cd.getValue())));
        }
    }

    /**
     * Applies visibility/managed state for the discount banner based on:
     * <ul>
     *   <li>Role is {@link DesktopScreenController.Role#SUBSCRIBER}</li>
     *   <li>{@link #discountInfo} is non-null and active</li>
     * </ul>
     * Updates the discount value label accordingly.
     */
    private void applyDiscountVisibility() {
        boolean show = role == DesktopScreenController.Role.SUBSCRIBER
                && discountInfo != null
                && discountInfo.isActive();

        if (discountBanner != null) {
            discountBanner.setVisible(show);
            discountBanner.setManaged(show);
        }
        if (discountValueLabel != null) {
            discountValueLabel.setText(show ? discountInfo.getDisplayText() : "");
        }
    }

    /**
     * Formats a {@link LocalDate} for display in the history table.
     *
     * @param d the date to format
     * @return formatted date string, or {@code "-"} if {@code d} is {@code null}
     */
    private String formatDate(LocalDate d) {
        return d == null ? "-" : d.format(DATE_FMT);
    }

    /**
     * Formats a {@link LocalTime} for display in the history table.
     *
     * @param t the time to format
     * @return formatted time string, or {@code "-"} if {@code t} is {@code null}
     */
    private String formatTime(LocalTime t) {
        return t == null ? "-" : t.format(TIME_FMT);
    }

    /**
     * Formats a positive integer for display; returns {@code "-"} for non-positive values.
     *
     * @param v value to format
     * @return string representation or {@code "-"} if {@code v <= 0}
     */
    private String formatInt(int v) {
        return v <= 0 ? "-" : String.valueOf(v);
    }

    /**
     * Formats the total price for display, applying subscriber discount when applicable.
     * <p>
     * If the current role is {@link DesktopScreenController.Role#SUBSCRIBER} and {@link #discountInfo} is active,
     * the displayed total is reduced by {@link DiscountInfo#getRate()} and rounded to two decimals.
     *
     * @param total the original total price (before discount); may be {@code null}
     * @return a formatted currency string (e.g. {@code "₪12.50"}) or {@code "-"} if {@code total} is {@code null}
     */
    private String formatTotal(Double total) {
        if (total == null) return "-";
        double value = total;
        if (role == DesktopScreenController.Role.SUBSCRIBER
                && discountInfo != null
                && discountInfo.isActive()
                && discountInfo.getRate() > 0) {
            value = roundMoney(total * (1.0 - discountInfo.getRate()));
        }
        return String.format("₪%.2f", value);
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
     * Infers a user-friendly history status string from a {@link UserHistoryResponse}.
     * <p>
     * Rules:
     * <ul>
     *   <li>If {@code status} equals "CANCELLED" (case-insensitive) → "Cancelled"</li>
     *   <li>If no check-in and no check-out → "No Show"</li>
     *   <li>If check-in exists and check-out missing → "Seated"</li>
     *   <li>If both check-in and check-out exist → "Completed"</li>
     *   <li>Otherwise → "-"</li>
     * </ul>
     *
     * @param r history row
     * @return inferred status string
     */
    private String inferStatus(UserHistoryResponse r) {
        if (r == null) return "-";
        String status = r.getStatus();
        if (status != null && status.equalsIgnoreCase("CANCELLED")) return "Cancelled";
        if (r.getCheckInTime() == null && r.getCheckOutTime() == null) return "No Show";
        if (r.getCheckInTime() != null && r.getCheckOutTime() == null) return "Seated";
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null) return "Completed";
        return "-";
    }

    /**
     * Sets the informational text displayed to the user.
     *
     * @param msg the message to display; may be {@code null}
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Value object that describes whether a discount is active and how to display/apply it.
     * <p>
     * Intended to be constructed and passed by the parent desktop shell.
     */
    public static final class DiscountInfo {
        /** Whether a discount is active. */
        private final boolean active;
        /** Display text shown to the user (e.g., "10% subscriber discount"). */
        private final String displayText;
        /** Discount rate as a fraction (e.g., {@code 0.10} for 10%). */
        private final double rate;

        /**
         * Creates a new discount info object.
         *
         * @param active      whether the discount is active
         * @param displayText text shown to the user; if {@code null}, becomes empty string
         * @param rate        discount rate (fraction)
         */
        private DiscountInfo(boolean active, String displayText, double rate) {
            this.active = active;
            this.displayText = displayText == null ? "" : displayText;
            this.rate = rate;
        }

        /**
         * Creates a discount info instance indicating no active discount.
         *
         * @return a non-active {@link DiscountInfo} with zero rate
         */
        public static DiscountInfo none() {
            return new DiscountInfo(false, "", 0.0);
        }

        /**
         * Creates an active discount info instance.
         *
         * @param displayText text shown to the user (e.g., "10% subscriber discount")
         * @param rate        discount rate as a fraction (e.g., {@code 0.10} for 10%)
         * @return an active {@link DiscountInfo}
         */
        public static DiscountInfo active(String displayText, double rate) {
            return new DiscountInfo(true, displayText, rate);
        }

        /**
         * Indicates whether the discount is active.
         *
         * @return {@code true} if active; otherwise {@code false}
         */
        public boolean isActive() {
            return active;
        }

        /**
         * Returns the display text for the discount banner.
         *
         * @return display text (never {@code null})
         */
        public String getDisplayText() {
            return displayText;
        }

        /**
         * Returns the discount rate as a fraction.
         *
         * @return discount rate (e.g., {@code 0.10} for 10%)
         */
        public double getRate() {
            return rate;
        }
    }
}
