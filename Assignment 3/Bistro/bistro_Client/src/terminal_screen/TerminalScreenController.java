package terminal_screen;

import controllers.ClientController;
import controllers.ClientControllerAware;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

public class TerminalScreenController {

    @FXML private StackPane contentHolder;

    private ClientController clientController;
    private boolean connected;

    private enum View {
        CHECK_IN, WAITING_LIST, PAY_BILL, LOST_CODE
    }

    private final Map<View, Parent> cache = new EnumMap<>(View.class);

    public void setClientController(ClientController controller, boolean connected) {
        this.clientController = controller;
        this.connected = connected;
    }

    @FXML
    private void initialize() {
        show(View.CHECK_IN);
    }

    @FXML private void onCheckIn()        { show(View.CHECK_IN); }
    @FXML private void onJoinWaitingList(){ show(View.WAITING_LIST); }
    @FXML private void onPayBill()        { show(View.PAY_BILL); }
    @FXML private void onLostCode()       { show(View.LOST_CODE); }

    private void show(View view) {
        Parent root = cache.computeIfAbsent(view, this::loadView);
        contentHolder.getChildren().setAll(root);
    }
    
    // view loader for the fxml sub-pages inside termianl UI
    private Parent loadView(View view) {
        try {
            String fxml = switch (view) {
                case CHECK_IN -> "/terminal_screen/TerminalCheckInView.fxml";
                case WAITING_LIST -> "/terminal_screen/TerminalWaitingListView.fxml";
                case PAY_BILL -> "/terminal_screen/TerminalPayBillView.fxml";
                case LOST_CODE -> "/terminal_screen/TerminalLostCodeView.fxml";
            };

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof ClientControllerAware aware) {
                aware.setClientController(clientController, connected);
            }

            return root;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + view, e);
        }
    }
}