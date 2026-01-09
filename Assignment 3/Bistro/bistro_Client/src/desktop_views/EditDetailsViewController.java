package desktop_views;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import requests.ReservationRequest.ReservationRequestType;
import responses.ReservationResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Edit reservation flow:
 * Step 1 confirmation code
 * Step 2 edit/cancel
 */
public class EditDetailsViewController implements ClientControllerAware {

    // Step panes
    @FXML private VBox step1Pane;
    @FXML private VBox step2Pane;

    // Step 1
    @FXML private TextField confirmationCodeField;
    @FXML private Button loadButton;
    @FXML private Label codeErrorLabel;
    @FXML private Label statusLabel;

    // Step 2 reservation summary + actions
    @FXML private Label reservationSummaryLabel;

    @FXML private DatePicker editDatePicker;
    @FXML private TextField editTimeField;
    @FXML private Spinner<Integer> editPartySizeSpinner;
    @FXML private Label editErrorLabel;

    @FXML private Label actionStatusLabel;

    private boolean cancelledLock;

    private ClientController clientController;
    private boolean connected;

    private boolean guestMode;
    private String guestContact;
    private String userId;

    private int currentConfirmationCode;

    private static final int MIN_PARTY = 1;
    private static final int MAX_PARTY = 20;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        initPartySpinner();
        switchToStep1();
        setStatus("");
    }

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

    private void initPartySpinner() {
        if (editPartySizeSpinner == null) return;
        editPartySizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(MIN_PARTY, MAX_PARTY, 2)
        );
        editPartySizeSpinner.setEditable(true);
    }

    @FXML
    private void onLoadReservation() {
        clearErrors();

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

        Integer code = parseConfirmationCode();
        if (code == null) return;

        currentConfirmationCode = code;
        setStatus("Loading reservation...", false);

        // Expect server to return ReservationResponseType.SHOW_RESERVATION
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

    @FXML
    private void onSubmitEdit() {
        clearErrors();

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

        LocalDate d = editDatePicker == null ? null : editDatePicker.getValue();
        if (d == null) {
            setEditError("Please choose a date.");
            return;
        }
        if (d.isBefore(LocalDate.now())) {
            setEditError("Date cannot be in the past.");
            return;
        }

        LocalTime t;
        try {
            String raw = editTimeField == null ? "" : editTimeField.getText();
            t = LocalTime.parse(raw.trim(), TIME_FMT);
        } catch (Exception e) {
            setEditError("Time must be in HH:mm format.");
            return;
        }

        int party = editPartySizeSpinner == null ? 0 : editPartySizeSpinner.getValue();
        if (party < MIN_PARTY || party > MAX_PARTY) {
            setEditError("Party size must be 1-20.");
            return;
        }

        setActionStatus("Submitting edit request...", false);

        clientController.requestNewReservation(
                ReservationRequestType.EDIT_RESERVATION,
                d,
                t,
                party,
                userId,
                guestContact,
                currentConfirmationCode
        );
    }

    @FXML
    private void onCancelReservation() {
        clearErrors();

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
                userId,
                guestContact,
                currentConfirmationCode
        );
    }

    @FXML
    private void onBackToCode() {
        switchToStep1();
        if (!cancelledLock) setStatus("", false);
    }

    @FXML
    private void onClearCode() {
        if (cancelledLock) {
            setStatus("Reservation was cancelled. Loading by confirmation code is disabled.", true);
            return;
        }

        if (confirmationCodeField != null) confirmationCodeField.setText("");
        clearErrors();
        setStatus("", false);
    }

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

    private void switchToStep1() {
        if (step1Pane != null) { step1Pane.setVisible(true); step1Pane.setManaged(true); }
        if (step2Pane != null) { step2Pane.setVisible(false); step2Pane.setManaged(false); }
    }

    private void switchToStep2() {
        if (step1Pane != null) { step1Pane.setVisible(false); step1Pane.setManaged(false); }
        if (step2Pane != null) { step2Pane.setVisible(true); step2Pane.setManaged(true); }
    }

    private void clearErrors() {
        if (codeErrorLabel != null) codeErrorLabel.setText("");
        if (editErrorLabel != null) editErrorLabel.setText("");
        if (actionStatusLabel != null) actionStatusLabel.setText("");
        if (statusLabel != null) statusLabel.setText("");
    }

    private void lockLoadingAfterCancel() {
        cancelledLock = true;

        if (confirmationCodeField != null) confirmationCodeField.setDisable(true);
        if (loadButton != null) loadButton.setDisable(true);

        setStatus("Reservation was cancelled. Loading by confirmation code is disabled.", true);
    }

    private void setCodeError(String msg) {
        if (codeErrorLabel != null) codeErrorLabel.setText(msg == null ? "" : msg);
    }

    private void setEditError(String msg) {
        if (editErrorLabel != null) editErrorLabel.setText(msg == null ? "" : msg);
    }

    private void setStatus(String msg, boolean error) {
        if (statusLabel == null) return;
        statusLabel.setText(msg == null ? "" : msg);
        statusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    private void setStatus(String msg) {
        setStatus(msg, false);
    }

    private void setActionStatus(String msg, boolean error) {
        if (actionStatusLabel == null) return;
        actionStatusLabel.setText(msg == null ? "" : msg);
        actionStatusLabel.setStyle(error
                ? "-fx-text-fill: #EF4444; -fx-font-weight: 800;"
                : "-fx-text-fill: #2E9B5F; -fx-font-weight: 800;");
    }

    /**
     * DesktopScreenController should forward ReservationResponse here too.
     */
    public void onReservationResponse(ReservationResponse resp) {
        if (resp == null) return;

        switch (resp.getType()) {
        case SHOW_RESERVATION -> {
            LocalDate d = resp.getNewDate();
            LocalTime t = resp.getNewTime();
            int party = resp.getNewPartySize();
            Integer code = resp.getConfirmationCode();

            String owner = (resp.getUserID() != null && !resp.getUserID().isBlank())
                    ? ("Subscriber: " + resp.getUserID())
                    : ("Guest: " + (resp.getNewGuestContact() == null ? "—" : resp.getNewGuestContact()));

            String dateStr = (d == null) ? "—" : d.toString();
            String timeStr = (t == null) ? "—" : t.format(TIME_FMT);
            String partyStr = (party > 0) ? String.valueOf(party) : "—";
            String codeStr = (code == null) ? String.valueOf(currentConfirmationCode) : String.valueOf(code);

            if (reservationSummaryLabel != null) {
                reservationSummaryLabel.setText(
                        "Code: " + codeStr + " | " + dateStr + " " + timeStr + " | Party: " + partyStr + " | " + owner
                );
            }

            // Best-effort prefill if server included these:
            try {
                if (editDatePicker != null && d != null) editDatePicker.setValue(d);
                if (editTimeField != null && t != null) editTimeField.setText(t.format(TIME_FMT));
                if (editPartySizeSpinner != null && party > 0) editPartySizeSpinner.getValueFactory().setValue(party);
            } catch (Exception ignored) { }

            setActionStatus("Reservation loaded.", false);
            switchToStep2();
        }

            case EDIT_RESERVATION -> {
                setActionStatus("Reservation updated successfully.", false);
            }

            case CANCEL_RESERVATION -> {
                setActionStatus("Reservation cancelled successfully.", false);
                switchToStep1();
                lockLoadingAfterCancel();
            }

            default -> {
                // If your server uses different types, add them here.
            }
        }
    }
}
