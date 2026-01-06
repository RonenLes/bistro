package desktop_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Shell controller (top bar + left navigation + center content host).
 * Handles:
 *  - Role-based nav visibility (Rep/Subscriber/Manager)
 *  - Single selection across all nav toggle buttons
 *  - Content swapping into contentHost
 */
public class DesktopScreenController {

    public enum Role {
        GUEST, CUSTOMER, SUBSCRIBER, REP, MANAGER
    }

    @FXML private StackPane contentHost;

    // Role groups
    @FXML private VBox repGroup;
    @FXML private VBox subscriberGroup;
    @FXML private VBox managerGroup;

    // Top bar
    @FXML private Label welcomeNameLabel;

    // Nav buttons
    @FXML private ToggleButton reservationsBtn;
    @FXML private ToggleButton waitlistBtn;
    @FXML private ToggleButton tablesBtn;

    @FXML private ToggleButton historyBtn;
    @FXML private ToggleButton editDetailsBtn;

    @FXML private ToggleButton reportsBtn;
    @FXML private ToggleButton analyticsBtn;

    private final ToggleGroup navToggleGroup = new ToggleGroup();
    private final Map<ToggleButton, ScreenKey> navMap = new HashMap<>();

    private ClientController clientController;
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
        ANALYTICS
    }

    // Public API
    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    public void setRole(Role role) {
        this.role = role != null ? role : Role.GUEST;
        applyRoleVisibility();
        navToggleGroup.selectToggle(null);
    }

    public void setWelcomeName(String name) {
        if (welcomeNameLabel != null) {
            welcomeNameLabel.setText(
                    "Welcome," + (name == null || name.isBlank() ? "" : " " + name)
            );
        }
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    // Initialization
    @FXML
    private void initialize() {

        registerNavButton(reservationsBtn, ScreenKey.RESERVATIONS);
        registerNavButton(waitlistBtn, ScreenKey.WAITLIST);
        registerNavButton(tablesBtn, ScreenKey.TABLES);

        registerNavButton(historyBtn, ScreenKey.HISTORY);
        registerNavButton(editDetailsBtn, ScreenKey.EDIT_DETAILS);

        registerNavButton(reportsBtn, ScreenKey.REPORTS);
        registerNavButton(analyticsBtn, ScreenKey.ANALYTICS);

        applyRoleVisibility();


        navToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            // Allow "no selection" so welcome screen can remain visible.
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

    // Role visibility
    private void applyRoleVisibility() {
        setGroupVisible(repGroup, role == Role.REP || role == Role.MANAGER);
        setGroupVisible(subscriberGroup, role == Role.SUBSCRIBER || role == Role.MANAGER);
        setGroupVisible(managerGroup, role == Role.MANAGER);
    }

    private void setGroupVisible(VBox group, boolean visible) {
        if (group == null) return;
        group.setVisible(visible);
        group.setManaged(visible);
    }

    private void selectDefaultForRole() {
        switch (role) {
            case MANAGER -> selectIfVisible(reportsBtn);
            case REP -> selectIfVisible(reservationsBtn);
            case SUBSCRIBER -> selectIfVisible(historyBtn);
            default -> selectFirstAvailable();
        }
    }

    private void selectFirstAvailable() {
        for (ToggleButton btn : navMap.keySet()) {
            if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
                btn.setSelected(true);
                navigate(navMap.get(btn));
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
        }
    }

    private void loadIntoContentHost(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            Object ctrl = loader.getController();
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
}
