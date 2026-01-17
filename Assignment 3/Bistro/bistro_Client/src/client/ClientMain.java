package client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import controllers.ClientController;
import main_screen.MainScreenController;

// application entry point for the bistro client
public class ClientMain {
	// server IP address loaded from properties file
	private static String host;
	
	// loads server connection details from properties file
	// tries external file first, then falls back to internal resource
	private static void loadServerDetails() {
		Properties props = new Properties();
		boolean loaded = false;
		try {
		
			// attempt to load from external file in working directory
			try (InputStream external = new FileInputStream("serverDetails.properties")){
				props.load(external);
				System.out.println("Loaded External details file for server.");
	            loaded = true;
			}catch(Exception e) {
				System.out.println("External files for client not found. Trying INTERNAL file...");
			}
			
			
			// fallback: load from resources folder (bundled with JAR)
			if(!loaded) {
				try(InputStream internal = ClientMain.class.getClassLoader().getResourceAsStream("serverDetails.properties")){
					if(internal !=null) {
						props.load(internal);
						System.out.println("Loaded INTERNAL details file.");
						loaded=true;
					}
				}
			}
			host = props.getProperty("hostIP").trim();
			
			if(host == null || host.isBlank()) throw new RuntimeException("Missing property: hostIP");
			 System.out.println(host);
			
		}catch(Exception e) {
			System.out.println("error loading");
		}
	}

    // main entry point: sets up networking, controller, and launches UI
    public static void main(String[] args) {
    	loadServerDetails();
        // config connection
        //String host = "localhost";
        int port = 5555;
        

        // networking
        // create OCSF client (does not connect yet)
        BistroEchoClient echoClient = new BistroEchoClient(host, port);

        //application controller
        // wire up the controller and client (bidirectional reference)
        ClientController controller = new ClientController(echoClient);
        echoClient.setClientController(controller);

        // connect (non-fatal)
        // attempt connection but allow app to launch even if it fails
        boolean connected = false;
        try {
            echoClient.openConnection();
            connected = true;
            System.out.println("[CLIENT] Connected to " + host + ":" + port);
        } catch (Exception e) {
            System.err.println("[CLIENT] Connection failed: " + e.getMessage());
        }

        // launch JavaFX UI
        // pass controller and connection status to UI layer
        MainScreenController.launchUI(controller, connected);
    }
}
