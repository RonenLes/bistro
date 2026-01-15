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
 * desktop app and terminal app
 * Keeps MainScreenController lightweight
 */
// manages navigation between different UI modes (main, login, desktop, terminal)
// acts as the navigation hub for the entire application
// provides bridge handlers for routing server responses to active UI
public class AppNavigator {
	private static final double APP_W = 1280;
	private static final double APP_H = 800;
	
    // JavaFX stage reference for swapping scenes
    private final Stage stage;
    // shared controller for server communication
    private final ClientController clientController;
    private final boolean connected;

    // cached root for returning to main menu
    private Parent mainRoot;

    // cached controllers for each UI mode
    private DesktopScreenController desktopController;
    private TerminalScreenController terminalController;
    private SubscriberMainScreenController subscriberController;


    // default handler for when no specific UI is active
    private final ClientUIHandler navigationHandler = new NavigationUIHandler();

    // constructor: receives stage and controller from MainScreenController
    public AppNavigator(Stage stage, ClientController clientController, boolean connected) {
        this.stage = stage;
        this.clientController = clientController;
        this.connected = connected;
    }

    // exposes navigation handler for MainScreenController initialization
    public ClientUIHandler getNavigationHandler() {
        return navigationHandler;
    }

    // stores main menu root for back navigation
    public void setMainRoot(Parent mainRoot) {
        this.mainRoot = mainRoot;
    }

    // returns to main menu screen
    public void showMain() {
        runOnFx(() -> {
            if (mainRoot == null) return;

            stage.getScene().setRoot(mainRoot);
            stage.setTitle("Bistro Client");
            stage.sizeToScene();
            stage.centerOnScreen();

            // main screen does not handle server callbacks
            // use navigation handler for generic alerts
            clientController.setUIHandler(navigationHandler);
        });
    }


