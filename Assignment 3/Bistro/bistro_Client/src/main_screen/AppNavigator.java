package main_screen;

import controllers.ClientController;
import controllers.ClientUIHandler;
import desktop_screen.DesktopScreenController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import responses.ManagerResponse;
import responses.UserHistoryResponse;
import terminal_screen.TerminalScreenController;
import subscriber_screen.SubscriberMainScreenController;

/**
 * Central UI router that swaps between the two client applications:
 * desktop app and terminal app.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Loads FXML screens and swaps the {@link Stage}'s scene root.</li>
 *   <li>Wires callbacks between screens (back navigation, logout, login success).</li>
 *   <li>Sets the active {@link controllers.ClientUIHandler} on the {@link ClientController}
 *       according to the currently displayed UI mode.</li>
 *   <li>Provides bridge handlers for routing server responses to the active UI controller.</li>
 * </ul>
 * Keeps {@code MainScreenController} lightweight by concentrating navigation logic here.
 */
public class AppNavigator {
    private static final double APP_W = 1280;
    private static final double APP_H = 800;

    /** JavaFX stage reference used for swapping scenes/roots. */
    private final Stage stage;
    /** Shared controller used for server communication across screens. */
    private final ClientController clientController;
    /** Indicates whether the client is currently connected to the server. */
    private final boolean connected;

    /** Cached root node for returning back to the main menu. */
    private Parent mainRoot;

    /** Cached desktop shell controller, when the desktop mode is active. */
    private DesktopScreenController desktopController;
    /** Cached terminal controller, when the terminal mode is active. */
    private TerminalScreenController terminalController;
    /** Cached subscriber home controller (not necessarily active at all times). */
    private SubscriberMainScreenController subscriberController;

    /** Default handler used when no specific screen should receive server callbacks. */
    private final ClientUIHandler navigationHandler = new NavigationUIHandler();

    /**
     * Constructs a navigator for the given stage and shared client controller.
     *
     * @param stage           the JavaFX stage used to swap scene roots
     * @param clientController shared controller used for server communication
     * @param connected       initial connection status of the client
     */
    public AppNavigator(Stage stage, ClientController clientController, boolean connected) {
        this.stage = stage;
        this.clientController = clientController;
        this.connected = connected;
    }

    /**
     * Returns the default navigation handler.
     * <p>
     * Typically used by {@code MainScreenController} during initialization so
     * generic popups can be shown even when no specific screen is active.
     *
     * @return a {@link ClientUIHandler} for generic alerts and navigation routing
     */
    public ClientUIHandler getNavigationHandler() {
        return navigationHandler;
    }

    /**
     * Stores the main menu root for back navigation.
     *
     * @param mainRoot the root node of the main menu screen
     */
    public void setMainRoot(Parent mainRoot) {
        this.mainRoot = mainRoot;
    }

    /**
     * Returns to the main menu screen.
     * <p>
     * Also assigns the {@link #navigationHandler} as the active UI handler so
     * server callbacks are handled as generic alerts while in the main menu.
     */
    public void showMain() {
        runOnFx(() -> {
            if (mainRoot == null) return;

            stage.getScene().setRoot(mainRoot);
            stage.setTitle("Bistro Client");
            stage.sizeToScene();
            stage.centerOnScreen();

            clientController.setUIHandler(navigationHandler);
        });
    }

