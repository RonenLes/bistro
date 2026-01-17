package manager_screen;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.util.function.BiConsumer;

/**
 * JavaFX controller for the first step in the manager reservation flow.
 * <p>
 * Allows a manager to choose whether a reservation should be created for:
 * <ul>
 *   <li>a subscriber (by entering a subscriber/user ID), or</li>
 *   <li>a guest (by entering guest contact information).</li>
 * </ul>
 * <p>
 * This controller does not perform server requests. It validates the local input and forwards the
 * chosen identity to the next step using callbacks.
 */
public class ManagerReservationStartScreenController {

    /** Radio button for selecting "subscriber reservation". */
    @FXML private RadioButton subscriberRadio;

    /** Radio button for selecting "guest reservation". */
    @FXML private RadioButton guestRadio;

    /** Toggle group that enforces a single choice between subscriber/guest. */
    @FXML private ToggleGroup reservationTargetGroup;

    /** Input field for subscriber/user ID (enabled when subscriber is selected). */
    @FXML private TextField userIdField;

    /** Input field for guest contact (enabled when guest is selected). */
    @FXML private TextField guestContactField;

    /** Label used for validation feedback and informational messages. */
    @FXML private Label infoLabel;

    /**
     * Callback invoked when the user presses "Continue" and the input is valid.
     * <p>
     * The callback receives two values:
     * <ul>
     *   <li>subscriber user ID (non-null only when subscriber was selected)</li>
     *   <li>guest contact (non-null only when guest was selected)</li>
     * </ul>
     */
    private BiConsumer<String, String> onContinue;

    /**
     * Callback invoked when the user cancels this step (e.g., presses "Back" / "Cancel").
     * Provided by the parent controller to navigate away.
     */
    private Runnable onCancel;

    /**
     * Initializes the view after FXML injection.
     * <p>
     * Sets default selection to subscriber and wires a listener to toggle the input fields
     * based on the selected radio button.
     */
    @FXML
    private void initialize() {
        if (reservationTargetGroup != null && subscriberRadio != null) {
            subscriberRadio.setSelected(true);
            reservationTargetGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> updateTargetFields());
            updateTargetFields();
        }
    }

    /**
     * Sets the callback that is invoked when the user proceeds to the next step successfully.
     *
     * @param onContinue callback receiving (userId, guestContact). One of them will be {@code null}
     *                   depending on the selected reservation target.
     */
    public void setOnContinue(BiConsumer<String, String> onContinue) {
        this.onContinue = onContinue;
    }

    /**
     * Sets the callback that is invoked when the user cancels this step.
     *
     * @param onCancel callback to run (may be {@code null})
     */
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    /**
     * Button handler that validates input and forwards the chosen reservation target to the next step.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>If subscriber is selected: user ID must be provided.</li>
     *   <li>If guest is selected: guest contact must be provided.</li>
     * </ul>
     */
    @FXML
    private void onContinue() {
        boolean isSubscriber = subscriberRadio != null && subscriberRadio.isSelected();
        boolean isGuest = guestRadio != null && guestRadio.isSelected();

        if (!isSubscriber && !isGuest) {
            setInfo("Select subscriber or guest.");
            return;
        }

        String userId = userIdField == null ? null : userIdField.getText();
        String guestContact = guestContactField == null ? null : guestContactField.getText();
        userId = userId == null ? null : userId.trim();
        guestContact = guestContact == null ? null : guestContact.trim();

        if (isSubscriber && (userId == null || userId.isBlank())) {
            setInfo("Subscriber ID is required.");
            return;
        }
        if (isGuest && (guestContact == null || guestContact.isBlank())) {
            setInfo("Guest contact is required.");
            return;
        }

        if (onContinue != null) {
            onContinue.accept(isSubscriber ? userId : null, isGuest ? guestContact : null);
        }
    }

    /**
     * Button handler that cancels this step and returns control to the parent controller.
     */
    @FXML
    private void onCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    /**
     * Updates which input field is visible and enabled according to the selected reservation target.
     * <p>
     * When subscriber is selected: enables userIdField and hides guestContactField.
     * When guest is selected: enables guestContactField and hides userIdField.
     */
    private void updateTargetFields() {
        boolean isSubscriber = subscriberRadio != null && subscriberRadio.isSelected();

        if (userIdField != null) {
            userIdField.setDisable(!isSubscriber);
            userIdField.setManaged(isSubscriber);
            userIdField.setVisible(isSubscriber);
            if (!isSubscriber) {
                userIdField.clear();
            }
        }

        if (guestContactField != null) {
            guestContactField.setDisable(isSubscriber);
            guestContactField.setManaged(!isSubscriber);
            guestContactField.setVisible(!isSubscriber);
            if (isSubscriber) {
                guestContactField.clear();
            }
        }
    }

    /**
     * Updates the info label.
     *
     * @param msg message to display (null-safe)
     */
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}