    // loads and displays login screen
    // wires up callbacks for back button and successful login
    public void showLogin() {
        runOnFx(() -> {
        	
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_screen/LoginScreen.fxml"));
                Parent loginRoot = loader.load();

                LoginScreenController loginCtrl = loader.getController();
                loginCtrl.setClientController(clientController, connected);

                // set up back button callback
                loginCtrl.setOnBackToMain(this::showMain);

                // set up successful login callback
                loginCtrl.setOnLoginAsRole(role -> {
                    String welcome = (role == DesktopScreenController.Role.GUEST) ? "guest" : loginCtrl.getUsernameForWelcome();
                    showDesktop(role, welcome);
                });

                stage.getScene().setRoot(loginRoot);
                stage.setTitle("Login");
                stage.sizeToScene();
                stage.centerOnScreen();

                // while in login, handle popups and routeToDesktop
                // navigation handler will route to desktop after successful login
                clientController.setUIHandler(navigationHandler);

            } catch (Exception e) {
                e.printStackTrace();
                navigationHandler.showError("Navigation Error", "Failed to open LoginScreen.\n" + e.getMessage());
            }
        });
    }
    

    // loads desktop screen with role and welcome name
    // desktop becomes the active UI handler for server responses
    public void showDesktop(DesktopScreenController.Role role, String welcomeName) {
        runOnFx(() -> {
            try {
            	
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop_screen/DesktopScreen.fxml"));
                Parent desktopRoot = loader.load();

                desktopController = loader.getController();
                desktopController.setClientController(clientController, connected);
                desktopController.setRole(role);
                desktopController.setWelcomeName(welcomeName);

                // wire up logout callback to return to login
                desktopController.setOnLogout(() -> {
                    desktopController = null;
                    showLogin();
                });

                stage.getScene().setRoot(desktopRoot);
                stage.setTitle("Desktop");
                stage.sizeToScene();
                stage.centerOnScreen();

                // desktop becomes the active UI handler for all server callbacks
                // routes responses to child view controllers
                clientController.setUIHandler(desktopController);

            } catch (Exception e) {
                e.printStackTrace();
                navigationHandler.showError("Navigation Error", "Failed to open DesktopScreen.\n" + e.getMessage());
            }
        });
    }
    
   
    
    // ensures code runs on JavaFX application thread
    private void runOnFx(Runnable r) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            r.run();
        } else {
            javafx.application.Platform.runLater(r);
        }
    }
    
    // loads terminal screen for walk-in customer operations
    // uses TerminalUIBridge to route responses
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

            // wire up back button callback
            terminalController.setOnBackToMain(() -> {
                terminalController = null;
                showMain();
            });

            stage.getScene().setRoot(terminalRoot);
            stage.setTitle("Terminal");
            stage.sizeToScene();
            stage.centerOnScreen();

            // terminal becomes the active UI handler for server callbacks
            // bridge pattern routes responses to terminal controller
            clientController.setUIHandler(new TerminalUIBridge(terminalController));

        } catch (Exception e) {
            e.printStackTrace();
            navigationHandler.showError("Navigation Error", "Failed to open Terminal.\n" + e.getMessage());
        }
    }

    // default UI handler used when no specific screen is active
    // handles generic alerts and routes login success to desktop
    private final class NavigationUIHandler implements ClientUIHandler {

        // generic alert displays
        @Override
        public void showInfo(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.INFORMATION);
        }

        @Override
        public void showWarning(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.WARNING);
        }

        @Override
        public void showError(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.ERROR);
        }

        @Override
        public void showPayload(Object payload) {
            showInfo("Server Message", String.valueOf(payload));
        }

        // called by ClientController after successful login
        @Override
        public void routeToDesktop(DesktopScreenController.Role role, String username) {
            showDesktop(role, username);
        }

        // operation-specific callbacks
        // most are no-ops here since navigation handler is for transitions only
        @Override
        public void onReservationResponse(responses.ReservationResponse response) {
            // ignore here, desktop will be the handler when reservations are relevant
        }
        @Override
        public void onReportResponse(responses.ReportResponse reportResponse) {
            // Not handled here (desktop will handle when active)
        }

        @Override
        public void onSeatingResponse(responses.SeatingResponse response) {
            // ignore here, terminal will be the handler when seating is relevant
        }

        @Override
        public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
            // ignore here
        }
        

        @Override
        public void onUserHistoryError(String message) {
            showError("History", message);
        }
        @Override
        public void onUpcomingReservationsResponse(java.util.List<responses.ReservationResponse> rows) {
            // ignore here
        }

        @Override
        public void onUpcomingReservationsError(String message) {
            showError("Upcoming Reservations", message);
        }

		@Override
		public void onManagerResponse(ManagerResponse response) {
			// not handled by navigation handler
			
		}

		// billing callbacks forward to terminal if available
		@Override
		public void onBillTotal(double baseTotal, boolean isCash) {
			terminalController.onBillTotal(baseTotal);
			
		}

		@Override
		public void onBillPaid(Integer tableNumber) {
			  terminalController.onBillPaid();
			
		}

		@Override
		public void onBillError(String message) {
			terminalController.onBillError(message);
			
		}
		@Override
        public void onUserDetailsResponse(String email, String phone) {
            showInfo("Subscriber Details", "Email: " + email + "\nPhone: " + phone);
        }
		@Override
        public void onWaitingListCancellation(responses.WaitingListResponse response) {
            boolean cancelled = response != null && response.getHasBeenCancelled();
            showInfo("Waiting List", cancelled
                    ? "Waiting list entry cancelled."
                    : "Unable to cancel waiting list entry.");
        }
    }
    

    // bridge handler for terminal mode
    // routes terminal-specific responses (seating, billing) to terminal controller
    private static final class TerminalUIBridge implements ClientUIHandler {

        private final TerminalScreenController terminalController;

        private TerminalUIBridge(TerminalScreenController terminalController) {
            this.terminalController = terminalController;
        }

        // generic alerts
        @Override
        public void showInfo(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.INFORMATION);
        }

        @Override
        public void showWarning(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.WARNING);
        }

        @Override
        public void showError(String title, String message) {
            MainScreenController.showAlert(title, message, javafx.scene.control.Alert.AlertType.ERROR);
        }

        @Override
        public void showPayload(Object payload) {
            showInfo("Server Message", String.valueOf(payload));
        }

        @Override
        public void routeToDesktop(DesktopScreenController.Role role, String username) {
            // terminal does not navigate to desktop
        }
        @Override
        public void onReportResponse(responses.ReportResponse reportResponse) {
            // Terminal app doesn't display reports
        }

        @Override
        public void onReservationResponse(responses.ReservationResponse response) {
            // terminal does not handle reservations
        }

        // terminal-specific: seating and check-in operations
        @Override
        public void onSeatingResponse(responses.SeatingResponse response) {
            javafx.application.Platform.runLater(() -> terminalController.onSeatingResponse(response));
        }

        @Override
        public void onUserHistoryResponse(java.util.List<responses.UserHistoryResponse> rows) {
            // terminal does not handle history
        }
        @Override
        public void onUpcomingReservationsResponse(java.util.List<responses.ReservationResponse> rows) {
            // terminal does not handle upcoming reservations
        }

        @Override
        public void onUpcomingReservationsError(String message) {
            showError("Upcoming Reservations", message);
        }

        @Override
        public void onUserHistoryError(String message) {
            showError("History", message);
        }

		@Override
		public void onManagerResponse(ManagerResponse response) {
			// terminal does not handle manager responses
			
		}

		// terminal-specific: billing operations for walk-in customers
		@Override
		public void onBillTotal(double baseTotal, boolean isCash) {
			if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillTotal(baseTotal));
			
		}

		@Override
		public void onBillPaid(Integer tableNumber) {
			if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillPaid());
			
			
		}

		@Override
		public void onBillError(String message) {
			if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onBillError(message));
			
		}
		
		@Override
        public void onUserDetailsResponse(String email, String phone) {
            showInfo("Subscriber Details", "Email: " + email + "\nPhone: " + phone);
        }
        
        // terminal-specific: waiting list cancellation
		@Override
        public void onWaitingListCancellation(responses.WaitingListResponse response) {
            if (terminalController == null) {
                return;
            }
            javafx.application.Platform.runLater(() -> terminalController.onWaitingListCancellation(response));
        }
    }
}
