package server;

import serverGui.NetworkInfoController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.BistroEchoServer;

public class ServerMain extends Application {

    private static BistroEchoServer server;

    public static void main(String[] args) {
        server = new BistroEchoServer(3000);
        try {
            server.listen();
            System.out.println("Server started on port " + server.getPort());
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/serverGui/NetworkInfoView.fxml"));
        Scene scene = new Scene(loader.load());

        NetworkInfoController controller = loader.getController();
        controller.setServer(server);

        primaryStage.setTitle("Bistro Server - Network Info");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
