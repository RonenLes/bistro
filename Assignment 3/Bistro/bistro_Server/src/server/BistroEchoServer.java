package server;

import ocsf.server.*;

import requests.*;
import responses.*;
import serverGUI.ServerMainScreenControl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import database.SeatingDAO;
import database.ReservationDAO;
import database.UserDAO;
import database.OpeningHoursDAO;

import controllers.NotificationControl;
import controllers.WaitingListControl;

import controllers.*;
import kryo.KryoUtil;


/**
 * Main OCSF server for the Bistro system.
 *
 * <p>Main idea:
 * This server receives client messages as {@code byte[]} (Kryo-serialized {@link requests.Request} objects),
 * deserializes them, dispatches them to the appropriate controller, then Kryo-serializes a
 * {@link responses.Response} back to the client.</p>
 *
 * <p>Core responsibilities:
 * <ul>
 *   <li>Maintain connected-client sessions ({@link ClientSession}) keyed by {@link ConnectionToClient}</li>
 *   <li>Update the JavaFX server GUI ({@link ServerMainScreenControl}) on connect/login/logout/disconnect</li>
 *   <li>Route incoming commands to controllers: user, reservation, seating, manager, billing, reports, waiting list</li>
 *   <li>Enforce role-based permissions for manager/report commands</li>
 *   <li>Enforce "single manager logged in" behavior (manager singleton gate)</li>
 *   <li>Start/stop background schedulers when the server starts/stops (e.g., {@link BillingScheduler})</li>
 * </ul>
 *
 * <p>Threading:
 * OCSF callbacks may run on non-JavaFX threads; GUI updates are delegated via the GUI controller which
 * internally uses {@code Platform.runLater(...)}.</p>
 */
public class BistroEchoServer extends AbstractServer {
	
	/** Server metadata (host name/IP, etc.). */
	private ServerSession serverSession;
	
	/** Connected clients mapped to their current session metadata (role/userId/username/ip). */
	private final Map<ConnectionToClient,ClientSession> loggedUsers = new ConcurrentHashMap<>();
	
	/** JavaFX controller used to reflect client status changes in real time. */
	private final ServerMainScreenControl serverGUI;
	
	/** Tracks the currently logged-in manager client to enforce a single active manager session. */
	private volatile ConnectionToClient activeManager;
	
	//controllers
	private final UserControl userControl;
	private final ReservationControl reservationControl;
	private final SeatingControl seatingControl;
	private final ManagementControl managerControl;
	private final BillingControl billingControl;
	private final ReportControl reportControl;
	private final WaitingListControl waitingListControl;
	
	/** Background scheduler (e.g., periodic billing/report/waiting-list maintenance). */
	private final BillingScheduler billingScheduler;

	 
	/**
	 * 
	 * @param port
	 * @param serverGUI for updating the client details in real time
	 */
	public BistroEchoServer(int port,ServerMainScreenControl serverGUI) {
		super(port);
		
		serverSession  = new ServerSession();//server details will be stored here		
		
		this.serverGUI = serverGUI;// server gui to fetch client details
		
		//controllers init
		userControl = new UserControl();
		reservationControl = new ReservationControl();
		seatingControl = new SeatingControl();
		managerControl = new ManagementControl();
		billingControl = new BillingControl();
		reportControl = new ReportControl();
		waitingListControl=new WaitingListControl();
		billingScheduler = new BillingScheduler(
		        new SeatingDAO(),
		        billingControl,
		        new ReservationDAO(),
		        new UserDAO(),
		        new NotificationControl(),
		        reportControl,
		        waitingListControl,
		        new OpeningHoursDAO()
		);
	}
	
	/**
     * Called by OCSF when the server starts listening.
     * Starts the background scheduler(s).
     */
	@Override
	protected void serverStarted() {
	    System.out.println("SERVER started listening on port " + getPort());
	    billingScheduler.start(); 
	}
	
	/**
     * Called by OCSF when the server stops listening.
     * Stops the background scheduler(s).
     */
	@Override
	protected void serverStopped() {
	    System.out.println("SERVER stopped listening");
	    billingScheduler.stop();
	}
	