    /**
     * Loads and displays the login screen.
     * <p>
     * Wires callbacks for:
     * <ul>
     *   <li>Back to main menu</li>
     *   <li>Successful login (routes to desktop with the selected role)</li>
     * </ul>
     * While in login, sets {@link #navigationHandler} as active handler for generic alerts
     * and for routing login success to the desktop screen.
     */
    public void showLogin() {
        runOnFx(() -> {

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/LoginScreen.fxml"));
                Parent loginRoot = loader.load();

                LoginScreenController loginCtrl = loader.getController();
                loginCtrl.setClientController(clientController, connected);

                loginCtrl.setOnBackToMain(this::showMain);

                loginCtrl.setOnLoginAsRole(role -> {
                    String welcome = (role == DesktopScreenController.Role.GUEST)
                            ? "guest"
                            : loginCtrl.getUsernameForWelcome();
                    showDesktop(role, welcome);
                });

                stage.getScene().setRoot(loginRoot);
                stage.setTitle("Login");
                stage.sizeToScene();
                stage.centerOnScreen();

                clientController.setUIHandler(navigationHandler);

            } catch (Exception e) {
                e.printStackTrace();
                navigationHandler.showError("Navigation Error", "Failed to open LoginScreen.\n" + e.getMessage());
            }
        });
    }

    /**
     * Loads and displays the desktop application shell with the given role and welcome name.
     * <p>
     * Sets the desktop controller as the active UI handler so server responses are routed
     * to the currently loaded desktop views.
     *
     * @param role        the role used to configure desktop navigation and permissions
     * @param welcomeName the display name shown in the desktop top bar
     */
    public void showDesktop(DesktopScreenController.Role role, String welcomeName) {
        runOnFx(() -> {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop_screen/DesktopScreen.fxml"));
                Parent desktopRoot = loader.load();

                desktopController = loader.getController();
                desktopController.setClientController(clientController, connected);
                desktopController.setRole(role);
                desktopController.setWelcomeName(welcomeName);

                desktopController.setOnLogout(() -> {
                    desktopController = null;
                    showLogin();
                });

                stage.getScene().setRoot(desktopRoot);
                stage.setTitle("Desktop");
                stage.sizeToScene();
                stage.centerOnScreen();

                clientController.setUIHandler(desktopController);

            } catch (Exception e) {
                e.printStackTrace();
                navigationHandler.showError("Navigation Error", "Failed to open DesktopScreen.\n" + e.getMessage());
            }
        });
    }

    /**
     * Ensures that a runnable is executed on the JavaFX Application Thread.
     *
     * @param r the runnable to execute
     */
    private void runOnFx(Runnable r) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            r.run();
        } else {
            javafx.application.Platform.runLater(r);
        }
    }

    /**
     * Loads and displays the terminal screen used for walk-in customer operations.
     * <p>
     * Sets a {@link TerminalUIBridge} as the active UI handler so that terminal-related
     * server callbacks (seating, billing, waiting list cancellation) are routed to the
     * {@link TerminalScreenController}.
     */
    public void showTerminal() {
        try {
            String fxml = "/terminal_screen/TerminalScreen.fxml";

            var url = getClass().getResource(fxml);
            if (url == null) {
                throw new IllegalArgumentException("FXML not found on classpath: " + fxml);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent terminalRoot = loader.load();

            terminalController = loader.getController();
            terminalController.setClientController(clientController, connected);

            terminalController.setOnBackToMain(() -> {
                terminalController = null;
                showMain();
            });

            stage.getScene().setRoot(terminalRoot);
            stage.setTitle("Terminal");
            stage.sizeToScene();
            stage.centerOnScreen();

            clientController.setUIHandler(new TerminalUIBridge(terminalController));

        } catch (Exception e) {
            e.printStackTrace();
            navigationHandler.showError("Navigation Error", "Failed to open Terminal.\n" + e.getMessage());
        }
    }

    /**
     * Default UI handler used when no specific screen is active.
     * <p>
     * Shows generic alerts and routes a successful login to the desktop screen.
     * Most operation-specific callbacks are intentionally no-ops here because
     * the relevant UI handler will be set when the appropriate screen is active.
     */
    private final class NavigationUIHandler implements ClientUIHandler {

        /**
         * Displays an informational alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showInfo(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.INFORMATION);
        }

        /**
         * Displays a warning alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showWarning(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.WARNING);
        }

        /**
         * Displays an error alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showError(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.ERROR);
        }

        /**
         * Displays an arbitrary payload as an informational alert.
         *
         * @param payload payload to display
         */
        @Override
        public void showPayload(Object payload) {
            showInfo("Server Message", String.valueOf(payload));
        }

        /**
         * Called by {@link ClientController} after a successful login.
         * Routes directly to the desktop shell.
         *
         * @param role     the logged-in user's role
         * @param username username/display name for the desktop welcome label
         */
        @Override
        public void routeToDesktop(DesktopScreenController.Role role, String username) {
            showDesktop(role, username);
        }

        /** Not handled here (desktop becomes handler when relevant). */
        @Override
        public void onReservationResponse(responses.ReservationResponse response) {
        }

        /** Not handled here (desktop becomes handler when active). */
        @Override
        public void onReportResponse(responses.ReportResponse reportResponse) {
        }

        /** Not handled here (terminal becomes handler when active). */
        @Override
        public void onSeatingResponse(responses.SeatingResponse response) {
        }

        /** Not handled here. */
        @Override
        public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
        }

        /**
         * Displays a history-related error alert.
         *
         * @param message error message
         */
        @Override
        public void onUserHistoryError(String message) {
            showError("History", message);
        }

        /** Not handled here. */
        @Override
        public void onUpcomingReservationsResponse(java.util.List<responses.ReservationResponse> rows) {
        }

        /**
         * Displays an alert for upcoming reservation errors.
         *
         * @param message error message
         */
        @Override
        public void onUpcomingReservationsError(String message) {
            showError("Upcoming Reservations", message);
        }

        /** Not handled by the navigation handler. */
        @Override
        public void onManagerResponse(ManagerResponse response) {
        }

        /**
         * Billing callback (desktop is not active): forwards to terminal controller if available.
         *
         * @param baseTotal total bill amount before any client-side discount computation
         * @param isCash    whether the payment method is cash
         */
        @Override
        public void onBillTotal(double baseTotal, boolean isCash) {
            terminalController.onBillTotal(baseTotal);
        }

        /**
         * Billing callback: forwards payment completion to terminal controller if available.
         *
         * @param tableNumber table number freed by the payment (may be {@code null})
         */
        @Override
        public void onBillPaid(Integer tableNumber) {
            terminalController.onBillPaid();
        }

        /**
         * Billing callback: forwards billing errors to terminal controller if available.
         *
         * @param message error message
         */
        @Override
        public void onBillError(String message) {
            terminalController.onBillError(message);
        }

        /**
         * Displays subscriber details as an informational alert.
         *
         * @param email subscriber email
         * @param phone subscriber phone number
         */
        @Override
        public void onUserDetailsResponse(String email, String phone) {
            showInfo("Subscriber Details", "Email: " + email + "\nPhone: " + phone);
        }

        /**
         * Displays the result of a waiting list cancellation request.
         *
         * @param response cancellation response from the server (may be {@code null})
         */
        @Override
        public void onWaitingListCancellation(responses.WaitingListResponse response) {
            boolean cancelled = response != null && response.getHasBeenCancelled();
            showInfo("Waiting List", cancelled
                    ? "Waiting list entry cancelled."
                    : "Unable to cancel waiting list entry.");
        }
    }

    /**
     * UI bridge handler for terminal mode.
     * <p>
     * Routes terminal-specific server callbacks (seating, billing, waiting list cancellation)
     * to the {@link TerminalScreenController}. Non-terminal callbacks are no-ops.
     */
    private static final class TerminalUIBridge implements ClientUIHandler {

        /** Target terminal controller that receives routed callbacks. */
        private final TerminalScreenController terminalController;

        /**
         * Creates a new terminal UI bridge.
         *
         * @param terminalController the terminal controller to receive routed callbacks
         */
        private TerminalUIBridge(TerminalScreenController terminalController) {
            this.terminalController = terminalController;
        }

        /**
         * Displays an informational alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showInfo(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.INFORMATION);
        }

        /**
         * Displays a warning alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showWarning(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.WARNING);
        }

        /**
         * Displays an error alert dialog.
         *
         * @param title   dialog title
         * @param message dialog content
         */
        @Override
        public void showError(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.ERROR);
        }

        /**
         * Displays an arbitrary payload as an informational alert.
         *
         * @param payload payload to display
         */
        @Override
        public void showPayload(Object payload) {
            showInfo("Server Message", String.valueOf(payload));
        }

        /** Terminal mode does not route to desktop. */
        @Override
        public void routeToDesktop(DesktopScreenController.Role role, String username) {
        }

        /** Terminal app doesn't display reports. */
        @Override
        public void onReportResponse(responses.ReportResponse reportResponse) {
        }

        /** Terminal does not handle reservation creation/edit flows. */
        @Override
        public void onReservationResponse(responses.ReservationResponse response) {
        }

        /**
         * Routes seating responses to the terminal controller on the JavaFX thread.
         *
         * @param response seating response from the server
         */
        @Override
        public void onSeatingResponse(responses.SeatingResponse response) {
            javafx.application.Platform.runLater(() -> terminalController.onSeatingResponse(response));
        }

        /** Terminal does not handle history. */
        @Override
        public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
        }

        /** Terminal does not handle upcoming reservations. */
        @Override
        public void onUpcomingReservationsResponse(java.util.List<responses.ReservationResponse> rows) {
        }

        /**
         * Displays an error alert for upcoming reservation errors.
         *
         * @param message error message
         */
        @Override
        public void onUpcomingReservationsError(String message) {
            showError("Upcoming Reservations", message);
        }

        /**
         * Displays an error alert for history errors.
         *
         * @param message error message
         */
        @Override
        public void onUserHistoryError(String message) {
            showError("History", message);
        }

        /** Terminal does not handle manager responses. */
        @Override
        public void onManagerResponse(ManagerResponse response) {
        }

        /**
         * Routes bill total callback to terminal controller on the JavaFX thread.
         *
         * @param baseTotal total bill amount
         * @param isCash    whether the payment method is cash
         */
        @Override
        public void onBillTotal(double baseTotal, boolean isCash) {
            if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillTotal(baseTotal));
        }

        /**
         * Routes bill paid callback to terminal controller on the JavaFX thread.
         *
         * @param tableNumber table number freed by the payment (may be {@code null})
         */
        @Override
        public void onBillPaid(Integer tableNumber) {
            if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillPaid());
        }

        /**
         * Routes bill error callback to terminal controller on the JavaFX thread.
         *
         * @param message error message
         */
        @Override
        public void onBillError(String message) {
            if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillError(message));
        }

        /**
         * Displays subscriber details as an informational alert.
         *
         * @param email subscriber email
         * @param phone subscriber phone number
         */
        @Override
        public void onUserDetailsResponse(String email, String phone) {
            showInfo("Subscriber Details", "Email: " + email + "\nPhone: " + phone);
        }

        /**
         * Routes waiting list cancellation results to the terminal controller on the JavaFX thread.
         *
         * @param response cancellation response from the server
         */
        @Override
        public void onWaitingListCancellation(responses.WaitingListResponse response) {
            if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onWaitingListCancellation(response));
        }
    }
}
