package manager_screen;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.util.function.BiConsumer;

// first step of manager reservation flow
// allows manager to choose whether reservation is for subscriber or guest
// validates input and passes data to next step via callbacks
public class ManagerReservationStartScreenController {

    @FXML private RadioButton subscriberRadio;
    @FXML private RadioButton guestRadio;
    @FXML private ToggleGroup reservationTargetGroup;
    @FXML private TextField userIdField;
    @FXML private TextField guestContactField;
    @FXML private Label infoLabel;

    // callback to pass selected user/guest data to next step
    private BiConsumer<String, String> onContinue;
    // callback to cancel and return to previous screen
    private Runnable onCancel;

    @FXML
    // sets up radio button listener to toggle field visibility
    private void initialize() {
        if (reservationTargetGroup != null && subscriberRadio != null) {
            subscriberRadio.setSelected(true);
            reservationTargetGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> updateTargetFields());
            updateTargetFields();
        }
    }

    // sets callback for continue button
    public void setOnContinue(BiConsumer<String, String> onContinue) {
        this.onContinue = onContinue;
    }

    // sets callback for cancel button
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    @FXML
    // validates input and invokes callback with user ID or guest contact
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

        // pass selected data to next step
        if (onContinue != null) {
            onContinue.accept(isSubscriber ? userId : null, isGuest ? guestContact : null);
        }
    }

    @FXML
    // invokes cancel callback to return to previous screen
    private void onCancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    // shows appropriate field based on selected radio button
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

    // updates info label
    private void setInfo(String msg) {
        if (infoLabel != null) infoLabel.setText(msg == null ? "" : msg);
    }
}