	/**
     * Returns server metadata for UI display (host name/IP, etc.).
     *
     * @return current {@link ServerSession}
     */
	public ServerSession getCurrentSession() {
		return this.serverSession;
	}
	
	
	/**
     * Called by OCSF when a client connects.
     *
     * <p>Creates a {@link ClientSession}, stores it in {@link #loggedUsers}, and notifies the GUI.</p>
     *
     * @param client the connected client
     */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		 String clientIP = client.getInetAddress().getHostAddress();
		 ClientSession clientSession = new ClientSession(clientIP);
		 System.out.println("Client connected from: " + clientIP);
		 loggedUsers.put(client,clientSession);
		 serverGUI.onClientConnected(clientIP);
	}
	
	
	/**
     * Called by OCSF when a client disconnects.
     *
     * <p>Removes the client session, updates the GUI, and clears {@link #activeManager} if needed.</p>
     *
     * @param client the disconnected client
     */
	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		System.out.println("SERVER clientDisconnected fired!");
		ClientSession clientSession = loggedUsers.remove(client);
		if(clientSession!=null) {
			System.out.println("Client disconnected: " + clientSession.getIp());
			serverGUI.onClientDisconnected(clientSession.getIp());
		}
		if (client == activeManager) {
			activeManager = null;
		}
	}
	
	
	/**
     * Called by OCSF when a client throws an exception.
     * Treated as a disconnect for cleanup + GUI update.
     *
     * @param client client that threw the exception
     * @param exception thrown exception
     */
	@Override
	protected void clientException(ConnectionToClient client, Throwable exception) {
	    System.out.println("Client exception (treat as disconnect): " + exception.getMessage());
	    clientDisconnected(client); // cleanup + GUI update
	}
	
	
	/**
     * Main message handler (OCSF callback).
     *
     * <p>Expected message type is either:
     * <ul>
     *   <li>{@code byte[]} containing Kryo-serialized {@link requests.Request}</li>
     *   <li>or (rarely) an already-decoded {@link requests.Request} object</li>
     * </ul>
     *
     * <p>Flow:
     * <ol>
     *   <li>Deserialize byte[] using Kryo (if needed)</li>
     *   <li>Validate the message is a {@code Request<?>}</li>
     *   <li>Dispatch by {@code request.getCommand()}</li>
     *   <li>Call the appropriate controller</li>
     *   <li>Apply session/GUI side effects (login/logout identity updates)</li>
     *   <li>Serialize and send back a {@link responses.Response}</li>
     * </ol>
     *
     * @param msg incoming message (usually {@code byte[]})
     * @param client sending client connection
     */
	@Override
	protected void handleMessageFromClient(Object msg,ConnectionToClient client) {
		
		//the response that will be sent to client (use downcast)
		Response<?> response = null;
		ClientSession session = loggedUsers.get(client);//session holds the user info
		
	    try {
	        Object decoded = msg;
	        if (msg instanceof byte[] bytes)  decoded = KryoUtil.deserialize(bytes);	
	        
	                

	        if (!(decoded instanceof Request<?> request)) {response = new Response<>(false, "Invalid request type", null);}
	            
	         else {

	            switch (request.getCommand()) {

	                case USER_REQUEST -> {
	                	LoginRequest loginReq = (LoginRequest) request.getData();
	                    Response<LoginResponse> loginResp = userControl.handleUserRequest(loginReq);	                    
	                    response = handleLoginWithManagerGate(client, loginReq, loginResp);
	                    if (loginReq != null && loginReq.getUserCommand() == LoginRequest.UserCommand.LOGIN_REQUEST) {
	                        if (session == null) {
	                            System.out.println("session=null");
	                        } else if (session.getUserId() == null || session.getUserId().isBlank()) {
	                            System.out.println("session userId=<unset>");
	                        } else {
	                            System.out.println("session userId=" + session.getUserId());
	                        }
	                    }
	                    
	                }
	                case RESERVATION_REQUEST->{
	                	ReservationRequest reservationReq = (ReservationRequest) request.getData();
	                   
	                     session = loggedUsers.get(client);
	                	if (session != null) {
	                		String existingUserId = reservationReq.getUserID();
	                		String existingContact = reservationReq.getGuestContact();
	                		boolean hasIdentity = (existingUserId != null && !existingUserId.isBlank())
	                				|| (existingContact != null && !existingContact.isBlank());
	                		if (!hasIdentity) {
	                			String sessionUserId = session.getUserId();
	                			if (sessionUserId != null && !sessionUserId.isBlank()) {
	                				reservationReq.setUserID(sessionUserId);
	                			}
	                		}
	                	}
	                    Response<ReservationResponse> reservationResp = reservationControl.handleReservationRequest(reservationReq);	                           
	                    response = reservationResp;
	                }
	                case SEATING_REQUEST ->{
	                	SeatingRequest seatingReq = (SeatingRequest) request.getData();
	                	Response<SeatingResponse> seatingResp = seatingControl.handleSeatingRequest(seatingReq);
	                	response = seatingResp;
	                }
	                case MANAGER_REQUEST ->{
	                	String role = loggedUsers.get(client) == null ? null : loggedUsers.get(client).getRole();
	                	if(!"REPRESENTATIVE".equals(role) && !"MANAGER".equals(role)){
	                		response = new Response<>(false,"No premissions",null);
	                		break;
	                	}
	                	ManagerRequest managerReq = (ManagerRequest)request.getData();
	                	Response<ManagerResponse> managerResp = managerControl.handleManagerRequest(managerReq);
	                	response = managerResp;	                	
	                }
	                case BILLING_REQUEST ->{
	                	BillRequest billReq = (BillRequest)request.getData();
	                	Response<BillResponse> billResp = billingControl.handleBillRequest(billReq);
	                	response = billResp;
	                }
	                case REPORT_REQUEST->{
	                	String role = loggedUsers.get(client) == null ? null : loggedUsers.get(client).getRole();
	                	if(!"MANAGER".equals(role)){
	                		response = new Response<>(false,"No premissions",null);
	                		break;
	                	}
	                	ReportRequest repReq = (ReportRequest)request.getData();	                	
	                	Response<ReportResponse> repResp = reportControl.handleReportRequest(repReq);
	                	response= repResp;
	                }
	                case GUEST_REQUEST->{
	                	GuestRequest guestReq = (GuestRequest)request.getData();
	                	loggedUsers.get(client).setRole("GUEST");
	                	loggedUsers.get(client).setUsername(guestReq.getContact());
	                	response = new Response<>(true,"Welcome to McDickies",null);
	                }
	                case LOST_CODE-> {
	                	String contact = (String)request.getData();
	                	response = reservationControl.retrieveConfirmationCode(contact);
	                }
	                case LOGOUT_REQUEST->{
	                	ClientSession clientSession = loggedUsers.get(client);
	                	if (clientSession != null) {
	                		String clientIp = clientSession.getIp();
	                		clientSession.setRole(null);
	                		clientSession.setUserId(null);
	                		clientSession.setUsername(null);
	                		serverGUI.onClientLogout(clientIp);
	                	}
	                	response = new Response<>(true,"Logged out successfully",null);
	                }
	                case WAITING_LIST_REQUEST -> {
	                	WaitingListRequest waitingListReq = (WaitingListRequest) request.getData();
	                	Response<WaitingListResponse> waitingListResp = waitingListControl.cancelWaitingList(waitingListReq);
	                	response = waitingListResp;
	                }
	                	                
	                default -> response = new Response<>(false, "Unknown command", null);
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	        response = new Response<>(false, "Server error", null);
	    }

	    
	    try {
	    	// Serialize response using Kryo before sending to client
	        client.sendToClient(KryoUtil.serialize(response));
	    } catch (Exception e) {
	        System.out.println("Failed to send response to client");
	    }
	}
	
	
	/**
     * Updates the {@link ClientSession} and server GUI when a login succeeds.
     *
     * @param client current client that logged in
     * @param logReq login request sent by the client
     * @param logRes response produced by {@link UserControl}
     */
	private void handleLoginSuccess(ConnectionToClient client,LoginRequest logReq, Response<LoginResponse> logRes) {
		if(!logRes.isSuccess() || logRes.getData() == null) return;
		
		ClientSession clientSession = loggedUsers.get(client);
		if(client == null) return;
		
		String userId = logRes.getData().getUserID();
		clientSession.setUserId(userId);
		clientSession.setUsername(logRes.getData().getUsername());
		clientSession.setRole(logRes.getData().getRole());		
		serverGUI.onClientLogin(userId,clientSession.getUsername(),clientSession.getRole(),clientSession.getIp());
		System.out.println("[LOGIN SUCCESS] role=" + logRes.getData().getRole()
		        + " userId=" + logRes.getData().getUserID()
		        + " username=" + logRes.getData().getUsername());
	}
	
	 /**
     * Enforces "single manager logged in" behavior, then applies login side-effects.
     *
     * <p>If the incoming login is for a MANAGER and a different manager is already active,
     * returns a failure response. Otherwise, updates {@link #activeManager} and calls
     * {@link #handleLoginSuccess(ConnectionToClient, LoginRequest, Response)}.</p>
     *
     * @param client client attempting login
     * @param loginReq login request
     * @param loginResp login controller response
     * @return the original login response if allowed, otherwise a failure response
     */
	private Response<LoginResponse> handleLoginWithManagerGate(ConnectionToClient client,LoginRequest loginReq,Response<LoginResponse> loginResp) {
		
		if (loginReq == null || loginReq.getUserCommand() != LoginRequest.UserCommand.LOGIN_REQUEST) {
	        return loginResp;
	    }

		if (loginResp == null || !loginResp.isSuccess() || loginResp.getData() == null) {
	        return loginResp;
	    }
		
		if (loginResp.getData().getResponseCommand() != LoginResponse.UserReponseCommand.LOGIN_RESPONSE) {
	        return loginResp;
	    }

		boolean isManager = "MANAGER".equalsIgnoreCase(loginResp.getData().getRole());
		if (isManager) {
	        if (activeManager != null && activeManager != client) {
	            return new Response<>(false, "Another manager is already logged in.", null);
	        }
	        activeManager = client;
	    }
		
		 handleLoginSuccess(client, loginReq, loginResp);
		    return loginResp;
	}	
			
}
