package desktop_views;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Shared UI helper for rendering reservation slot buttons (availability and suggestions).
 * <p>
 * This class is used by both {@code ReservationsViewController} and {@code EditReservationViewController}
 * to build time-slot tiles into a {@link TilePane}. It supports:
 * <ul>
 *   <li>Non-selectable mode: clicking a slot immediately triggers an action callback.</li>
 *   <li>Selectable mode: clicking a slot updates an internal selection state and adds a CSS marker class.</li>
 * </ul>
 * <p>
 * Selection behavior (selectable mode):
 * <ul>
 *   <li>Tracks {@link #selectedDate} and {@link #selectedTime}.</li>
 *   <li>Adds/removes CSS class {@code "slot-selected"} on the currently selected {@link Button}.</li>
 *   <li>Still calls {@code onPick} whenever a slot is clicked.</li>
 * </ul>
 */
public final class ReservationSlotsUI {

    /** Time formatter used for slot labels (HH:mm). */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    /** Short date formatter used for slot labels (dd/MM). */
    private static final DateTimeFormatter DATE_SHORT = DateTimeFormatter.ofPattern("dd/MM");

    /** Container where slot buttons are rendered. */
    private final TilePane slotsTile;
    /** Label used to display informational messages near the slot area. */
    private final Label slotInfoLabel;

    /** Preferred slot button width. */
    private final double slotWidth;
    /** Preferred slot button height. */
    private final double slotHeight;

    /** Currently selected slot button (selectable mode). */
    private Button selectedButton;
    /** Currently selected slot date (selectable mode). */
    private LocalDate selectedDate;
    /** Currently selected slot time (selectable mode). */
    private LocalTime selectedTime;

    /**
     * Creates a new slot UI helper.
     *
     * @param slotsTile     tile container where slot buttons should be rendered
     * @param slotInfoLabel label used to show informational text to the user
     * @param slotWidth     preferred width for each slot button
     * @param slotHeight    preferred height for each slot button
     */
    public ReservationSlotsUI(TilePane slotsTile, Label slotInfoLabel, double slotWidth, double slotHeight) {
        this.slotsTile = slotsTile;
        this.slotInfoLabel = slotInfoLabel;
        this.slotWidth = slotWidth;
        this.slotHeight = slotHeight;
    }

    /**
     * Clears all rendered slot buttons and resets informational text and selection state.
     */
    public void clear() {
        if (slotsTile != null) slotsTile.getChildren().clear();
        if (slotInfoLabel != null) slotInfoLabel.setText("");
        clearSelection();
    }

    /**
     * Displays an informational message to the user.
     *
     * @param msg message to display; if {@code null}, an empty string is shown
     */
    public void info(String msg) {
        if (slotInfoLabel != null) slotInfoLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Clears the current selection (selectable mode) and removes the {@code "slot-selected"} CSS class
     * from the previously selected button.
     */
    public void clearSelection() {
        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("slot-selected");
        }
        selectedButton = null;
        selectedDate = null;
        selectedTime = null;
    }

    /**
     * Returns the currently selected date (selectable mode).
     *
     * @return selected date, or {@code null} if nothing is selected
     */
    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    /**
     * Returns the currently selected time (selectable mode).
     *
     * @return selected time, or {@code null} if nothing is selected
     */
    public LocalTime getSelectedTime() {
        return selectedTime;
    }

    /**
     * Indicates whether a slot is currently selected (selectable mode).
     *
     * @return {@code true} if both selected date and time are set; otherwise {@code false}
     */
    public boolean hasSelection() {
        return selectedDate != null && selectedTime != null;
    }

    /**
     * Renders available time slots for a specific date (non-selectable variant).
     * <p>
     * Each rendered button triggers {@code onPick} when clicked.
     * Slots can be filtered out using {@code exclude}.
     *
     * @param selectedDate   date to associate with all provided {@code availableTimes}
     * @param availableTimes list of available times to display; may be {@code null}
     * @param exclude        predicate to skip rendering a specific (date,time) slot; may be {@code null}
     * @param onPick         callback invoked when the user clicks a slot; may be {@code null}
     */
    public void renderAvailability(
            LocalDate selectedDate,
            List<LocalTime> availableTimes,
            BiPredicate<LocalDate, LocalTime> exclude,
            BiConsumer<LocalDate, LocalTime> onPick
    ) {
        if (slotsTile == null) return;

        slotsTile.getChildren().clear();
        clearSelection();

        if (selectedDate == null) {
            info("Select a date to see available times.");
            return;
        }

        if (slotsTile == null) return;

        boolean addedAny = false;

        if (availableTimes != null) {
            for (LocalTime t : availableTimes) {
                if (exclude != null && exclude.test(selectedDate, t)) continue;
                slotsTile.getChildren().add(createSlotButton(selectedDate, t, "ghost", onPick, false));
                addedAny = true;
            }
        }

        if (!addedAny) info("No available times for this date.");
    }

    /**
     * Renders suggested alternative slots across multiple dates (non-selectable variant).
     * <p>
     * Suggested dates are rendered in sorted date order. Each slot triggers {@code onPick} when clicked.
     *
     * @param suggestedDates map of suggested dates to their available times; may be {@code null} or empty
     * @param exclude        predicate to skip rendering a specific (date,time) slot; may be {@code null}
     * @param onPick         callback invoked when the user clicks a slot; may be {@code null}
     */
    public void renderSuggestions(
            Map<LocalDate, List<LocalTime>> suggestedDates,
            BiPredicate<LocalDate, LocalTime> exclude,
            BiConsumer<LocalDate, LocalTime> onPick
    ) {
        if (slotsTile == null) return;

        AtomicBoolean addedAny = new AtomicBoolean(false);

        if (suggestedDates != null && !suggestedDates.isEmpty()) {
            suggestedDates.keySet().stream().sorted().forEach(d -> {
                List<LocalTime> times = suggestedDates.get(d);
                if (times == null) return;

                for (LocalTime t : times) {
                    if (exclude != null && exclude.test(d, t)) continue;
                    slotsTile.getChildren().add(createSlotButton(d, t, "primary", onPick, false));
                    addedAny.set(true);
                }
            });
        }

        if (!addedAny.get()) info("No alternative suggestions available.");
    }

    /**
     * Renders available time slots for a specific date (selectable variant).
     * <p>
     * In selectable mode, clicking a slot:
     * <ul>
     *   <li>Updates internal selection state ({@link #selectedDate}, {@link #selectedTime}).</li>
     *   <li>Adds CSS class {@code "slot-selected"} to the chosen button and removes it from the previous one.</li>
     *   <li>Invokes {@code onPick} callback.</li>
     * </ul>
     *
     * @param selectedDate   date to associate with all provided {@code availableTimes}
     * @param availableTimes list of available times to display; may be {@code null}
     * @param exclude        predicate to skip rendering a specific (date,time) slot; may be {@code null}
     * @param onPick         callback invoked when the user selects a slot; may be {@code null}
     */
    public void renderAvailabilitySelectable(
            LocalDate selectedDate,
            List<LocalTime> availableTimes,
            BiPredicate<LocalDate, LocalTime> exclude,
            BiConsumer<LocalDate, LocalTime> onPick
    ) {
        if (slotsTile == null) return;

        boolean addedAny = false;

        if (availableTimes != null) {
            for (LocalTime t : availableTimes) {
                if (exclude != null && exclude.test(selectedDate, t)) continue;
                slotsTile.getChildren().add(createSlotButton(selectedDate, t, "ghost", onPick, true));
                addedAny = true;
            }
        }

        if (!addedAny) info("No available times for this date.");
    }

    /**
     * Renders suggested alternative slots across multiple dates (selectable variant).
     * <p>
     * In selectable mode, clicking a slot updates the internal selection and applies the {@code "slot-selected"}
     * CSS class, then triggers {@code onPick}.
     *
     * @param suggestedDates map of suggested dates to their available times; may be {@code null} or empty
     * @param exclude        predicate to skip rendering a specific (date,time) slot; may be {@code null}
     * @param onPick         callback invoked when the user selects a slot; may be {@code null}
     */
    public void renderSuggestionsSelectable(
            Map<LocalDate, List<LocalTime>> suggestedDates,
            BiPredicate<LocalDate, LocalTime> exclude,
            BiConsumer<LocalDate, LocalTime> onPick
    ) {
        if (slotsTile == null) return;

        AtomicBoolean addedAny = new AtomicBoolean(false);

        if (suggestedDates != null && !suggestedDates.isEmpty()) {
            suggestedDates.keySet().stream().sorted().forEach(d -> {
                List<LocalTime> times = suggestedDates.get(d);
                if (times == null) return;

                for (LocalTime t : times) {
                    if (exclude != null && exclude.test(d, t)) continue;
                    slotsTile.getChildren().add(createSlotButton(d, t, "primary", onPick, true));
                    addedAny.set(true);
                }
            });
        }

        if (!addedAny.get()) info("No alternative suggestions available.");
    }

    /**
     * Creates a single slot button with consistent styling and click handling.
     *
     * @param date       slot date (may be {@code null} to display time only)
     * @param time       slot time (must not be {@code null} for meaningful display)
     * @param styleClass base style class to apply (e.g., "ghost" or "primary")
     * @param onPick     callback invoked when the button is clicked; may be {@code null}
     * @param selectable whether clicking the button should update internal selection state
     * @return a configured {@link Button} instance
     */
    private Button createSlotButton(
            LocalDate date,
            LocalTime time,
            String styleClass,
            BiConsumer<LocalDate, LocalTime> onPick,
            boolean selectable
    ) {
        String text = date == null
                ? time.format(TIME_FMT)
                : time.format(TIME_FMT) + "\n" + date.format(DATE_SHORT);
        Button btn = new Button(text);

        btn.getStyleClass().addAll(styleClass, "slot-btn");
        btn.setPrefSize(slotWidth, slotHeight);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-text-alignment: center; -fx-font-size: 11px;");

        btn.setOnAction(e -> {
            if (selectable) {
                applySelection(btn, date, time);
            }
            if (onPick != null) onPick.accept(date, time);
        });

        return btn;
    }

    /**
     * Applies selection styling and stores the selected date/time.
     * <p>
     * Removes {@code "slot-selected"} from the previously selected button, then applies it to {@code btn}.
     *
     * @param btn  the button that was selected
     * @param date the selected slot date
     * @param time the selected slot time
     */
    private void applySelection(Button btn, LocalDate date, LocalTime time) {
        if (btn == null) return;

        if (selectedButton != null) {
            selectedButton.getStyleClass().remove("slot-selected");
        }

        selectedButton = btn;
        selectedDate = date;
        selectedTime = time;

        if (!selectedButton.getStyleClass().contains("slot-selected")) {
            selectedButton.getStyleClass().add("slot-selected");
        }
    }
}
