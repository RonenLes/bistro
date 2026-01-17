package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import requests.ReservationRequest.ReservationRequestType;
import responses.ReservationResponse;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.DateCell;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller for the two-phase reservation creation flow on desktop.
 * <p>
 * Flow:
 * <ul>
 *   <li><b>Phase 1</b>: user selects date + party size, then the server returns availability (or suggestions).</li>
 *   <li><b>Phase 2</b>: user selects a specific time slot and the client submits a confirmation request.</li>
 * </ul>
 * <p>
 * Supports both subscriber and guest reservations:
 * <ul>
 *   <li>Guest: uses a guest contact string.</li>
 *   <li>Subscriber: uses the logged-in subscriber user ID / username from the {@link ClientController} session.</li>
 * </ul>
 * <p>
 * Also supports a manager "override identity" mode, where a manager can create a reservation on behalf of
 * a specific subscriber or guest (by calling {@link #setReservationIdentity(String, String)}).
 * <p>
 * Implements {@link ClientControllerAware} to receive a {@link ClientController} reference and connection status
 * from the parent desktop shell.
 */
public class ReservationsViewController implements ClientControllerAware {

    /** Party size selection spinner (step 1). */
    @FXML private Spinner<Integer> partySizeSpinner;
    /** Date picker for reservation date selection (step 1). */
    @FXML private DatePicker datePicker;
    /** General info/status label for the reservation flow. */
    @FXML private Label infoLabel;

    /** Step 1 container (date + party size selection). */
    @FXML private VBox step1Pane;
    /** Step 2 container (availability/suggestions). */
    @FXML private VBox step2Pane;
    /** Tile pane injected from FXML (not used directly when creating dynamic grids). */
    @FXML private TilePane slotsTile;
    /** Header label shown above the slots area. */
    @FXML private Label slotHeaderLabel;
    /** Label used by {@link ReservationSlotsUI} for slot-specific messages. */
    @FXML private Label slotInfoLabel;
    /** Container where dynamically created slot sections (headers + grids) are placed. */
    @FXML private VBox slotsContainer;

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;

    /** Currently selected reservation date used for requests and UI state. */
    private LocalDate currentDate;
    /** Currently selected party size used for requests and UI state. */
    private int currentPartySize;
    /** Subscriber user ID used when not in guest mode. */
    private String userID;

    /** Whether the current reservation flow is for a guest session. */
    private boolean guestMode;
    /** Guest contact used when in guest mode. */
    private String guestContact;
    /** Whether the client is currently connected to the server. */
    private boolean connected;

    /** Manager override subscriber ID. */
    private String overrideUserId;
    /** Manager override guest contact. */
    private String overrideGuestContact;
    /** Whether identity override is active. */
    private boolean overrideIdentity;

    /** Minimum allowed party size. */
    private static final int MIN_PARTY = 1;
    /** Maximum allowed party size. */
    private static final int MAX_PARTY = 20;

    /** Time formatter used in status messages. */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Preferred slot button width used by {@link ReservationSlotsUI}. */
    private static final double SLOT_WIDTH = 130.0;
    /** Preferred slot button height used by {@link ReservationSlotsUI}. */
    private static final double SLOT_HEIGHT = 55.0;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Configures party size spinner + date picker limits and shows the initial step.
     */
    @FXML
    private void initialize() {
        initPartySizeSpinner();
        initDatePickerLimit();
        switchToStep1();
        setInfo("Pick party size + date, then choose a time block.");
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

        refreshIdentityFromController();
    }

    /**
     * Configures the date picker to only allow selecting today through the next 6 days.
     * <p>
     * Uses a {@link DateCell} factory to disable dates outside the allowed range and clamps selection.
     */
    private void initDatePickerLimit() {
        if (datePicker == null) return;

        LocalDate today = LocalDate.now();
        LocalDate max = today.plusDays(6);

        datePicker.setValue(today);

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;

                boolean disabled = item.isBefore(today) || item.isAfter(max);
                setDisable(disabled);

                if (disabled) {
                    setStyle("-fx-opacity: 0.45;");
                } else {
                    setStyle("");
                }
            }
        });

        datePicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            if (newV.isBefore(today)) datePicker.setValue(today);
            else if (newV.isAfter(max)) datePicker.setValue(max);
        });
    }

    /**
     * Configures the party size spinner with bounds and input validation.
     * <p>
     * Spinner is editable; a {@link TextFormatter} restricts input to up to two digits,
     * and {@link #clampPartySize()} is applied on enter/focus loss.
     */
    private void initPartySizeSpinner() {
        if (partySizeSpinner == null) return;

        partySizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_PARTY, MAX_PARTY, 2)
        );
        partySizeSpinner.setEditable(true);

        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) return change;
            return newText.matches("\\d{0,2}") ? change : null;
        });

        partySizeSpinner.getEditor().setTextFormatter(formatter);
        partySizeSpinner.getEditor().setOnAction(e -> clampPartySize());
        partySizeSpinner.getEditor().focusedProperty().addListener((obs, was, isNow) -> {
            if (!isNow) clampPartySize();
        });
    }

    /**
     * Ensures the party size value is within the allowed bounds ({@link #MIN_PARTY} to {@link #MAX_PARTY}).
     * <p>
     * Reads the value from the spinner editor, parses it to an integer, clamps it, and writes it back.
     */
    private void clampPartySize() {
        if (partySizeSpinner == null || partySizeSpinner.getValueFactory() == null) return;

        String txt = partySizeSpinner.getEditor() == null ? "" : partySizeSpinner.getEditor().getText();
        int v;
        try { v = Integer.parseInt(txt); }
        catch (Exception e) { v = MIN_PARTY; }

        if (v < MIN_PARTY) v = MIN_PARTY;
        if (v > MAX_PARTY) v = MAX_PARTY;

        partySizeSpinner.getValueFactory().setValue(v);
    }

    /**
     * Button handler for "Show times" (Phase 1).
     * <p>
     * Validates date + party size, refreshes identity (guest/subscriber/override),
     * then sends {@link ReservationRequestType#FIRST_PHASE} to the server.
     */
    @FXML
    private void onFindTimes() {
        clampPartySize();

        Integer partySize = partySizeSpinner == null ? null : partySizeSpinner.getValue();
        LocalDate date = datePicker == null ? null : datePicker.getValue();

        if (partySize == null || partySize < MIN_PARTY) { setInfo("Party size must be at least 1."); return; }
        if (partySize > MAX_PARTY) { setInfo("Party size cannot exceed 20."); return; }
        if (date == null) { setInfo("Please choose a date."); return; }
        if (date.isBefore(LocalDate.now())) { setInfo("Date cannot be in the past."); return; }

        currentPartySize = partySize;
        currentDate = date;

        if (clientController == null) { setInfo("ClientController not set."); return; }
        refreshIdentityFromController();
        setInfo("Fetching available times...");

        String userId = guestMode ? null : this.userID;
        String guestContactLocal = guestMode ? this.guestContact : null;
        if (guestMode && (guestContactLocal == null || guestContactLocal.isBlank())) {
            setInfo("Guest contact is required to create a reservation.");
            return;
        }
        if (!guestMode && (userId == null || userId.isBlank())) {
            setInfo("Subscriber ID is missing. Please log in again.");
            return;
        }

        clientController.requestNewReservation(
                ReservationRequestType.FIRST_PHASE,
                date,
                null,
                Integer.valueOf(partySize),
                userId,
                guestContactLocal,
                0
        );
        System.out.println("Sent FIRST_PHASE request to server");
        System.out.println("Date: " + date.toString() + ", Party size: " + partySize);
        System.out.println("Guest mode: " + guestMode + ", UserID: " + userId + ", Guest contact: " + guestContactLocal);

    }

    /**
     * Button handler: returns from step 2 (slots view) back to step 1 (form).
     */
    @FXML
    private void onBackToForm() {
        switchToStep1();
        setInfo("Pick party size + date, then choose a time block.");
    }

    /**
     * Handles reservation-related server responses routed by the desktop shell.
     * <p>
     * Expected response types include:
     * <ul>
     *   <li>{@code FIRST_PHASE_SHOW_AVAILABILITY}: show available times for a date.</li>
     *   <li>{@code FIRST_PHASE_SHOW_SUGGESTIONS}: show alternative date/time suggestions.</li>
     *   <li>{@code FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS}: fully booked.</li>
     *   <li>{@code SECOND_PHASE_CONFIRMED}: reservation successfully created.</li>
     * </ul>
     *
     * @param resp the reservation response received from the server; ignored if {@code null}
     */
    public void handleServerResponse(ReservationResponse resp) {
        if (resp == null) return;

        if (slotsContainer != null) slotsContainer.getChildren().clear();

        switch (resp.getType()) {
        case FIRST_PHASE_SHOW_AVAILABILITY -> {
            LocalDate effectiveDate = resp.getNewDate() != null ? resp.getNewDate() : currentDate;

            if (effectiveDate == null) {
                setInfo("No date provided for availability. Please select a date and try again.");
                switchToStep1();
                return;
            }

            currentDate = effectiveDate;

            setInfo("Select a time for " + effectiveDate);

            addSectionHeader("Available times");
            buildTimeSlots(effectiveDate, resp.getAvailableTimes());

            if (resp.getSuggestedDates() != null && !resp.getSuggestedDates().isEmpty()) {
                addSectionHeader("Or choose a different date:");
                buildSuggestions(resp.getSuggestedDates());
            }

            switchToStep2();
        }

            case FIRST_PHASE_SHOW_SUGGESTIONS -> {
                setInfo("No exact matches found.");
                addSectionHeader("Available dates in the coming week:");
                buildSuggestions(resp.getSuggestedDates());
                switchToStep2();
            }

            case SECOND_PHASE_CONFIRMED -> {
                setInfo("Reservation confirmed. Code: " + resp.getConfirmationCode());

                if (slotsContainer != null) {
                    slotsContainer.getChildren().clear();
                }
                switchToStep1();
            }

            case FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS -> {
                setInfo("Fully booked for the next 7 days");
                switchToStep1();
            }
        }
    }

    /**
     * Adds a styled section header label to {@link #slotsContainer}.
     *
     * @param text header text to display
     */
    private void addSectionHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("h3");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");
        label.setMaxWidth(Double.MAX_VALUE);
        if (slotsContainer != null) slotsContainer.getChildren().add(label);
    }

    /**
     * Builds and renders a grid of available time slots for a single date.
     *
     * @param date  date associated with all times in {@code times}
     * @param times list of available times to render; may be {@code null}
     */
    private void buildTimeSlots(LocalDate date, List<LocalTime> times) {
        TilePane grid = createNewGrid();

        ReservationSlotsUI ui = new ReservationSlotsUI(grid, slotInfoLabel, SLOT_WIDTH, SLOT_HEIGHT);
        ui.renderAvailability(
                date,
                times,
                null,
                (d, t) -> sendSecondPhaseRequest(d, t)
        );

        if (slotsContainer != null) slotsContainer.getChildren().add(grid);
    }

    /**
     * Builds and renders a grid of suggested alternative date/time slots.
     *
     * @param suggestions map of dates to suggested times; may be {@code null}
     */
    private void buildSuggestions(Map<LocalDate, List<LocalTime>> suggestions) {
        TilePane grid = createNewGrid();

        ReservationSlotsUI ui = new ReservationSlotsUI(grid, slotInfoLabel, SLOT_WIDTH, SLOT_HEIGHT);
        ui.renderSuggestions(
                suggestions,
                null,
                (d, t) -> sendSecondPhaseRequest(d, t)
        );

        if (slotsContainer != null) slotsContainer.getChildren().add(grid);
    }

    /**
     * Creates a new {@link TilePane} grid configured for rendering slot buttons.
     *
     * @return a configured tile pane instance
     */
    private TilePane createNewGrid() {
        TilePane tp = new TilePane();
        tp.setHgap(10);
        tp.setVgap(10);
        tp.setPrefColumns(6);
        tp.setTileAlignment(Pos.CENTER_LEFT);
        return tp;
    }

    /**
     * Sends the phase-2 reservation confirmation request for a selected (date,time) slot.
     * <p>
     * Refreshes identity (guest/subscriber/override) and sends {@link ReservationRequestType#SECOND_PHASE}.
     *
     * @param date selected reservation date
     * @param time selected reservation time
     */
    private void sendSecondPhaseRequest(LocalDate date, LocalTime time) {
        setInfo("Confirming reservation for " + date + " at " + time + "...");
        refreshIdentityFromController();
        String userId = guestMode ? null : this.userID;
        String guestContactLocal = guestMode ? this.guestContact : null;
        if (guestMode && (guestContactLocal == null || guestContactLocal.isBlank())) {
            setInfo("Guest contact is required to confirm a reservation.");
            return;
        }
        if (!guestMode && (userId == null || userId.isBlank())) {
            setInfo("Subscriber ID is missing. Please log in again.");
            return;
        }

        clientController.requestNewReservation(
                ReservationRequestType.SECOND_PHASE,
                date,
                time,
                currentPartySize,
                userId,
                guestContactLocal,
                0
        );
    }

    /**
     * Refreshes identity fields from the {@link #clientController} session.
     * <p>
     * Precedence:
     * <ol>
     *   <li>If override identity is enabled, uses {@link #overrideUserId} / {@link #overrideGuestContact}.</li>
     *   <li>If the current session is a guest session, uses guest contact.</li>
     *   <li>Otherwise, uses subscriber session identifiers.</li>
     * </ol>
     */
    private void refreshIdentityFromController() {
        if (clientController == null) {
            return;
        }
        if (overrideIdentity) {
            this.userID = overrideUserId;
            this.guestContact = overrideGuestContact;
            this.guestMode = overrideGuestContact != null && !overrideGuestContact.isBlank();
            return;
        }

        if (clientController.isGuestSession()) {
            this.guestMode = true;
            this.userID = null;
            this.guestContact = clientController.getGuestContact();
            return;
        }

        String subId = clientController.getCurrentUserId();
        String username = clientController.getCurrentUsername();

        this.guestMode = false;
        this.userID = (subId != null && !subId.isBlank()) ? subId : username;
        this.guestContact = null;
    }

    /**
     * Enables manager override identity so reservations can be created for a specific customer.
     * <p>
     * If either {@code userId} or {@code guestContact} is provided (non-blank), override mode becomes active.
     * Override identity takes precedence over the current session identity when sending requests.
     *
     * @param userId       subscriber identifier to reserve for; may be {@code null} or blank
     * @param guestContact guest contact to reserve for; may be {@code null} or blank
     */
    public void setReservationIdentity(String userId, String guestContact) {
        this.overrideUserId = userId == null || userId.isBlank() ? null : userId.trim();
        this.overrideGuestContact = guestContact == null || guestContact.isBlank() ? null : guestContact.trim();
        this.overrideIdentity = this.overrideUserId != null || this.overrideGuestContact != null;
        refreshIdentityFromController();
        if (overrideIdentity) {
            String label = guestMode ? "guest" : "subscriber";
            String value = guestMode ? overrideGuestContact : overrideUserId;
            setInfo("Creating reservation for " + label + ": " + value);
        }
    }

    /**
     * Shows step 1 and hides step 2.
     */
    private void switchToStep1() {
        if (step1Pane != null) { step1Pane.setVisible(true); step1Pane.setManaged(true); }
        if (step2Pane != null) { step2Pane.setVisible(false); step2Pane.setManaged(false); }
    }

    /**
     * Shows step 2 and hides step 1.
     */
    private void switchToStep2() {
        if (step1Pane != null) { step1Pane.setVisible(false); step1Pane.setManaged(false); }
        if (step2Pane != null) { step2Pane.setVisible(true); step2Pane.setManaged(true); }
    }

    /**
     * Sets the informational text displayed to the user.
     *
     * @param msg message to display; may be {@code null}
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Clears any manager override identity and returns to using the current session identity.
     * <p>
     * If override is not active, this method does nothing.
     */
    public void clearReservationIdentity() {
        if (!overrideIdentity) {
            return;
        }
        this.overrideUserId = null;
        this.overrideGuestContact = null;
        this.overrideIdentity = false;
        refreshIdentityFromController();
        setInfo("Pick party size + date, then choose a time block.");
    }
}
