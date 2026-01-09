package desktop_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import desktop_views.ReservationsViewController;
import responses.ReservationResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shell controller (top bar + left navigation + center content host)
 * Handles:
 * Role based nav visibility (per button)
 * Single selection across all nav toggle buttons
 * Content swapping into contentHost
 */
public class DesktopScreenController {

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
    @FXML private ToggleButton editDetailsBtn;

    @FXML private ToggleButton reportsBtn;
    @FXML private ToggleButton analyticsBtn;
    @FXML private ToggleButton payBtn;

    private final ToggleGroup navToggleGroup = new ToggleGroup();
    private final Map<ToggleButton, ScreenKey> navMap = new HashMap<>();

    private ClientController clientController;
    private ReservationsViewController reservationsVC;

    private Role role = Role.GUEST;
    private Runnable onLogout;

    // Screens
    private enum ScreenKey {
        RESERVATIONS,
        WAITLIST,
        TABLES,
        HISTORY,
        EDIT_DETAILS,
        REPORTS,
        ANALYTICS,
        PAY
    }

    // Role -> allowed screens
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
                ScreenKey.TABLES
        ));

        // MANAGER -> edit details, new reservations, pay, history, tables, analytics, reports
        ROLE_SCREENS.put(Role.MANAGER, EnumSet.of(
                ScreenKey.EDIT_DETAILS,
                ScreenKey.RESERVATIONS,
                ScreenKey.PAY,
                ScreenKey.HISTORY,
                ScreenKey.TABLES,
                ScreenKey.ANALYTICS,
                ScreenKey.REPORTS
        ));
    }

    // Public API
    public void setClientController(ClientController controller) {
        this.clientController = controller;
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
        registerNavButton(editDetailsBtn,  ScreenKey.EDIT_DETAILS);

        registerNavButton(reportsBtn,      ScreenKey.REPORTS);
        registerNavButton(analyticsBtn,    ScreenKey.ANALYTICS);
        registerNavButton(payBtn,          ScreenKey.PAY);

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
            case SUBSCRIBER -> selectIfVisible(historyBtn);    // subscriber default
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

            case HISTORY ->
                    loadIntoContentHost("/desktop_views/HistoryView.fxml");

            case EDIT_DETAILS ->
                    loadIntoContentHost("/desktop_views/EditDetailsView.fxml");

            case REPORTS ->
                    loadIntoContentHost("/desktop_views/ReportsView.fxml");

            case ANALYTICS ->
                    loadIntoContentHost("/desktop_views/AnalyticsView.fxml");

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
            
            if (ctrl instanceof ClientControllerAware aware) {
                aware.setClientController(clientController, clientController != null);
            }

            contentHost.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            Label error = new Label("Failed to load screen:\n" + fxmlPath);
            error.getStyleClass().add("muted");
            contentHost.getChildren().setAll(error);
        }
    }
    public void onReservationResponse(ReservationResponse response) {
        // Ensure UI updates happen on the FX Thread
        javafx.application.Platform.runLater(() -> {
            if (reservationsVC != null) {
                reservationsVC.handleServerResponse(response);
            }
        });
    }
    
}
