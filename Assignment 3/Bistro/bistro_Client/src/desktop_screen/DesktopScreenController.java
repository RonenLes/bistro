package desktop_screen;

import controllers.ClientController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Shell controller (top bar + left navigation + center content host).
 * Handles:
 *  - Role-based nav visibility (Rep/Subscriber/Manager)
 *  - Single selection across all nav toggle buttons
 *  - Content swapping into contentHost (placeholder for now)
 */
public class DesktopScreenController {

    public enum Role {
        GUEST, CUSTOMER, SUBSCRIBER, REP, MANAGER
    }

    @FXML private StackPane contentHost;

    // Role groups from FXML
    @FXML private VBox repGroup;
    @FXML private VBox subscriberGroup;
    @FXML private VBox managerGroup;

    // Optional UI elements
    @FXML private Label welcomeNameLabel;

    // Nav buttons (recommended to have fx:id)
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


    // What screens exist (placeholder keys)
    private enum ScreenKey {
        RESERVATIONS,
        WAITLIST,
        TABLES,
        HISTORY,
        EDIT_DETAILS,
        REPORTS,
        ANALYTICS
    }

    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    /** Call this after loading, based on login response/session */
    public void setRole(Role role) {
        this.role = role != null ? role : Role.GUEST;
        applyRoleVisibility();
        selectDefaultForRole();
    }

    /** Optional: show the name in the top bar */
    public void setWelcomeName(String name) {
        if (welcomeNameLabel != null) {
            welcomeNameLabel.setText("Welcome," + (name == null || name.isBlank() ? "" : " " + name));
        }
    }

    @FXML
    private void initialize() {
        // 1) Register buttons into one ToggleGroup (single selection)
        registerNavButton(reservationsBtn, ScreenKey.RESERVATIONS);
        registerNavButton(waitlistBtn, ScreenKey.WAITLIST);
        registerNavButton(tablesBtn, ScreenKey.TABLES);

        registerNavButton(historyBtn, ScreenKey.HISTORY);
        registerNavButton(editDetailsBtn, ScreenKey.EDIT_DETAILS);

        registerNavButton(reportsBtn, ScreenKey.REPORTS);
        registerNavButton(analyticsBtn, ScreenKey.ANALYTICS);

        // 2) Default visibility (safe)
        applyRoleVisibility();

        // 3) Default content
        // You can choose a better initial screen later
        selectFirstAvailable();
        navToggleGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {

            // Prevent having NO selected nav item (keeps highlight always)
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }

            if (newT instanceof ToggleButton btn) {
                ScreenKey key = navMap.get(btn);
                if (key != null) {
                    navigate(key);
                }
            }
        });
    }

    private void registerNavButton(ToggleButton btn, ScreenKey key) {
        if (btn == null) return;

        btn.setToggleGroup(navToggleGroup);
        navMap.put(btn, key);
    }

    private void applyRoleVisibility() {
        // Rep tools: Reservations / Waitlist / Tables
        boolean repVisible = (role == Role.REP || role == Role.MANAGER);
        setGroupVisible(repGroup, repVisible);

        // Subscriber tools: History / Edit Details
        boolean subscriberVisible = (role == Role.SUBSCRIBER || role == Role.MANAGER);
        setGroupVisible(subscriberGroup, subscriberVisible);

        // Manager tools: Reports / Analytics
        boolean managerVisible = (role == Role.MANAGER);
        setGroupVisible(managerGroup, managerVisible);
    }

    private void setGroupVisible(VBox group, boolean visible) {
        if (group == null) return;
        group.setVisible(visible);
        group.setManaged(visible); // IMPORTANT: removed from layout when hidden
    }

    private void selectDefaultForRole() {
        // Choose a sensible default per role
        switch (role) {
            case MANAGER -> selectIfVisible(reportsBtn);
            case REP -> selectIfVisible(reservationsBtn);
            case SUBSCRIBER -> selectIfVisible(historyBtn);
            default -> selectFirstAvailable();
        }
    }

    private void selectFirstAvailable() {
        // Pick the first visible nav button
        for (ToggleButton btn : navMap.keySet()) {
            if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
                btn.setSelected(true);
                navigate(navMap.get(btn));
                return;
            }
        }
        // If none: clear content
        if (contentHost != null) contentHost.getChildren().clear();
    }

    private void selectIfVisible(ToggleButton btn) {
        if (btn != null && btn.isVisible() && btn.isManaged() && !btn.isDisable()) {
            btn.setSelected(true);
            navigate(navMap.get(btn));
        } else {
            selectFirstAvailable();
        }
    }

    // ---------- FXML Handlers ----------

    /** All ToggleButtons can point to this single handler via onAction="#onNavClicked" */
    @FXML
    private void onNavClicked(javafx.event.ActionEvent event) {
        if (!(event.getSource() instanceof ToggleButton btn)) return;

        ScreenKey key = navMap.get(btn);
        if (key == null) return;

        // Ensure selected state (toggle group already does this)
        btn.setSelected(true);
        navigate(key);
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }
    
    @FXML
    private void onLogoutClicked() {

        if (onLogout != null) {
            onLogout.run();
            return;
        }

        // fallback behavior if no callback is set:
        setRole(Role.GUEST);
        setWelcomeName("");
    }

    // ---------- Navigation ----------

    private void navigate(ScreenKey key) {
        System.out.println("Navigate to: " + key);

        // For now: placeholder node so you SEE something swap in
        Node screen = PlaceholderScreens.make(key);

        contentHost.getChildren().setAll(screen);

        // Later: replace with FXMLLoader-based loading:
        // loadIntoContentHost("/main_screen/Reservations.fxml");
    }

    /**
     * Minimal placeholder screens so navigation feels real.
     * Replace this with actual FXML loads later.
     */
    private static class PlaceholderScreens {
        static Node make(ScreenKey key) {
            VBox box = new VBox(10);
            box.getStyleClass().add("card");
            box.setPrefWidth(900);

            Label title = new Label(key.name().replace('_', ' '));
            title.getStyleClass().add("title");

            Label hint = new Label("Placeholder screen. Connect to real FXML later.");
            hint.getStyleClass().add("muted");

            box.getChildren().addAll(title, hint);
            return box;
        }
    }
}
