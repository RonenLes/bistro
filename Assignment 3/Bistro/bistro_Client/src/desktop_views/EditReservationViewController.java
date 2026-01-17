package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import requests.ReservationRequest.ReservationRequestType;
import responses.ReservationResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Controller for the "Edit Reservation" desktop flow.
 * <p>
 * Implements a two-step UI process:
 * <ol>
 *   <li>Load an existing reservation by confirmation code.</li>
 *   <li>Select new reservation details (date, party size, time) that must differ from the current reservation,
 *       and submit an {@link ReservationRequestType#EDIT_RESERVATION} request.</li>
 * </ol>
 * <p>
 * Also supports reservation cancellation via {@link ReservationRequestType#CANCEL_RESERVATION}.
 * <p>
 * Implements {@link ClientControllerAware} to receive a {@link ClientController} reference and connection status
 * from the parent desktop shell.
 */
public class EditReservationViewController implements ClientControllerAware {

    /** Step-1 UI container (confirmation code entry). */
    @FXML private VBox step1Pane;
    /** Step-2 UI container (choose new date/party/time). */
    @FXML private VBox step2Pane;

    /** Confirmation code input field (step 1). */
    @FXML private TextField confirmationCodeField;
    /** Validation/error label for confirmation code input (step 1). */
    @FXML private Label codeErrorLabel;
    /** General status label for step 1 (loading/errors/success). */
    @FXML private Label statusLabel;
    /** Button used to load reservation details by confirmation code. */
    @FXML private Button loadButton;

    /** Date picker for selecting a new reservation date (step 2). */
    @FXML private DatePicker datePicker;
    /** Spinner for selecting party size (step 2). */
    @FXML private Spinner<Integer> partySizeSpinner;
    /** Informational label for slot selection (step 2). */
    @FXML private Label slotInfoLabel;
    /** Container for clickable time slot UI elements (step 2). */
    @FXML private TilePane slotsTile;
    /** Status label for actions (edit/cancel) in step 2. */
    @FXML private Label actionStatusLabel;

    /** Label that shows the currently loaded (original) reservation summary. */
    @FXML private Label originalSummaryLabel;
    /** Label that shows the newly selected reservation summary. */
    @FXML private Label newSummaryLabel;
    /** Button used to confirm and submit the edit request (step 2). */
    @FXML private Button confirmNewDetailsButton;

    /** Helper component used to render and manage time slot selection UI. */
    private ReservationSlotsUI slotsUI;

    /** Reference to the client controller used for server communication (injected by parent shell). */
    private ClientController clientController;
    /** Indicates whether the client is connected to the server (injected by parent shell). */
    private boolean connected;

    /** Whether the current session is in guest mode (affects identity fields sent to server). */
    private boolean guestMode;
    /** Guest contact identifier used when {@code guestMode == true}. */
    private String guestContact;
    /** Logged-in user identifier used when {@code guestMode == false}. */
    private String userId;

    /** Confirmation code of the currently loaded reservation. */
    private int currentConfirmationCode;

    /** Date of the currently loaded reservation. */
    private LocalDate currentReservationDate;
    /** Start time of the currently loaded reservation. */
    private LocalTime currentReservationTime;
    /** Party size of the currently loaded reservation. */
    private int currentReservationParty;

    /** Newly selected date for edit submission. */
    private LocalDate selectedDate;
    /** Newly selected party size for edit submission. */
    private Integer selectedParty;

    /** Lock to prevent re-loading a reservation after it has been cancelled. */
    private boolean cancelledLock;

    /** Minimum allowed party size. */
    private static final int MIN_PARTY = 1;
    /** Maximum allowed party size. */
    private static final int MAX_PARTY = 20;

    /** Formatter used for displaying times as {@code HH:mm}. */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Preferred width for rendered time slot UI elements. */
    private static final double SLOT_WIDTH = 130.0;
    /** Preferred height for rendered time slot UI elements. */
    private static final double SLOT_HEIGHT = 55.0;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     * Sets up controls (party size spinner, date limits, slot UI helper) and resets the UI state
     * to step 1.
     */
    @FXML
    private void initialize() {
        initPartySizeSpinner();
        initDatePickerLimit();

        slotsUI = new ReservationSlotsUI(slotsTile, slotInfoLabel, SLOT_WIDTH, SLOT_HEIGHT);

        resetStep2State();
        switchToStep1();
        setStatus("");
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this view controller.
     * <p>
     * Also initializes identity fields based on whether the session is a guest session.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    @Override
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;

        if (controller != null) {
            this.guestMode = controller.isGuestSession();
            if (guestMode) {
                this.userId = null;
                this.guestContact = controller.getGuestContact();
            } else {
                this.userId = controller.getCurrentUsername();
                this.guestContact = null;
            }
        }
    }

    /**
     * Initializes the party size spinner (range, default value, and selection listeners).
     * <p>
     * Updates the new summary and confirmation enablement when party size changes.
     */
    private void initPartySizeSpinner() {
        if (partySizeSpinner == null) return;

        partySizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_PARTY, MAX_PARTY, 2)
        );
        partySizeSpinner.setEditable(true);

        partySizeSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            selectedParty = newV;
            if (slotsUI != null) slotsUI.clearSelection();
            updateNewSummary();
            updateConfirmEnabled();
        });
    }

    /**
     * Limits the date picker to a bounded range: from today through the next 6 days (inclusive).
     * <p>
     * Disables days outside the range and clamps user selection to the allowed interval.
     * Clears any existing slot selection when the date changes.
     */
    private void initDatePickerLimit() {
        if (datePicker == null) return;

        LocalDate today = LocalDate.now();
        LocalDate max = today.plusDays(6);

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) return;

                boolean disabled = item.isBefore(today) || item.isAfter(max);
                setDisable(disabled);
                setStyle(disabled ? "-fx-opacity: 0.45;" : "");
            }
        });

        datePicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;

            LocalDate clamped = newV;
            if (clamped.isBefore(today)) clamped = today;
            if (clamped.isAfter(max)) clamped = max;

            if (!clamped.equals(newV)) {
                datePicker.setValue(clamped);
                return;
            }

            selectedDate = clamped;
            if (slotsUI != null) slotsUI.clearSelection();
            if (slotsUI != null) slotsUI.clear();
            updateNewSummary();
            updateConfirmEnabled();
        });
    }

    /**
     * Button handler (step 1): parses the confirmation code from input and requests reservation details.
     */
    @FXML
    private void onLoadReservation() {
        loadReservationByCode(parseConfirmationCode());
    }

    /**
     * Fetches reservation details from the server using a confirmation code.
     * <p>
     * Can be called from the load button (step 1) or programmatically (e.g., from subscriber home).
     * Prevents loading if the reservation has already been cancelled (see {@code cancelledLock}).
     *
     * @param confirmationCode the confirmation code identifying the reservation to load;
     *                         if {@code null}, no request is sent
     */
    public void loadReservationByCode(Integer confirmationCode) {
        clearMessages();

        if (cancelledLock) {
            setStatus("Reservation was cancelled. Loading by confirmation code is disabled.", true);
            return;
        }
        if (clientController == null) {
            setStatus("ClientController not set.", true);
            return;
        }
        if (!connected) {
            setStatus("Not connected to server.", true);
            return;
        }
        if (confirmationCode == null) return;

        currentConfirmationCode = confirmationCode;
        setStatus("Loading reservation...", false);
        if (confirmationCodeField != null) {
            confirmationCodeField.setText(String.valueOf(confirmationCode));
        }

        clientController.requestNewReservation(
                ReservationRequestType.SHOW_RESERVATION,
                null,
                null,
                0,
                userId,
                guestContact,
                currentConfirmationCode
        );
    }

    /**
     * Button handler (step 2): validates selected date/party size and requests available times from the server.
     * <p>
     * Sends a {@link ReservationRequestType#FIRST_PHASE} request to retrieve availability for the selected date/party.
     */
    @FXML
    private void onFindTimes() {
        clearMessages();

        if (slotsUI != null) slotsUI.clear();
        if (slotsUI != null) slotsUI.clearSelection();

        if (clientController == null) {
            setSlotInfo("ClientController not set.");
            return;
        }
        if (!connected) {
            setSlotInfo("Not connected to server.");
            return;
        }
        if (currentConfirmationCode <= 0) {
            setSlotInfo("Missing confirmation code.");
            return;
        }

        LocalDate d = datePicker == null ? null : datePicker.getValue();
        Integer p = partySizeSpinner == null ? null : partySizeSpinner.getValue();

        if (d == null) {
            setSlotInfo("Please choose a date.");
            return;
        }
        if (p == null || p < MIN_PARTY || p > MAX_PARTY) {
            setSlotInfo("Party size must be 1-20.");
            return;
        }

        selectedDate = d;
        selectedParty = p;

        updateNewSummary();
        updateConfirmEnabled();

        setSlotInfo("Fetching available times...");

        clientController.requestNewReservation(
                ReservationRequestType.FIRST_PHASE,
                d,
                null,
                p,
                guestMode ? null : userId,
                guestMode ? guestContact : null,
                currentConfirmationCode
        );
    }

    /**
     * Button handler (step 2): validates the new selection and submits an edit request to the server.
     * <p>
     * Sends a {@link ReservationRequestType#EDIT_RESERVATION} request with the selected date/time/party.
     * Ensures the new details differ from the current reservation.
     */
    @FXML
    private void onConfirmChange() {
        clearMessages();

        if (clientController == null) {
            setActionStatus("ClientController not set.", true);
            return;
        }
        if (!connected) {
            setActionStatus("Not connected to server.", true);
            return;
        }
        if (currentConfirmationCode <= 0) {
            setActionStatus("Missing confirmation code.", true);
            return;
        }
        if (selectedDate == null) {
            setActionStatus("Please choose a date.", true);
            return;
        }
        if (selectedParty == null || selectedParty < MIN_PARTY || selectedParty > MAX_PARTY) {
            setActionStatus("Party size must be 1-20.", true);
            return;
        }
        if (slotsUI == null || !slotsUI.hasSelection()) {
            setActionStatus("Please select a time.", true);
            return;
        }

        LocalDate pickedDate = slotsUI.getSelectedDate();
        LocalTime pickedTime = slotsUI.getSelectedTime();

        if (pickedDate == null || pickedTime == null) {
            setActionStatus("Please select a time.", true);
            return;
        }

        if (!pickedDate.equals(selectedDate)) {
            setActionStatus("Please select a time for the chosen date.", true);
            return;
        }

        if (isSameAsCurrent(pickedDate, pickedTime, selectedParty)) {
            setActionStatus("New details must be different from the original.", true);
            return;
        }

        setActionStatus("Submitting edit request...", false);

        clientController.requestNewReservation(
                ReservationRequestType.EDIT_RESERVATION,
                pickedDate,
                pickedTime,
                selectedParty,
                guestMode ? null : userId,
                guestMode ? guestContact : null,
                currentConfirmationCode
        );
    }

    /**
     * Button handler: submits a cancel reservation request for the currently loaded confirmation code.
     * <p>
     * Sends a {@link ReservationRequestType#CANCEL_RESERVATION} request.
     */
    @FXML
    private void onCancelReservation() {
        clearMessages();

        if (clientController == null) {
            setActionStatus("ClientController not set.", true);
            return;
        }
        if (!connected) {
            setActionStatus("Not connected to server.", true);
            return;
        }
        if (currentConfirmationCode <= 0) {
            setActionStatus("Missing confirmation code.", true);
            return;
        }

        setActionStatus("Submitting cancel request...", false);

        clientController.requestNewReservation(
                ReservationRequestType.CANCEL_RESERVATION,
                null,
                null,
                0,
                guestMode ? null : userId,
                guestMode ? guestContact : null,
                currentConfirmationCode
        );
    }

    /**
     * Button handler: returns from step 2 back to step 1.
     * Clears status message unless loading is locked due to cancellation.
     */
    @FXML
    private void onBackToCode() {
        switchToStep1();
        if (!cancelledLock) setStatus("", false);
    }

    /**
     * Button handler: clears the confirmation code input field and resets messages.
     * <p>
     * If loading is locked due to cancellation, shows an error instead.
     */
    @FXML
    private void onClearCode() {
        if (cancelledLock) {
            setStatus("Reservation was cancelled. Loading by confirmation code is disabled.", true);
            return;
        }
        if (confirmationCodeField != null) confirmationCodeField.setText("");
        clearMessages();
        setStatus("", false);
    }

    /**
     * Parses and validates the confirmation code from {@code confirmationCodeField}.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Required (non-empty)</li>
     *   <li>Must be 4–10 digits</li>
     *   <li>Must parse to an {@link Integer}</li>
     * </ul>
     *
     * @return the parsed confirmation code, or {@code null} if validation fails
     */
    private Integer parseConfirmationCode() {
        String raw = confirmationCodeField == null ? "" : confirmationCodeField.getText();
        raw = raw == null ? "" : raw.trim();

        if (raw.isEmpty()) {
            setCodeError("Confirmation code is required.");
            return null;
        }
        if (!raw.matches("\\d{4,10}")) {
            setCodeError("Confirmation code must be 4-10 digits.");
            return null;
        }

        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            setCodeError("Invalid confirmation code.");
            return null;
        }
    }

    /**
     * Resets all step-2 state (selected date/party/time UI, summaries, and action messages).
     */
    private void resetStep2State() {
        selectedDate = null;
        selectedParty = null;

        if (originalSummaryLabel != null) originalSummaryLabel.setText("-");
        if (newSummaryLabel != null) newSummaryLabel.setText("Select date, party, and time");

        if (confirmNewDetailsButton != null) confirmNewDetailsButton.setDisable(true);

        if (slotsUI != null) slotsUI.clear();
        if (slotInfoLabel != null) slotInfoLabel.setText("");
        if (actionStatusLabel != null) actionStatusLabel.setText("");
    }

    /**
     * Updates the "original reservation" summary label based on the currently loaded reservation details.
     */
    private void updateOriginalSummary() {
        if (originalSummaryLabel == null) return;

        if (currentReservationDate == null || currentReservationTime == null || currentReservationParty <= 0) {
            originalSummaryLabel.setText("-");
            return;
        }

        originalSummaryLabel.setText(
                currentReservationDate + " " + currentReservationTime.format(TIME_FMT) + " | party " + currentReservationParty
        );
    }

    /**
     * Updates the "new reservation" summary label based on current selections (date/party/time).
     * <p>
     * If a slot is selected in {@code slotsUI}, the summary includes the selected time; otherwise it
     * prompts the user for missing fields.
     */
    private void updateNewSummary() {
        if (newSummaryLabel == null) return;

        LocalDate d = selectedDate != null ? selectedDate : (datePicker == null ? null : datePicker.getValue());
        Integer p = selectedParty != null ? selectedParty : (partySizeSpinner == null ? null : partySizeSpinner.getValue());

        if (slotsUI != null && slotsUI.hasSelection() && slotsUI.getSelectedTime() != null && slotsUI.getSelectedDate() != null) {
            LocalDate sd = slotsUI.getSelectedDate();
            LocalTime st = slotsUI.getSelectedTime();
            newSummaryLabel.setText(sd + " " + st.format(TIME_FMT) + " | party " + (p == null ? "-" : p));
            return;
        }

        if (d == null && p == null) {
            newSummaryLabel.setText("Select date, party, and time");
            return;
        }

        if (d != null && p != null) {
            newSummaryLabel.setText(d + " | party " + p + " | select time");
            return;
        }

        if (d != null) {
            newSummaryLabel.setText(d + " | select party and time");
            return;
        }

        newSummaryLabel.setText("party " + p + " | select date and time");
    }

    /**
     * Enables/disables the confirm button based on whether:
     * <ul>
     *   <li>Date and party are selected</li>
     *   <li>A slot is selected</li>
     *   <li>The selected details are different from the current reservation</li>
     * </ul>
     */
    private void updateConfirmEnabled() {
        if (confirmNewDetailsButton == null) return;

        LocalDate d = selectedDate != null ? selectedDate : (datePicker == null ? null : datePicker.getValue());
        Integer p = selectedParty != null ? selectedParty : (partySizeSpinner == null ? null : partySizeSpinner.getValue());

        if (d == null || p == null) {
            confirmNewDetailsButton.setDisable(true);
            return;
        }

        if (slotsUI == null || !slotsUI.hasSelection()) {
            confirmNewDetailsButton.setDisable(true);
            return;
        }

        LocalDate sd = slotsUI.getSelectedDate();
        LocalTime st = slotsUI.getSelectedTime();

        if (sd == null || st == null) {
            confirmNewDetailsButton.setDisable(true);
            return;
        }

        boolean different = !isSameAsCurrent(sd, st, p);
        confirmNewDetailsButton.setDisable(!different);
    }

    /**
     * Checks whether the provided (date, time, party) matches the currently loaded reservation.
     *
     * @param d     date to compare
     * @param t     time to compare
     * @param party party size to compare
     * @return {@code true} if all three match the current reservation; otherwise {@code false}
     */
    private boolean isSameAsCurrent(LocalDate d, LocalTime t, int party) {
        if (d == null || t == null) return false;
        if (currentReservationDate == null || currentReservationTime == null || currentReservationParty <= 0) return false;
        return currentReservationDate.equals(d) && currentReservationTime.equals(t) && currentReservationParty == party;
    }

    /**
     * Determines whether a given slot should be excluded because it represents the current reservation slot.
     * <p>
     * Party size for comparison is taken from {@code selectedParty} if present; otherwise falls back to the current party.
     *
     * @param d slot date
     * @param t slot time
     * @return {@code true} if the slot matches the current reservation (and should be excluded); otherwise {@code false}
     */
    private boolean excludeCurrentSlotOnly(LocalDate d, LocalTime t) {
        return isSameAsCurrent(d, t, selectedParty == null ? currentReservationParty : selectedParty);
    }

    /**
     * Slot selection callback used by {@link ReservationSlotsUI} when the user picks a slot.
     *
     * @param d selected date
     * @param t selected time
     */
    private void onSlotPicked(LocalDate d, LocalTime t) {
        updateNewSummary();
        updateConfirmEnabled();
        setActionStatus("", false);
    }

    /**
     * Clears all user-facing message labels (errors/status/info) across both steps.
     */
    private void clearMessages() {
        if (codeErrorLabel != null) codeErrorLabel.setText("");
        if (statusLabel != null) statusLabel.setText("");
        if (actionStatusLabel != null) actionStatusLabel.setText("");
        if (slotInfoLabel != null) slotInfoLabel.setText("");
    }

    /**
     * Switches the UI to step 1 (confirmation code entry).
     */
    private void switchToStep1() {
        if (step1Pane != null) { step1Pane.setVisible(true); step1Pane.setManaged(true); }
        if (step2Pane != null) { step2Pane.setVisible(false); step2Pane.setManaged(false); }
    }

    /**
     * Switches the UI to step 2 (select new date/party/time).
     */
    private void switchToStep2() {
        if (step1Pane != null) { step1Pane.setVisible(false); step1Pane.setManaged(false); }
        if (step2Pane != null) { step2Pane.setVisible(true); step2Pane.setManaged(true); }
    }

    /**
     * Displays a validation error related to the confirmation code input.
     *
     * @param msg error message to display (may be {@code null})
     */
    private void setCodeError(String msg) {
        if (codeErrorLabel != null) codeErrorLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Sets the step-1 status label text and color formatting.
     *
     * @param msg   status message to display
     * @param error whether to render the message as an error
     */
    private void setStatus(String msg, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(msg == null ? "" : msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    /**
     * Convenience overload for setting a non-error status message.
     *
     * @param msg status message to display
     */
    private void setStatus(String msg) {
        setStatus(msg, false);
    }

    /**
     * Sets the step-2 action status label text and color formatting.
     *
     * @param msg   status message to display
     * @param error whether to render the message as an error
     */
    private void setActionStatus(String msg, boolean error) {
        if (actionStatusLabel == null) return;
        actionStatusLabel.setText(msg == null ? "" : msg);
        actionStatusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    /**
     * Sets the slot information label text (step 2).
     *
     * @param msg the message to display
     */
    private void setSlotInfo(String msg) {
        if (slotInfoLabel != null) slotInfoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Locks the UI so reservation loading by confirmation code is disabled after a successful cancellation.
     * <p>
     * Disables confirmation code input and load button, and shows a status message.
     */
    private void lockLoadingAfterCancel() {
        cancelledLock = true;

        if (confirmationCodeField != null) confirmationCodeField.setDisable(true);
        if (loadButton != null) loadButton.setDisable(true);

        setStatus("Reservation was cancelled. Loading by confirmation code is disabled.", true);
    }

    /**
     * Handles reservation-related responses forwarded by the desktop shell.
     * <p>
     * Expected to be invoked by {@code DesktopScreenController} after receiving a {@link ReservationResponse}.
     * Updates the UI according to {@link ReservationResponse#getType()}:
     * <ul>
     *   <li>{@code SHOW_RESERVATION}: loads existing details and advances to step 2</li>
     *   <li>{@code FIRST_PHASE_*}: renders time availability/suggestions</li>
     *   <li>{@code EDIT_RESERVATION}: shows success, resets flow</li>
     *   <li>{@code CANCEL_RESERVATION}: shows success and locks further loading</li>
     * </ul>
     *
     * @param resp the server response payload; if {@code null}, no action is taken
     */
    public void onReservationResponse(ReservationResponse resp) {
        if (resp == null) return;

        switch (resp.getType()) {
            case SHOW_RESERVATION -> {
                currentReservationDate = resp.getNewDate();
                currentReservationTime = resp.getNewTime();
                currentReservationParty = resp.getNewPartySize();

                Integer code = resp.getConfirmationCode();
                if (code != null) currentConfirmationCode = code;

                resetStep2State();
                updateOriginalSummary();

                try {
                    if (datePicker != null && currentReservationDate != null) datePicker.setValue(currentReservationDate);
                    if (partySizeSpinner != null && currentReservationParty > 0) {
                        partySizeSpinner.getValueFactory().setValue(currentReservationParty);
                    }
                } catch (Exception ignored) { }

                selectedDate = datePicker == null ? currentReservationDate : datePicker.getValue();
                selectedParty = partySizeSpinner == null ? currentReservationParty : partySizeSpinner.getValue();

                updateNewSummary();
                updateConfirmEnabled();

                switchToStep2();
                setSlotInfo("Pick a time, then confirm.");

                onFindTimes();
            }

            case FIRST_PHASE_SHOW_AVAILABILITY -> {
                if (slotsUI != null) slotsUI.clear();

                if (slotsUI != null) {
                    slotsUI.info("Select a time.");
                    slotsUI.renderAvailabilitySelectable(
                            selectedDate,
                            resp.getAvailableTimes(),
                            (d, t) -> excludeCurrentSlotOnly(d, t),
                            (d, t) -> onSlotPicked(d, t)
                    );
                }

                Map<LocalDate, java.util.List<LocalTime>> sug = resp.getSuggestedDates();
                if (sug != null && !sug.isEmpty() && slotsUI != null) {
                    slotsUI.renderSuggestionsSelectable(
                            sug,
                            (d, t) -> excludeCurrentSlotOnly(d, t),
                            (d, t) -> onSlotPicked(d, t)
                    );
                }
            }

            case FIRST_PHASE_SHOW_SUGGESTIONS -> {
                if (slotsUI != null) slotsUI.clear();

                if (slotsUI != null) {
                    slotsUI.info("No exact match found. Showing alternatives.");
                    slotsUI.renderSuggestionsSelectable(
                            resp.getSuggestedDates(),
                            (d, t) -> excludeCurrentSlotOnly(d, t),
                            (d, t) -> onSlotPicked(d, t)
                    );
                }
            }

            case FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS -> {
                if (slotsUI != null) slotsUI.clear();
                setSlotInfo("No available times for the selected date.");
            }

            case EDIT_RESERVATION -> {
                currentReservationDate = resp.getNewDate() != null ? resp.getNewDate() : currentReservationDate;
                currentReservationTime = resp.getNewTime() != null ? resp.getNewTime() : currentReservationTime;
                currentReservationParty = resp.getNewPartySize() > 0 ? resp.getNewPartySize() : currentReservationParty;

                switchToStep1();
                if (confirmationCodeField != null) confirmationCodeField.setText("");
                setStatus("Reservation updated successfully.", false);

                resetStep2State();
                currentConfirmationCode = 0;
            }

            case CANCEL_RESERVATION -> {
                setActionStatus("Reservation cancelled successfully.", false);
                switchToStep1();
                lockLoadingAfterCancel();
            }

            default -> { }
        }
    }
}
