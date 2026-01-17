package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for a terminal screen that handles walk-in table requests.
 * <p>
 * The flow:
 * <ul>
 *   <li>User enters either a subscriber user ID or a guest contact (phone/email).</li>
 *   <li>User selects party size (1-20) using a {@link Spinner}.</li>
 *   <li>The request is sent to the server, which attempts to seat immediately or adds the customer to the waiting list.</li>
 * </ul>
 * </p>
 * <p>
 * Server communication is performed via an injected {@link ClientController}. When offline, the controller displays
 * a demo message; however, the request method is still called (based on current implementation).
 * </p>
 */
public class TerminalWaitingListController implements ClientControllerAware {

    @FXML private TextField userIdField;
    @FXML private TextField guestContactField;
    @FXML private Spinner<Integer> partySizeField; // spinner
    @FXML private Label statusLabel;

    private ClientController clientController;
    private boolean connected;

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * <p>
     * Initializes the party size spinner to allow values from 1 to 20 (default 2)
     * and clears the status label.
     * </p>
     */
    @FXML
    private void initialize() {
        // set up party size spinner with range 1-20, default 2
        if (partySizeField != null) {
            partySizeField.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2)
            );
            partySizeField.setEditable(true); // optional: allow typing
        }
        setStatus("");
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
     * UI handler that submits a walk-in seating request to the server.
     * <p>
     * Validation rules:
     * <ul>
     *   <li>Either subscriber user ID or guest contact must be provided.</li>
     *   <li>Party size must be in range 1-20.</li>
     * </ul>
     * </p>
     * <p>
     * The server is expected to either:
     * <ul>
     *   <li>Seat the customer immediately (if a table is available), or</li>
     *   <li>Add the customer to the waiting list (if no table is available).</li>
     * </ul>
     * </p>
     */
    @FXML
    private void onJoin() {
        String userId = userIdField == null ? "" : userIdField.getText().trim();
        String guestContact = guestContactField == null ? "" : guestContactField.getText().trim();

        if (userId.isEmpty() && guestContact.isEmpty()) {
            setStatus("Enter a subscriber ID or guest contact.");
            return;
        }

        Integer sizeObj = (partySizeField == null) ? null : partySizeField.getValue();
        int size = (sizeObj == null) ? -1 : sizeObj;

        if (size < 1 || size > 20) {
            setStatus("Party size must be 1-20.");
            return;
        }

        if (!connected || clientController == null) {
            String contactLabel = userId.isEmpty() ? guestContact : userId;
            setStatus("Demo: added " + contactLabel + " (party of " + size + ") to waiting list.");
        }

        // sends walk-in seating request to server
        clientController.requestWalkInSeating(userId, guestContact, size);
        setStatus("Requesting a table...");
    }

    /**
     * UI handler that clears all input fields and resets party size to the default value.
     */
    @FXML
    private void onClear() {
        if (userIdField != null) userIdField.clear();
        if (guestContactField != null) guestContactField.clear();
        if (partySizeField != null) partySizeField.getValueFactory().setValue(2);
        setStatus("");
    }

    /**
     * Updates the status label displayed on the screen.
     *
     * @param msg message to show; if {@code null}, an empty string is displayed
     */
    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg == null ? "" : msg);
    }

    /**
     * Handles a seating response returned from the server after a walk-in request.
     * <p>
     * Expected behaviors (based on {@code response.getType()}):
     * <ul>
     *   <li>{@code CUSTOMER_CHECKED_IN}: indicates seating succeeded; may include table number</li>
     *   <li>{@code CUSTOMER_IN_WAITINGLIST}: indicates the customer was added to the waiting list</li>
     * </ul>
     * </p>
     *
     * @param response seating response received from the server; may be {@code null}
     */
    public void handleSeatingResponse(responses.SeatingResponse response) {
        if (response == null) return;
        switch (response.getType()) {
            case CUSTOMER_CHECKED_IN -> {
                Integer table = response.getTableNumberl();
                setStatus("Table ready! You're seated" + (table != null ? " at table " + table + "." : "."));
            }
            case CUSTOMER_IN_WAITINGLIST -> setStatus("You're on the take-a-seat list. We'll notify you soon.");
        }
    }
}
