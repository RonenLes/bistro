package desktop_screen;


import controllers.ClientController;
import controllers.ClientControllerAware;
import desktop_views.ReservationsViewController;
import desktop_views.TablesViewController;
import responses.ReservationResponse;
import responses.SeatingResponse;
import desktop_views.EditReservationViewController;
import desktop_views.HistoryViewController;
import desktop_views.EditReservationViewController;
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
import manager_screen.ShowDataScreenController;
import manager_screen.UpdateOpeningHoursScreenController;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import responses.ManagerResponse;
import responses.ReservationResponse;
import responses.SeatingResponse;

/**
 * Shell controller (top bar + left navigation + center content host)
 * Handles:
 * Role based nav visibility (per button)
 * Single selection across all nav toggle buttons
 * Content swapping into contentHosts
 */
public class DesktopScreenController implements controllers.ClientUIHandler {

    public enum Role {
        GUEST,
        SUBSCRIBER,
        REP,
        MANAGER
    }

    @FXML private StackPane contentHost;

    // Role groups (just layout containers)
    @FXML private VBox repGroup;
    @FXML private VBox subscriberGroup;
    @FXML private VBox managerGroup;

    // Top bar
    @FXML private Label welcomeNameLabel;

    // Nav buttons
    @FXML private ToggleButton reservationsBtn;   // "New Reservations"
    @FXML private ToggleButton waitlistBtn;
    @FXML private ToggleButton tablesBtn;

    @FXML private ToggleButton historyBtn;
    @FXML private ToggleButton editReservationBtn;

    @FXML private ToggleButton reportsBtn;
    @FXML private ToggleButton analyticsBtn;
    @FXML private ToggleButton payBtn;
    
    private boolean connected;

    private final ToggleGroup navToggleGroup = new ToggleGroup();
    private final Map<ToggleButton, ScreenKey> navMap = new HashMap<>();

    private ClientController clientController;
    private ReservationsViewController reservationsVC;
    private EditReservationViewController editReservationVC;
    private HistoryViewController historyVC;

 
    private TablesViewController tablesVC;
    private EditTableScreenController editTableVC;
    private UpdateOpeningHoursScreenController openingHoursVC;
    private ShowDataScreenController showDataVC;
    private AddSubscriberScreenController addSubscriberVC;
    
    @FXML private ToggleButton editTablesBtn;
    @FXML private ToggleButton openingHoursBtn;
    @FXML private ToggleButton managerDataBtn;
    @FXML private ToggleButton addSubscriberBtn;


    private Role role = Role.GUEST;
    private Runnable onLogout;

    // Screens
    private enum ScreenKey {
        RESERVATIONS,
        WAITLIST,
        TABLES,
        HISTORY,
        EDIT_DETAILS,
        ADD_SUBSCRIBER,
        REPORTS,
        ANALYTICS,
        PAY,
        EDIT_TABLES,
        OPENING_HOURS,
        MANAGER_DATA
    }

    // Role == allowed screens
    private static final Map<Role, Set<ScreenKey>> ROLE_SCREENS = new EnumMap<>(Role.class);
    static {
        // GUEST -> edit details, new reservations, pay
        ROLE_SCREENS.put(Role.GUEST, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY
        ));

        // SUBSCRIBER -> edit details, new reservations, pay, history
        ROLE_SCREENS.put(Role.SUBSCRIBER, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY,
                ScreenKey.HISTORY
        ));

        // REP -> edit details, new reservations, pay, history, tables
        ROLE_SCREENS.put(Role.REP, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY,
                ScreenKey.HISTORY,
                ScreenKey.WAITLIST,
                ScreenKey.TABLES
        ));
