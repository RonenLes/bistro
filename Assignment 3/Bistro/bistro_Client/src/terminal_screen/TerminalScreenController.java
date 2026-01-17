package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

/**
 * JavaFX "terminal mode" shell controller for walk-in operations.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Manage toolbar navigation between terminal sub-views (check-in, waiting list, lost code, billing, etc.).</li>
 *   <li>Load FXML views on demand and cache both roots and controllers for reuse.</li>
 *   <li>Inject {@link ClientController} into child controllers that implement {@link ClientControllerAware}.</li>
 *   <li>Route asynchronous server responses to the currently active terminal view controller.</li>
 *   <li>Handle online/offline state: disable toolbar and show a message when the terminal is offline.</li>
 * </ul>
 * </p>
 */
public class TerminalScreenController {

    @FXML private StackPane contentHolder;

    // Toolbar buttons
    @FXML private Button checkInBtn;
    @FXML private Button joinWaitlistBtn;
    @FXML private Button lostCodeBtn;
    @FXML private Button payBillBtn;
    @FXML private Button cancelWaitlistBtn;

    private ClientController clientController;
    private boolean connected;

    /**
     * Callback that returns the UI to the main screen (provided by the UI navigator).
     */
    private Runnable onBackToMain;

    /**
     * Internal enum representing the available terminal sub-views.
     */
    private enum View {
        CHECK_IN, WAITING_LIST, CANCEL_WAITLIST, PAY_BILL, LOST_CODE
    }

    /**
     * Cache of loaded FXML roots by view (for faster navigation).
     */
    private final Map<View, Parent> cache = new EnumMap<>(View.class);

    /**
     * Cache of loaded controllers by view (for dependency injection and response routing).
     */
    private final Map<View, Object> controllerCache = new EnumMap<>(View.class);

    /**
     * The controller instance of the currently displayed terminal view.
     * Used to decide where to route server responses.
     */
    private Object currentContentController;

    /**
     * The currently displayed terminal sub-view.
     */
    private View currentView;

    /**
     * Injects the {@link ClientController} and sets the current connection state.
     * <p>
     * This method may be called after FXML injection. It updates the toolbar state and either shows an offline
     * message or loads a default view.
     * </p>
     *
     * @param controller the application-level client controller used for server communication
     * @param connected  {@code true} if connected to the server; {@code false} otherwise
     */
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;

