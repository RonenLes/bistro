package desktop_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import controllers.ClientUIHandler;
import desktop_views.EditReservationViewController;
import desktop_views.HistoryViewController;
import desktop_views.PayViewController;
import desktop_views.ReservationsViewController;
import desktop_views.TablesViewController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import manager_screen.AddSubscriberScreenController;
import manager_screen.EditTableScreenController;
import manager_screen.ManagerReservationsScreenController;
import manager_screen.ShowDataScreenController;
import manager_screen.UpdateOpeningHoursScreenController;
import manager_screen.SubscribersScreenController;
import manager_screen.ManagerReservationStartScreenController;
import subscriber_screen.*;
import responses.ManagerResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;
import responses.UserHistoryResponse;
import responses.WaitingListResponse;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import desktop_views.ReportsViewController;
import responses.ReportResponse;

/**
 * Desktop shell controller (top bar + left navigation + center content host).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Controls role-based navigation visibility.</li>
 *   <li>Ensures single selection across navigation buttons (via a {@link ToggleGroup}).</li>
 *   <li>Swaps child views into {@code contentHost} based on navigation selection.</li>
 *   <li>Routes server responses to the currently loaded child view controllers.</li>
 * </ul>
 * <p>
 * This controller is the main desktop UI container and implements {@link ClientUIHandler}.
 */
public class DesktopScreenController implements ClientUIHandler {

    /**
     * User roles that determine which screens are accessible in the desktop shell.
     */
    public enum Role {
        /** Guest (non-subscriber) user. */
        GUEST,
        /** Subscriber user. */
        SUBSCRIBER,
        /** Restaurant representative user. */
        REP,
        /** Manager user. */
        MANAGER
    }

    /** Main container where all child views are loaded (center content host). */
    @FXML private StackPane contentHost;

    /** Role group container for representative (layout only). */
    @FXML private VBox repGroup;
    /** Role group container for subscriber (layout only). */
    @FXML private VBox subscriberGroup;
    /** Role group container for manager (layout only). */
    @FXML private VBox managerGroup;

    /** Top-bar label used to display the welcome message / user name. */
    @FXML private Label welcomeNameLabel;

    /** Navigation button: "New Reservations". */
    @FXML private ToggleButton reservationsBtn;
    /** Navigation button: waiting list view. */
    @FXML private ToggleButton waitlistBtn;
    /** Navigation button: tables view. */
    @FXML private ToggleButton tablesBtn;

    /** Navigation button: history view. */
    @FXML private ToggleButton historyBtn;
    /** Navigation button: edit reservation details view. */
    @FXML private ToggleButton editReservationBtn;
    /** Navigation button: subscriber home view. */
    @FXML private ToggleButton subscriberHomeBtn;

    /** Navigation button: reports view. */
    @FXML private ToggleButton reportsBtn;
    /** Navigation button: analytics view. */
    @FXML private ToggleButton analyticsBtn;
    /** Navigation button: pay/billing view. */
    @FXML private ToggleButton payBtn;

    /** Navigation button: edit tables view (manager/rep). */
    @FXML private ToggleButton editTablesBtn;
    /** Navigation button: opening hours view (manager/rep). */
    @FXML private ToggleButton openingHoursBtn;
    /** Navigation button: manager reservations view. */
    @FXML private ToggleButton managerReservationsBtn;
    /** Navigation button: manager data view. */
    @FXML private ToggleButton managerDataBtn;
    /** Navigation button: add subscriber view. */
    @FXML private ToggleButton addSubscriberBtn;
    /** Navigation button: subscribers view. */
    @FXML private ToggleButton viewSubscribersBtn;

    /** Tracks server connection status for dependency injection into child screens. */
    private boolean connected;

    /** Ensures only one navigation button is selected at a time. */
    private final ToggleGroup navToggleGroup = new ToggleGroup();
    /** Maps each navigation button to its corresponding internal {@link ScreenKey}. */
    private final Map<ToggleButton, ScreenKey> navMap = new HashMap<>();

