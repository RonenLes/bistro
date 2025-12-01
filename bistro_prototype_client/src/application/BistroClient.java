package application;
import java.net.Socket;
import controllers.ClientController;
import ocsf.client.AbstractClient;
import java.lang.reflect.Field;
public class BistroClient extends AbstractClient {

    private ClientController controller;

    public BistroClient(String host, int port, ClientController controller) {
        super(host, port);
        this.controller = controller;
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        controller.handleServerResponse(msg);
    }
    /**
     * פונקציה זו משתמשת ב-Reflection כדי לגשת לאובייקט ה-Socket הפרטי
     * שנמצא בתוך המחלקה האב (AbstractClient).
     * @return אובייקט Socket, או null אם יש שגיאה או אם החיבור סגור.
     */
    public Socket getSocket() {
        if (!this.isConnected()) {
            return null;
        }

        try {
            
            Field socketField = AbstractClient.class.getDeclaredField("clientSocket");
            socketField.setAccessible(true); 
            return (Socket) socketField.get(this);
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Error accessing clientSocket via Reflection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}