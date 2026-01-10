package server;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import serverGUI.ServerMainScreenControl;

public class ServerMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/serverGUI/ServerMainScreen.fxml"));
        Scene scene = new Scene(loader.load());
        
        ServerMainScreenControl controller = loader.getController();

        int port = 5555;
        BistroEchoServer server = new BistroEchoServer(port, controller);
        
        controller.setServer(server);

        stage.setTitle("Bistro Server");
        stage.setScene(scene);

      
        stage.setOnCloseRequest(ev -> {
            try {
                if (server != null) {
                    server.stopListening();
                    server.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
                System.exit(0);
            }
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