    /** Reference to application controller used for server communication. */
    private ClientController clientController;

    /** Cached child controller: reservations view. */
    private ReservationsViewController reservationsVC;
    /** Cached child controller: edit reservation view. */
    private EditReservationViewController editReservationVC;
    /** Cached child controller: history view. */
    private HistoryViewController historyVC;
    /** Cached child controller: pay/billing view. */
    private PayViewController payVC;

    /** Cached child controller: tables view. */
    private TablesViewController tablesVC;
    /** Cached child controller: edit table view. */
    private EditTableScreenController editTableVC;
    /** Cached child controller: update opening hours view. */
    private UpdateOpeningHoursScreenController openingHoursVC;
    /** Cached child controller: manager reservations view. */
    private ManagerReservationsScreenController managerReservationsVC;
    /** Cached child controller: show data view. */
    private ShowDataScreenController showDataVC;
    /** Cached child controller: add subscriber view. */
    private AddSubscriberScreenController addSubscriberVC;
    /** Cached child controller: subscribers view. */
    private SubscribersScreenController subscribersVC;
    /** Cached child controller: subscriber main (home) view. */
    private SubscriberMainScreenController subscriberMainVC;
    /** Cached child controller: reports view. */
    private ReportsViewController reportsVC;

    /** Current user role determining screen access; defaults to {@link Role#GUEST}. */
    private Role role = Role.GUEST;

    /** Callback to return to login screen / parent shell on logout. */
    private Runnable onLogout;

    /**
     * Internal keys identifying each available screen that can be loaded into {@code contentHost}.
     */
    private enum ScreenKey {
        /** Subscriber home screen. */
        SUBSCRIBER_HOME,
        /** Reservations screen. */
        RESERVATIONS,
        /** Waiting list screen. */
        WAITLIST,
        /** Tables screen. */
        TABLES,
        /** User history screen. */
        HISTORY,
        /** Edit reservation details screen. */
        EDIT_DETAILS,
        /** Add subscriber screen. */
        ADD_SUBSCRIBER,
        /** Subscribers screen. */
        SUBSCRIBERS,
        /** Reports screen. */
        REPORTS,
        /** Analytics screen. */
        ANALYTICS,
        /** Pay/billing screen. */
        PAY,
        /** Edit tables screen. */
        EDIT_TABLES,
        /** Manager reservations screen. */
        MANAGER_RESERVATIONS,
        /** Opening hours management screen. */
        OPENING_HOURS,
        /** Manager data screen. */
        MANAGER_DATA
    }

    /**
     * Defines which {@link ScreenKey}s each {@link Role} can access.
     */
    private static final Map<Role, Set<ScreenKey>> ROLE_SCREENS = new EnumMap<>(Role.class);
    static {
        ROLE_SCREENS.put(Role.GUEST, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY
        ));

        ROLE_SCREENS.put(Role.SUBSCRIBER, EnumSet.of(
                ScreenKey.SUBSCRIBER_HOME,
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY,
                ScreenKey.HISTORY
        ));