        // FXML fields might already be injected at this point
        applyConnectionState();
    }

    /**
     * Sets a callback that returns the UI to the main screen.
     *
     * @param onBackToMain callback invoked when the "Back" action is triggered or when flows finish
     */
    public void setOnBackToMain(Runnable onBackToMain) {
        this.onBackToMain = onBackToMain;
    }

    /**
     * JavaFX lifecycle callback invoked after the FXML fields are injected.
     * Applies the initial connection state (offline by default until {@link #setClientController(ClientController, boolean)}).
     */
    @FXML
    private void initialize() {
        // At initialize time, connected is whatever default we have (false until setClientController)
        applyConnectionState();
    }

    /**
     * Toolbar handler: returns to the main screen via {@link #onBackToMain}.
     */
    @FXML
    private void onBackToMain() {
        if (onBackToMain != null) onBackToMain.run();
    }

    /**
     * Toolbar handler: navigates to the check-in view.
     */
    @FXML
    private void onCheckIn() {
        show(View.CHECK_IN);
    }

    /**
     * Toolbar handler: navigates to the join-waiting-list view.
     */
    @FXML
    private void onJoinWaitingList() {
        show(View.WAITING_LIST);
    }

    /**
     * Toolbar handler: navigates to the cancel-waitlist view.
     */
    @FXML
    private void onCancelWaitlist() {
        show(View.CANCEL_WAITLIST);
    }

    /**
     * Toolbar handler: navigates to the pay-bill view.
     */
    @FXML
    private void onPayBill() {
        show(View.PAY_BILL);
    }

    /**
     * Toolbar handler: navigates to the lost-code view.
     */
    @FXML
    private void onLostCode() {
        show(View.LOST_CODE);
    }

    /**
     * Enables/disables toolbar actions based on connection state and loads an appropriate default view.
     * <p>
     * If offline, disables terminal operations and shows an offline message.
     * If online and no current view is active, loads the default check-in view.
     * If online and a view is active, re-injects controller dependencies.
     * </p>
     */
    private void applyConnectionState() {
        boolean online = connected && clientController != null;

        // Guard against early calls before FXML injection
        if (checkInBtn != null) {
            checkInBtn.setDisable(!online);
        }
        if (joinWaitlistBtn != null) {
            joinWaitlistBtn.setDisable(!online);
        }
        if (lostCodeBtn != null) {
            lostCodeBtn.setDisable(!online);
        }
        if (payBillBtn != null) {
            payBillBtn.setDisable(!online);
        }
        if (cancelWaitlistBtn != null) {
            cancelWaitlistBtn.setDisable(!online);
        }

        if (contentHolder == null) {
            return;
        }

        if (!online) {
            // Show offline message in the content area
            Label offlineLabel = new Label("""
                    Terminal is offline.

                    The server / database is currently not available.
                    Please contact staff or try again in a few moments.
                    """);
            offlineLabel.getStyleClass().add("muted"); // optional: you already have this in your CSS
            contentHolder.getChildren().setAll(offlineLabel);

            currentView = null;
            currentContentController = null;
        } else {
            // When we become online, show default view
            if (currentView == null) {
                show(View.CHECK_IN);
            } else {
                // keep current view, but re-inject controller with updated connection state
                reinjectActiveController();
            }
        }
    }

    /**
     * Routes waiting list cancellation responses to the active view (if it is the cancel-waitlist screen).
     *
     * @param response waiting list response received from the server
     */
    public void onWaitingListCancellation(responses.WaitingListResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (currentContentController instanceof TerminalCancelWaitingListController cancelCtrl) {
                cancelCtrl.handleWaitingListResponse(response);
            }
        });
    }

    /**
     * Routes seating responses to the active view (check-in or waiting-list) and then returns to the main screen.
     *
     * @param response seating response received from the server
     */
    public void onSeatingResponse(responses.SeatingResponse response) {
        javafx.application.Platform.runLater(() -> {
            // Route server responses only to the currently displayed view
            if (currentContentController instanceof terminal_screen.TerminalCheckInController checkInCtrl) {
                checkInCtrl.handleSeatingResponse(response);
            } else if (currentContentController instanceof terminal_screen.TerminalWaitingListController waitingListController) {
                waitingListController.handleSeatingResponse(response);
            }
            // return to main after seating operation completes
            returnToMain();
        });
    }

    /**
     * Routes bill totals to the pay-bill view (if it is currently displayed).
     *
     * @param baseTotal bill total returned by the server
     */
    public void onBillTotal(double baseTotal) {
        javafx.application.Platform.runLater(() -> {
            if (currentContentController instanceof TerminalPayBillController payBillCtrl) {
                payBillCtrl.onBillTotalLoaded(baseTotal);
            }
        });
    }

    /**
     * Routes payment success to the pay-bill view and then returns to the main screen.
     */
    public void onBillPaid() {
        javafx.application.Platform.runLater(() -> {
            if (currentContentController instanceof TerminalPayBillController payBillCtrl) {
                payBillCtrl.onBillPaid();
            }
            // return to main after payment completes
            returnToMain();
        });
    }

    /**
     * Routes billing errors to the pay-bill view (if it is currently displayed).
     *
     * @param message error message to show
     */
    public void onBillError(String message) {
        javafx.application.Platform.runLater(() -> {
            if (currentContentController instanceof TerminalPayBillController payBillCtrl) {
                payBillCtrl.onBillingError(message);
            }
        });
    }

    /**
     * Navigates to the specified terminal view, loading it if needed and caching it for reuse.
     * <p>
     * This method also updates {@link #currentContentController} and {@link #currentView} so that
     * server responses can be routed to the correct active controller.
     * </p>
     *
     * @param view terminal sub-view to display
     */
    private void show(View view) {
        if (!isOnline()) {
            // safety - ignore navigation if offline
            return;
        }

        if (view == currentView && contentHolder != null && !contentHolder.getChildren().isEmpty()) {
            return;
        }

        // loads view on demand, reuses cached views
        Parent root = cache.computeIfAbsent(view, this::loadView);

        // keep current controller aligned with the shown view
        // this determines where server responses are routed
        currentContentController = controllerCache.get(view);
        currentView = view;

        if (contentHolder != null) {
            contentHolder.getChildren().setAll(root);
        }
    }

    /**
     * @return {@code true} if connected to the server and {@link #clientController} is available
     */
    private boolean isOnline() {
        return connected && clientController != null;
    }

    /**
     * Returns to the main screen via {@link #onBackToMain} callback.
     */
    private void returnToMain() {
        if (onBackToMain != null) {
            onBackToMain.run();
        }
    }

    /**
     * Re-injects the current {@link ClientController} into the active terminal view after reconnect.
     * <p>
     * This is useful when the terminal transitions from offline to online and cached controllers must
     * receive updated dependencies/state.
     * </p>
     */
    private void reinjectActiveController() {
        if (!isOnline()) return;
        if (currentView == null) return;

        Object ctrl = controllerCache.get(currentView);
        if (ctrl instanceof ClientControllerAware aware) {
            aware.setClientController(clientController, connected);
        }
    }

    /**
     * Loads an FXML view corresponding to the specified {@link View}, caches its controller,
     * and injects dependencies into it.
     *
     * @param view the terminal sub-view to load
     * @return the root node of the loaded FXML view
     * @throws RuntimeException if the FXML fails to load
     */
    private Parent loadView(View view) {
        try {
            String fxml = switch (view) {
                case CHECK_IN -> "/terminal_screen/TerminalCheckInView.fxml";
                case WAITING_LIST -> "/terminal_screen/TerminalWaitingListView.fxml";
                case CANCEL_WAITLIST -> "/terminal_screen/TerminalCancelWaitingList.fxml";
                case PAY_BILL -> "/terminal_screen/TerminalPayBillView.fxml";
                case LOST_CODE -> "/terminal_screen/TerminalLostCodeView.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object ctrl = loader.getController();

            // cache controller so we can route async server responses to the active page
            controllerCache.put(view, ctrl);

            // inject dependencies into controller
            if (ctrl instanceof ClientControllerAware aware) {
                aware.setClientController(clientController, connected);
            }

            return root;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + view, e);
        }
    }
}
