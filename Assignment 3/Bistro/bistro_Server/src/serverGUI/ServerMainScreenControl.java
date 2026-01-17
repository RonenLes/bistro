package serverGUI;

import database.DBManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import javafx.collections.*;
import server.BistroEchoServer;

/**
 * JavaFX controller for the server main screen.
 *
 * <p>Main idea:
 * Controls the server UI that starts/stops {@link BistroEchoServer} and displays currently connected clients.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Initialize the clients {@link TableView} and bind columns to {@link ClientTableRow} properties</li>
 *   <li>Start the server (initialize DB, begin listening) and update the UI labels</li>
 *   <li>Stop the server and close the application cleanly</li>
 *   <li>Handle server events: client connected, login, logout, disconnected (UI-safe using {@link Platform#runLater(Runnable)})</li>
 * </ul>
 *
 * <p>Threading:
 * Server events may arrive from non-JavaFX threads, so UI changes are wrapped in {@code Platform.runLater(...)}.
 */
public class ServerMainScreenControl {
	
	/** The running server instance controlled by this UI. */
	private BistroEchoServer bistroServer;
	
	/** Backing list for the clients table (observable by the UI). */
	private final ObservableList<ClientTableRow> clients =  javafx.collections.FXCollections.observableArrayList();
	
	@FXML
	private Button btnStart;
	
	@FXML
	private Label lblHostName;
	
	@FXML
	private Label lblPort;
	
	@FXML
	private Label lblHostIp;
	
	@FXML
	private Label lblStatus;
	
	@FXML
	private TableView<ClientTableRow> clientTable; 
	
	@FXML
	private TableColumn<ClientTableRow, String> colUserId;
	
	@FXML
	private TableColumn<ClientTableRow, String> colUsername;
	
	@FXML
	private TableColumn<ClientTableRow, String> colRole;
	
	@FXML
	private TableColumn<ClientTableRow, String> colIp;
	
	@FXML
	private Button btnExit;
	
	/**
     * JavaFX initialization hook.
     *
     * <p>Sets up table column bindings and attaches the observable {@code clients} list
     * as the items source for {@link #clientTable}.
     */
	@FXML
	public void initialize() {
		colUserId .setCellValueFactory(c->c.getValue().userIdProperty());
		colUsername.setCellValueFactory(c->c.getValue().usernameProperty());
		colRole.setCellValueFactory(c->c.getValue().roleProperty());
		colIp.setCellValueFactory(c->c.getValue().ipProperty());
		
		clientTable.setItems(clients);
	}
	
	/**
     * Injects the server instance that this controller will manage.
     *
     * @param server the {@link BistroEchoServer} instance used for start/stop operations and session info
     */
	public void setServer(BistroEchoServer server) {
		this.bistroServer = server;
	}
	
	/**
     * UI event: a client connected (by IP only).
     *
     * <p>Adds a new row with the client IP. Username/userId/role may be filled later when login arrives.</p>
     *
     * @param ip client IP address
     */
	public void onClientConnected(String ip) {
		Platform.runLater(()->clients.add(new ClientTableRow(ip)));
	}
	
	/**
     * UI event: a client logged in.
     *
     * <p>Updates the existing row for the given IP if present. If the connect event wasn't received yet,
     * creates a row (handles "login happened before connect event").</p>
     *
     * @param userID user identifier
     * @param username username
     * @param role user role (e.g., SUBSCRIBER/MANAGER)
     * @param ip client IP address (used as the matching key)
     */
	public void onClientLogin(String userID,String username,String role,String ip) {
		Platform.runLater(() -> {
	        for (ClientTableRow row : clients) {
	            if (row.getIp().equals(ip)) {          
	                row.setUsername(username);
	                row.setUserId(userID);             
	                row.setRole(role);
	                clientTable.refresh(); 
	                return;
	            }
	        }
	        //login happened before connect event
	        ClientTableRow row = new ClientTableRow(ip);
	        row.setUserId(userID);
	        row.setUsername(username);
	        row.setRole(role);
	        clients.add(row);
	        clientTable.refresh(); 
	    });
	}
	
	/**
     * UI event: a client disconnected.
     *
     * <p>Removes the row associated with the given IP (if present).</p>
     *
     * @param ip client IP address
     */
	public void onClientDisconnected(String ip) {
	    Platform.runLater(() -> {
	        System.out.println("UI disconnect ip=" + ip);	        
	        boolean removed = clients.removeIf(r -> ip != null && ip.equals(r.getIp()));	       
	        clientTable.refresh(); 
	    });
	}

	/**
     * FXML action: starts the server.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate server instance exists</li>
     *   <li>Initialize DB via {@link DBManager#init()}</li>
     *   <li>Start listening via {@link BistroEchoServer#listen()}</li>
     *   <li>Update UI status/host labels and hide the start button</li>
     * </ol>
     *
     * @param event button event
     */
	@FXML
	private void startServer(ActionEvent event) {
		
		if (bistroServer == null) {
	        lblStatus.setText("Server status: no server instance");
	        return;
	    }

	    try {
	    	if (bistroServer == null) {
	            lblStatus.setText("Server status: no server instance");
	            return;
	        }
	    	DBManager.init();
	        bistroServer.listen();

	        btnStart.setVisible(false);
	        lblStatus.setText("Server status: running");
	        lblHostName.setText("Host name: " + bistroServer.getCurrentSession().getHostName());
	        lblPort.setText("PORT: " + bistroServer.getPort());
	        lblHostIp.setText("Host ip: " + bistroServer.getCurrentSession().getHostIP());

	    } catch (Exception e) {
	        lblStatus.setText("Server status: failed to start");
	        e.printStackTrace();
	    }
	}
	
	/**
     * FXML action: stops the server and closes the UI application.
     *
     * <p>Attempts to stop listening and close server resources, then closes the stage and exits JavaFX.</p>
     *
     * @param event button event (used to get the current window/stage)
     */
	@FXML
	private void stopServer(ActionEvent event) {
		try {
            if (bistroServer != null) {
            	bistroServer.stopListening();   
            	bistroServer.close();           
                System.out.println("Server stopped and closed.");
            }
        } catch (Exception e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        try {
        	bistroServer.close();
		} catch (Exception e) {
			
			System.err.println("failed to close server");
		}
        stage.close();
        Platform.exit();
        System.exit(0);
	}
	
	/**
     * UI event: a client logged out (still connected, but no longer associated with a user).
     *
     * <p>Resets the row fields to "-" for the matching IP.</p>
     *
     * @param ip client IP address
     */
	public void onClientLogout(String ip) {
		Platform.runLater(() -> {
			for (ClientTableRow row : clients) {
				if (row.getIp().equals(ip)) {
					row.setUsername("-");
					row.setUserId("-");
					row.setRole("-");
					break;
				}
			}
			clientTable.refresh();
		});
	}
	
}