        ROLE_SCREENS.put(Role.REP, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.EDIT_TABLES,
                ScreenKey.MANAGER_DATA,
                ScreenKey.MANAGER_RESERVATIONS,
                ScreenKey.ADD_SUBSCRIBER,
                ScreenKey.SUBSCRIBERS,
                ScreenKey.OPENING_HOURS,
                ScreenKey.PAY
        ));

        ROLE_SCREENS.put(Role.MANAGER, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.ADD_SUBSCRIBER,
                ScreenKey.SUBSCRIBERS,
                ScreenKey.EDIT_TABLES,
                ScreenKey.MANAGER_DATA,
                ScreenKey.MANAGER_RESERVATIONS,
                ScreenKey.REPORTS,
                ScreenKey.OPENING_HOURS,
                ScreenKey.PAY
        ));
    }

    /**
     * Injects the {@link ClientController} reference and connection status into this desktop shell.
     * <p>
     * Typically called by the parent screen/controller during initialization.
     *
     * @param controller the client controller used to communicate with the server
     * @param connected  whether the client is currently connected to the server
     */
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    /**
     * Sets the current user role for this desktop shell.
     * <p>
     * Applies role-based navigation visibility rules, clears the current selection, and selects
     * a default screen for the role.
     *
     * @param role the new role; if {@code null}, defaults to {@link Role#GUEST}
     */
    public void setRole(Role role) {
        this.role = role != null ? role : Role.GUEST;
        applyRoleVisibility();
        navToggleGroup.selectToggle(null); // clear selection
        selectDefaultForRole(); // pick default screen for this role

        if (this.role == Role.SUBSCRIBER && clientController != null)  clientController.requestUpcomingReservations();
        if (historyVC != null) historyVC.setUserContext(this.role, buildDiscountInfo());
        if (addSubscriberVC != null) addSubscriberVC.setRequesterRole(this.role);
    }

    /**
     * Updates the top bar welcome message with the provided user name.
     *
     * @param name the display name to show; may be {@code null} or blank
     */
    public void setWelcomeName(String name) {
        if (welcomeNameLabel != null) {
            String trimmed = name == null ? "" : name.trim();

            if (role == Role.GUEST || trimmed.equalsIgnoreCase("guest"))  welcomeNameLabel.setText("Hello, guest.");
            else if (trimmed.isEmpty())  welcomeNameLabel.setText("Hello.");
            else welcomeNameLabel.setText("Hello, " + trimmed + ".");
        }
    }

    /**
     * Sets a logout callback for returning to the login screen or parent shell.
     *
     * @param onLogout a runnable to execute when logout is completed; may be {@code null}
     */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /**
     * Initializes navigation button mappings and listeners after FXML injection.
     * <p>
     * Called automatically by JavaFX once {@code @FXML} fields are injected.
     */
    @FXML
    private void initialize() {
        registerNavButton(reservationsBtn, ScreenKey.RESERVATIONS);
        registerNavButton(waitlistBtn,     ScreenKey.WAITLIST);
        registerNavButton(tablesBtn,       ScreenKey.TABLES);
        registerNavButton(historyBtn,      ScreenKey.HISTORY);
        registerNavButton(editReservationBtn,  ScreenKey.EDIT_DETAILS);
        registerNavButton(subscriberHomeBtn, ScreenKey.SUBSCRIBER_HOME);
        registerNavButton(reportsBtn,      ScreenKey.REPORTS);
        registerNavButton(analyticsBtn,    ScreenKey.ANALYTICS);
        registerNavButton(payBtn,          ScreenKey.PAY);
        registerNavButton(editTablesBtn,   ScreenKey.EDIT_TABLES);
        registerNavButton(openingHoursBtn, ScreenKey.OPENING_HOURS);
        registerNavButton(managerReservationsBtn, ScreenKey.MANAGER_RESERVATIONS);
        registerNavButton(managerDataBtn,  ScreenKey.MANAGER_DATA);
        registerNavButton(addSubscriberBtn, ScreenKey.ADD_SUBSCRIBER);
        registerNavButton(viewSubscribersBtn, ScreenKey.SUBSCRIBERS);

        applyRoleVisibility();

        navToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT instanceof ToggleButton btn) {
                ScreenKey key = navMap.get(btn);
                if (key != null) navigate(key);
            }
        });
    }

    /**
     * Registers a navigation toggle button and maps it to a {@link ScreenKey}.
     *
     * @param btn the navigation toggle button (may be {@code null})
     * @param key the screen key that should be loaded when the button is selected
     */
    private void registerNavButton(ToggleButton btn, ScreenKey key) {
        if (btn == null) return;
        btn.setToggleGroup(navToggleGroup);
        navMap.put(btn, key);
    }

    /**
     * Applies role-based visibility rules to all registered navigation buttons and
     * updates the visibility/managed state of the role groups.
     */
    private void applyRoleVisibility() {
        Set<ScreenKey> allowed = ROLE_SCREENS.getOrDefault(role, Set.of());

        for (Map.Entry<ToggleButton, ScreenKey> entry : navMap.entrySet()) {
            ToggleButton btn = entry.getKey();
            ScreenKey key = entry.getValue();
            if (btn == null) continue;

            boolean visible = allowed.contains(key);
            btn.setVisible(visible);
            btn.setManaged(visible);
        }

        updateGroupVisibility(repGroup);
        updateGroupVisibility(subscriberGroup);
        updateGroupVisibility(managerGroup);
    }

    /**
     * Hides a navigation group container if it has no visible/managed toggle buttons.
     *
     * @param group the group container to update (may be {@code null})
     */
    private void updateGroupVisibility(VBox group) {
        if (group == null) return;

        boolean hasVisibleChild = group.getChildren().stream()
                .filter(n -> n instanceof ToggleButton)
                .map(n -> (ToggleButton) n)
                .anyMatch(btn -> btn.isVisible() && btn.isManaged());

        group.setVisible(hasVisibleChild);
        group.setManaged(hasVisibleChild);
    }

    /**
     * Selects the default navigation button (and screen) for the current role.
     */
    private void selectDefaultForRole() {
        switch (role) {
            case MANAGER -> selectIfVisible(reportsBtn);
            case REP     -> selectIfVisible(reservationsBtn);
            case SUBSCRIBER -> selectIfVisible(subscriberHomeBtn);
            case GUEST   -> selectIfVisible(reservationsBtn);
            default      -> selectFirstAvailable();
        }
    }

    /**
     * Fallback selection logic: selects the first visible/managed navigation button
     * and navigates to its corresponding screen. Clears the content host if none exists.
     */
    private void selectFirstAvailable() {
        for (Map.Entry<ToggleButton, ScreenKey> entry : navMap.entrySet()) {
            ToggleButton btn = entry.getKey();
            if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
                btn.setSelected(true);
                navigate(entry.getValue());
                return;
            }
        }
        if (contentHost != null) contentHost.getChildren().clear();
    }

    /**
     * Selects a specific navigation button if it is visible/managed; otherwise selects the first available.
     *
     * @param btn the navigation button to select
     */
    private void selectIfVisible(ToggleButton btn) {
        if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
            btn.setSelected(true);
            navigate(navMap.get(btn));
        } else {
            selectFirstAvailable();
        }
    }

    /**
     * Handles navigation button click events from the sidebar.
     *
     * @param event the JavaFX action event triggered by the clicked navigation button
     */
    @FXML
    private void onNavClicked(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof ToggleButton btn)) return;
        ScreenKey key = navMap.get(btn);
        if (key != null) {
            btn.setSelected(true);
            navigate(key);
        }
    }

    /**
     * Handles logout click events and routes back to the login flow.
     * <p>
     * If {@code onLogout} is set, it will be executed after {@link ClientController#logout()} is called.
     * Otherwise, the desktop shell is reset to the guest role and clears its content.
     */
    @FXML
    private void onLogoutClicked() {

        if (clientController != null) clientController.logout();
        if (onLogout != null)  onLogout.run();
        else {
            setRole(Role.GUEST);
            setWelcomeName("");
            if (contentHost != null) contentHost.getChildren().clear();
        }
    }

    /**
     * Navigates to a specific screen by swapping the content host.
     *
     * @param key the screen key to navigate to
     */
    private void navigate(ScreenKey key) {
        switch (key) {
            case SUBSCRIBER_HOME ->loadIntoContentHost("/subscriber_screen/SubscriberMainScreen.fxml");
            case RESERVATIONS ->loadIntoContentHost("/desktop_views/ReservationsView.fxml");
            case WAITLIST ->loadIntoContentHost("/desktop_views/WaitlistView.fxml");
            case TABLES ->loadIntoContentHost("/desktop_views/TablesView.fxml");
            case EDIT_TABLES ->  loadIntoContentHost("/manager_screen/EditTableScreen.fxml");
            case MANAGER_DATA ->loadIntoContentHost("/manager_screen/ShowDataScreen.fxml");
            case ADD_SUBSCRIBER -> loadIntoContentHost("/manager_screen/AddSubscriberScreen.fxml");
            case SUBSCRIBERS ->loadIntoContentHost("/manager_screen/SubscribersScreen.fxml");
            case HISTORY ->loadIntoContentHost("/desktop_views/HistoryView.fxml");
            case EDIT_DETAILS ->loadIntoContentHost("/desktop_views/EditReservationView.fxml");
            case REPORTS ->loadIntoContentHost("/desktop_views/ReportsView.fxml");
            case ANALYTICS ->loadIntoContentHost("/desktop_views/AnalyticsView.fxml");
            case OPENING_HOURS ->loadIntoContentHost("/manager_screen/UpdateOpeningHoursScreen.fxml");
            case MANAGER_RESERVATIONS ->loadIntoContentHost("/manager_screen/ManagerReservationsScreen.fxml");
            case PAY ->loadIntoContentHost("/desktop_views/PayView.fxml");
        }
    }

    /**
     * Loads an FXML view into the content host and wires its controller.
     * <p>
     * This method:
     * <ul>
     *   <li>Loads the given FXML path using {@link FXMLLoader}.</li>
     *   <li>Caches recognized child controllers for later response routing.</li>
     *   <li>Injects {@link ClientController} and connection status into controllers implementing
     *       {@link ClientControllerAware}.</li>
     *   <li>Optionally triggers initial data loads for certain manager screens.</li>
     *   <li>Replaces the content of {@code contentHost} with the loaded view.</li>
     * </ul>
     *
     * @param fxmlPath absolute classpath path to the FXML file to load (e.g. {@code "/desktop_views/PayView.fxml"})
     */
    private void loadIntoContentHost(String fxmlPath) {
        try {
            if (!"/desktop_views/ReservationsView.fxml".equals(fxmlPath) && reservationsVC != null)
                reservationsVC.clearReservationIdentity();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            Object ctrl = loader.getController();

            if (ctrl instanceof ReservationsViewController rvc) reservationsVC = rvc;
            if (ctrl instanceof EditReservationViewController edvc) editReservationVC = edvc;
            if (ctrl instanceof HistoryViewController hvc) historyVC = hvc;
            if (ctrl instanceof ReportsViewController rvc) reportsVC = rvc;
            if (ctrl instanceof HistoryViewController hvc) {
                historyVC = hvc;
                historyVC.setUserContext(role, buildDiscountInfo());
            }
            if (ctrl instanceof TablesViewController tvc) tablesVC = tvc;
            if (ctrl instanceof EditTableScreenController etc) editTableVC = etc;
            if (ctrl instanceof UpdateOpeningHoursScreenController uohc) openingHoursVC = uohc;
            if (ctrl instanceof ManagerReservationsScreenController mrc) {
                managerReservationsVC = mrc;
                managerReservationsVC.setOnStartReservationFlow(this::openManagerReservationStart);
            }
            if (ctrl instanceof ManagerReservationStartScreenController startCtrl) {
                startCtrl.setOnContinue(this::openReservationForIdentity);
                startCtrl.setOnCancel(this::openManagerReservations);
            }
            if (ctrl instanceof ShowDataScreenController sdc) showDataVC = sdc;
            if (ctrl instanceof AddSubscriberScreenController asc) {
                addSubscriberVC = asc;
                addSubscriberVC.setRequesterRole(role);
            }
            if (ctrl instanceof SubscribersScreenController ssc) subscribersVC = ssc;
            if (ctrl instanceof SubscriberMainScreenController smc) {
                subscriberMainVC = smc;
                subscriberMainVC.setOnEditReservation(this::openEditReservation);
                subscriberMainVC.setOnPayReservation(this::openPayReservation);
            }
            if (ctrl instanceof PayViewController pvc) payVC = pvc;

            if (ctrl instanceof ClientControllerAware aware) aware.setClientController(clientController, connected);

            if (ctrl instanceof EditTableScreenController etc) etc.requestInitialData();

            if (ctrl instanceof ShowDataScreenController sdc) sdc.requestInitialData();

            if (contentHost != null) contentHost.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
            Label error = new Label("Failed to load screen:\n" + fxmlPath + "\n" + e.getMessage());
            error.getStyleClass().add("muted");
            if (contentHost != null) contentHost.getChildren().setAll(error);
        }
    }

    /**
     * Receives a {@link ReportResponse} from the server and routes it to the reports view if loaded.
     * <p>
     * This handler schedules the UI update on the JavaFX application thread via {@code Platform.runLater}.
     *
     * @param reportResponse the report response payload from the server
     */
    @Override
    public void onReportResponse(ReportResponse reportResponse) {
        javafx.application.Platform.runLater(() -> {
            if (reportsVC != null) {
                reportsVC.onReportResponse(reportResponse);
                return;
            }
            showInfo("Reports", "Report received. Open Reports screen to view it.");
        });
    }

    /**
     * Receives a {@link ReservationResponse} from the server and routes it to loaded reservation-related views.
     * <p>
     * This handler schedules the UI update on the JavaFX application thread via {@code Platform.runLater}.
     *
     * @param response the reservation response payload from the server
     */
    @Override
    public void onReservationResponse(ReservationResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (reservationsVC != null) reservationsVC.handleServerResponse(response);
            if (editReservationVC != null) editReservationVC.onReservationResponse(response);
        });
    }

    /**
     * Receives a {@link SeatingResponse} from the server.
     * <p>
     * Desktop currently does not handle seating responses; seating is handled by terminal screens.
     *
     * @param response the seating response payload from the server
     */
    @Override
    public void onSeatingResponse(SeatingResponse response) {
        // desktop currently does not handle seating responses
        // seating is handled by terminal screens
    }

    /**
     * Constructs discount information for history/billing screens based on the current {@link Role}.
     *
     * @return discount information appropriate for the current role
     */
    private HistoryViewController.DiscountInfo buildDiscountInfo() {
        if (role == Role.SUBSCRIBER) return HistoryViewController.DiscountInfo.active("10% subscriber discount", 0.10);
        return HistoryViewController.DiscountInfo.none();
    }

    /**
     * Receives a list of user history rows and routes them to whichever history-related view is loaded.
     * <p>
     * This handler schedules the UI update on the JavaFX application thread via {@code Platform.runLater}.
     *
     * @param rows the history rows returned by the server
     */
    @Override
    public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
        javafx.application.Platform.runLater(() -> {
            if (historyVC != null) historyVC.renderHistory(rows);
            if (subscribersVC != null)  subscribersVC.renderHistory(rows);
        });
    }

    /**
     * Receives an error related to loading user history and routes it to the active view if present.
     * <p>
     * If no relevant view is loaded, falls back to showing an alert.
     *
     * @param message the error message to display
     */
    @Override
    public void onUserHistoryError(String message) {

        javafx.application.Platform.runLater(() -> {
            boolean handled = false;
            if (historyVC != null) {
                historyVC.showHistoryError(message);
                handled = true;
            }
            if (subscribersVC != null) {
                subscribersVC.showHistoryError(message);
                handled = true;
            }
            if(!handled) showError("History", message);
        });
    }

    /**
     * Receives upcoming reservations for the subscriber and routes them to the subscriber home view if loaded.
     *
     * @param rows upcoming reservations returned by the server
     */
    @Override
    public void onUpcomingReservationsResponse(java.util.List<ReservationResponse> rows) {
        javafx.application.Platform.runLater(() -> {
            if (subscriberMainVC != null) subscriberMainVC.onUpcomingReservationsResponse(rows);
        });
    }

    /**
     * Receives an error related to loading upcoming reservations and routes it to the subscriber home view if loaded.
     *
     * @param message the error message to display
     */
    @Override
    public void onUpcomingReservationsError(String message) {
        javafx.application.Platform.runLater(() -> {
            if (subscriberMainVC != null) subscriberMainVC.onUpcomingReservationsError(message);
        });
    }

    /**
     * Receives subscriber contact details and routes them to the subscriber home view if loaded,
     * otherwise shows them via an informational alert.
     *
     * @param email the subscriber email returned by the server
     * @param phone the subscriber phone number returned by the server
     */
    @Override
    public void onUserDetailsResponse(String email, String phone) {
        javafx.application.Platform.runLater(() -> {
            if (subscriberMainVC != null) subscriberMainVC.onUserDetailsResponse(email, phone);
            else showInfo("Subscriber Details", "Email: " + email + "\nPhone: " + phone);
        });
    }

    /**
     * Routes manager responses to whichever manager view is currently loaded.
     * <p>
     * This handler schedules the UI update on the JavaFX application thread via {@code Platform.runLater}.
     *
     * @param response the manager response payload from the server
     */
    @Override
    public void onManagerResponse(ManagerResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (tablesVC != null) tablesVC.handleManagerResponse(response);
            if (editTableVC != null) editTableVC.handleManagerResponse(response);
            if (openingHoursVC != null) openingHoursVC.handleManagerResponse(response);
            if (managerReservationsVC != null) managerReservationsVC.handleManagerResponse(response);
            if (showDataVC != null) showDataVC.handleManagerResponse(response);
            if (subscribersVC != null) subscribersVC.handleManagerResponse(response);
            if (addSubscriberVC != null) addSubscriberVC.handleManagerResponse(response);
        });
    }

    /**
     * Displays waiting list cancellation results.
     *
     * @param response response indicating whether the waiting list entry was cancelled
     */
    @Override
    public void onWaitingListCancellation(WaitingListResponse response) {
        javafx.application.Platform.runLater(() -> {
            boolean cancelled = response != null && response.getHasBeenCancelled();
            if (cancelled) {
                showInfo("Waiting List", "Waiting list entry cancelled.");
            } else {
                showWarning("Waiting List", "Unable to cancel waiting list entry.");
            }
        });
    }

    /**
     * Displays bill totals and applies subscriber discounts in the pay view.
     * <p>
     * If the pay view is not currently loaded, falls back to displaying an informational alert.
     *
     * @param baseTotal the bill total before any subscriber discount is applied
     * @param isCash    whether the selected payment method is cash
     */
    @Override
    public void onBillTotal(double baseTotal, boolean isCash) {
        javafx.application.Platform.runLater(() -> {
            if (payVC != null) {
                boolean isSubscriber = role == Role.SUBSCRIBER;
                payVC.onBillTotalLoaded(baseTotal, isCash, isSubscriber);
                return;
            }
            showInfo("Billing", "Bill total received: " + String.format("%.2f", baseTotal));
        });
    }

    /**
     * Navigates to the manager reservations screen by selecting the corresponding navigation button if visible.
     */
    private void openManagerReservations() {
        selectIfVisible(managerReservationsBtn);
    }

    /**
     * Opens the manager "start reservation" flow by loading its FXML into the content host.
     */
    private void openManagerReservationStart() {
        loadIntoContentHost("/manager_screen/ManagerReservationStartScreen.fxml");
    }

    /**
     * Sets an identity context (userId/guestContact) for reservation creation/editing flows.
     * <p>
     * This selects the reservations screen and then passes identity details to the loaded reservations view.
     *
     * @param userId       the user ID to associate with the reservation flow (may be {@code null})
     * @param guestContact guest contact details to associate with the reservation flow (may be {@code null})
     */
    private void openReservationForIdentity(String userId, String guestContact) {
        selectIfVisible(reservationsBtn);
        if (reservationsVC != null) {
            reservationsVC.setReservationIdentity(userId, guestContact);
        }
    }

    /**
     * Displays bill-paid confirmation in the pay view or via alert fallback.
     *
     * @param tableNumber the table number associated with the payment (may be {@code null})
     */
    @Override
    public void onBillPaid(Integer tableNumber) {
        javafx.application.Platform.runLater(() -> {
            if (payVC != null) {
                payVC.onBillPaid(tableNumber);
                return;
            }
            showInfo("Billing", "Payment completed.");
        });
    }

    /**
     * Displays a billing error in the pay view or via alert fallback.
     *
     * @param message the error message to display
     */
    @Override
    public void onBillError(String message) {
        javafx.application.Platform.runLater(() -> {
            if (payVC != null) {
                payVC.onBillingError(message);
                return;
            }
            showError("Billing", message == null ? "Billing failed." : message);
        });
    }

    /**
     * Opens the pay screen and loads the bill for the reservation's confirmation code.
     *
     * @param reservation the reservation used to determine the confirmation code; if {@code null}, no action is taken
     */
    private void openPayReservation(ReservationResponse reservation) {
        if (reservation == null) return;
        Integer code = reservation.getConfirmationCode();
        if (code == null || code <= 0) {
            showWarning("Billing", "Missing confirmation code.");
            return;
        }
        selectIfVisible(payBtn);
        if (payVC != null) {
            payVC.loadBillForConfirmationCode(code);
        }
    }

    /**
     * Shows an informational alert.
     *
     * @param title   the alert title
     * @param message the alert message
     */
    @Override
    public void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }

    /**
     * Shows a warning alert.
     *
     * @param title   the alert title
     * @param message the alert message
     */
    @Override
    public void showWarning(String title, String message) {
        showAlert(title, message, Alert.AlertType.WARNING);
    }

    /**
     * Shows an error alert.
     *
     * @param title   the alert title
     * @param message the alert message
     */
    @Override
    public void showError(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }

    /**
     * Displays an arbitrary payload from the server using the default informational alert UI.
     *
     * @param payload the payload to display
     */
    @Override
    public void showPayload(Object payload) {
        showInfo("Server Message", String.valueOf(payload));
    }

    /**
     * Routes the client to the desktop shell.
     * <p>
     * This implementation is a no-op because this controller already represents the desktop.
     *
     * @param role     the role to route to (unused)
     * @param username the username to route with (unused)
     */
    @Override
    public void routeToDesktop(Role role, String username) {
        // already in desktop
        // no-op: this controller is already the desktop
    }

    /**
     * Shows a JavaFX alert dialog on the UI thread.
     *
     * @param title the alert title
     * @param msg   the alert message (may be {@code null})
     * @param type  the alert type
     */
    private void showAlert(String title, String msg, Alert.AlertType type) {
        javafx.application.Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "" : msg);
            a.showAndWait();
        });
    }

    /**
     * Opens the edit reservation screen and loads reservation details by confirmation code.
     *
     * @param reservation the reservation providing the confirmation code; if {@code null}, no action is taken
     */
    private void openEditReservation(ReservationResponse reservation) {
        if (reservation == null) return;
        Integer code = reservation.getConfirmationCode();
        if (code == null || code <= 0) {
            showWarning("Reservation", "Missing confirmation code.");
            return;
        }
        selectIfVisible(editReservationBtn);
        if (editReservationVC != null)  editReservationVC.loadReservationByCode(code);
    }
}