// MANAGER -> edit details, new reservations, pay, history, tables, analytics, reports
        ROLE_SCREENS.put(Role.MANAGER, EnumSet.of(

                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY,

                ScreenKey.ADD_SUBSCRIBER,
                ScreenKey.TABLES,
                ScreenKey.EDIT_TABLES,
                ScreenKey.MANAGER_DATA,
                ScreenKey.ANALYTICS,
                ScreenKey.REPORTS,
                ScreenKey.OPENING_HOURS
        ));
}

    // Public API
    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }
    
    public void setRole(Role role) {
        this.role = role != null ? role : Role.GUEST;
        applyRoleVisibility();
        navToggleGroup.selectToggle(null);// clear selection
        selectDefaultForRole(); // pick default screen for this role
    }

    public void setWelcomeName(String name) {
        if (welcomeNameLabel != null) {
            String trimmed = name == null ? "" : name.trim();
            if (role == Role.GUEST || trimmed.equalsIgnoreCase("guest")) {
                welcomeNameLabel.setText("Hello, guest.");
            } else if (trimmed.isEmpty()) {
                welcomeNameLabel.setText("Hello.");
            } else {
                welcomeNameLabel.setText("Hello, " + trimmed + ".");
            }
        }
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    // Initialization
    @FXML
    private void initialize() {

        registerNavButton(reservationsBtn, ScreenKey.RESERVATIONS);
        registerNavButton(waitlistBtn,     ScreenKey.WAITLIST);
        registerNavButton(tablesBtn,       ScreenKey.TABLES);

        registerNavButton(historyBtn,      ScreenKey.HISTORY);
        registerNavButton(editReservationBtn,  ScreenKey.EDIT_DETAILS);

        registerNavButton(reportsBtn,      ScreenKey.REPORTS);
        registerNavButton(analyticsBtn,    ScreenKey.ANALYTICS);
        registerNavButton(payBtn,          ScreenKey.PAY);
        registerNavButton(editTablesBtn,   ScreenKey.EDIT_TABLES);
        registerNavButton(openingHoursBtn, ScreenKey.OPENING_HOURS);
        registerNavButton(managerDataBtn,  ScreenKey.MANAGER_DATA);
        registerNavButton(addSubscriberBtn, ScreenKey.ADD_SUBSCRIBER);

        applyRoleVisibility();

        navToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // Allow no selection so welcome screen can remain visible.
            if (newT instanceof ToggleButton btn) {
                ScreenKey key = navMap.get(btn);
                if (key != null) navigate(key);
            }
        });
    }

    private void registerNavButton(ToggleButton btn, ScreenKey key) {
        if (btn == null) return;
        btn.setToggleGroup(navToggleGroup);
        navMap.put(btn, key);
    }

    // Role visibility (PER BUTTON)
    private void applyRoleVisibility() {
        Set<ScreenKey> allowed = ROLE_SCREENS.getOrDefault(role, Set.of());

        // Show/hide each button individually
        for (Map.Entry<ToggleButton, ScreenKey> entry : navMap.entrySet()) {
            ToggleButton btn = entry.getKey();
            ScreenKey key = entry.getValue();
            if (btn == null) continue;

            boolean visible = allowed.contains(key);
            btn.setVisible(visible);
            btn.setManaged(visible);
        }

        // Update groups visibility based on whether they have any visible children
        updateGroupVisibility(repGroup);
        updateGroupVisibility(subscriberGroup);
        updateGroupVisibility(managerGroup);
    }

    private void updateGroupVisibility(VBox group) {
        if (group == null) return;

        boolean hasVisibleChild = group.getChildren().stream()
                .filter(n -> n instanceof ToggleButton)
                .map(n -> (ToggleButton) n)
                .anyMatch(btn -> btn.isVisible() && btn.isManaged());

        group.setVisible(hasVisibleChild);
        group.setManaged(hasVisibleChild);
    }

    private void selectDefaultForRole() {
        switch (role) {
            case MANAGER -> selectIfVisible(reportsBtn);       // manager default
            case REP     -> selectIfVisible(reservationsBtn);  // rep default
            case SUBSCRIBER -> selectIfVisible(reservationsBtn);    // subscriber default
            case GUEST   -> selectIfVisible(reservationsBtn);  // guest default
            default      -> selectFirstAvailable();
        }
    }

    private void selectFirstAvailable() {
        for (Map.Entry<ToggleButton, ScreenKey> entry : navMap.entrySet()) {
            ToggleButton btn = entry.getKey();
            if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
                btn.setSelected(true);
                navigate(entry.getValue());
                return;
            }
        }
        contentHost.getChildren().clear();
    }

    private void selectIfVisible(ToggleButton btn) {
        if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
            btn.setSelected(true);
            navigate(navMap.get(btn));
        } else {
            selectFirstAvailable();
        }
    }

    // FXML handlers
    @FXML
    private void onNavClicked(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof ToggleButton btn)) return;
        ScreenKey key = navMap.get(btn);
        if (key != null) {
            btn.setSelected(true);
            navigate(key);
        }
    }

    @FXML
    private void onLogoutClicked() {
        // 1) Tell controller to log out + disconnect from server
        if (clientController != null) {
            clientController.logout();
        }

        // 2) Let the outer UI (MainScreenController) do its thing (go back to login window, etc.)
        if (onLogout != null) {
            onLogout.run();
        } else {
            // Fallback: reset this shell to a “guest / empty” state
            setRole(Role.GUEST);
            setWelcomeName("");
            contentHost.getChildren().clear();
        }
    }

    // Navigation (REAL SCREEN LOADING)
    private void navigate(ScreenKey key) {
        switch (key) {
            case RESERVATIONS ->
                    loadIntoContentHost("/desktop_views/ReservationsView.fxml");

            case WAITLIST ->
                    loadIntoContentHost("/desktop_views/WaitlistView.fxml");

            case TABLES ->
                    loadIntoContentHost("/desktop_views/TablesView.fxml");
            case EDIT_TABLES ->
            		loadIntoContentHost("/manager_screen/EditTableScreen.fxml");
            case MANAGER_DATA ->
            		loadIntoContentHost("/manager_screen/ShowDataScreen.fxml");    
            case ADD_SUBSCRIBER ->
            		loadIntoContentHost("/manager_screen/AddSubscriberScreen.fxml");
            case HISTORY ->
                    loadIntoContentHost("/desktop_views/HistoryView.fxml");

            case EDIT_DETAILS ->
                    loadIntoContentHost("/desktop_views/EditReservationView.fxml");

            case REPORTS ->
                    loadIntoContentHost("/desktop_views/ReportsView.fxml");

            case ANALYTICS ->
                    loadIntoContentHost("/desktop_views/AnalyticsView.fxml");
            case OPENING_HOURS ->
            		loadIntoContentHost("/manager_screen/UpdateOpeningHoursScreen.fxml");

            case PAY ->
                    loadIntoContentHost("/desktop_views/PayView.fxml");
                    
        }
    }

    private void loadIntoContentHost(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof ReservationsViewController rvc) {
                reservationsVC = rvc;
            }
            
            if (ctrl instanceof TablesViewController tvc) {
                tablesVC = tvc;
            }
            if (ctrl instanceof EditTableScreenController etc) {
                editTableVC = etc;
            }
            if (ctrl instanceof UpdateOpeningHoursScreenController uohc) {
                openingHoursVC = uohc;
            }
            
            if (ctrl instanceof EditReservationViewController edvc) {
                editReservationVC = edvc;
            }
            if (ctrl instanceof ShowDataScreenController sdc) {
                showDataVC = sdc;
            }
            if (ctrl instanceof AddSubscriberScreenController asc) {
                addSubscriberVC = asc;
            }
            
            if (ctrl instanceof ClientControllerAware aware) {
            	aware.setClientController(clientController, connected);
            }
            if (ctrl instanceof HistoryViewController hvc) {
                historyVC = hvc;
            }
            if (ctrl instanceof EditTableScreenController etc) {
                etc.requestInitialData();
            }
            if (ctrl instanceof ShowDataScreenController sdc) {
                sdc.requestInitialData();
            }
            if (ctrl instanceof EditTableScreenController etc) {
                etc.requestInitialData();
            }
            if (ctrl instanceof ShowDataScreenController sdc) {
                sdc.requestInitialData();
            }



            contentHost.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            Label error = new Label("Failed to load screen:\n" + fxmlPath);
            error.getStyleClass().add("muted");
            contentHost.getChildren().setAll(error);
        }
    }
    // handle javafx run-later on responses
    public void onReservationResponse(ReservationResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (reservationsVC != null) {
                reservationsVC.handleServerResponse(response);
            }
            if (editReservationVC != null) {
                editReservationVC.onReservationResponse(response);
            }
        });
    }
    public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
        javafx.application.Platform.runLater(() -> {
            if (historyVC != null) {
                historyVC.renderHistory(rows);
            }
        });
    }

    @Override
    public void showInfo(String title, String message) {
        showAlert(title, message, Alert.AlertType.INFORMATION);
    }

    @Override
    public void showWarning(String title, String message) {
        showAlert(title, message, Alert.AlertType.WARNING);
    }

    @Override
    public void showError(String title, String message) {
        showAlert(title, message, Alert.AlertType.ERROR);
    }

    @Override
    public void showPayload(Object payload) {
        showInfo("Server Message", String.valueOf(payload));
    }

    @Override
    public void routeToDesktop(Role role, String username) {
        // already in desktop
    }

    @Override
    public void onSeatingResponse(responses.SeatingResponse response) {
        // desktop currently does not handle seating responses
    }

    @Override
    public void onUserHistoryError(String message) {
        javafx.application.Platform.runLater(() -> {
            if (historyVC != null) {
                historyVC.showHistoryError(message);
            } else {
                showError("History", message);
            }
        });
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        javafx.application.Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg == null ? "" : msg);
            a.showAndWait();
        });
    }
    
    public void onManagerResponse(ManagerResponse response) {
        javafx.application.Platform.runLater(() -> {
            if (tablesVC != null) {
                tablesVC.handleManagerResponse(response);
            }
            if (editTableVC != null) {
                editTableVC.handleManagerResponse(response);
            }
            if (openingHoursVC != null) {
                openingHoursVC.handleManagerResponse(response);
            }
            if (showDataVC != null) {
                showDataVC.handleManagerResponse(response);
            }
            if (addSubscriberVC != null) {
                addSubscriberVC.handleManagerResponse(response);
            }
        });
    }
    
}
