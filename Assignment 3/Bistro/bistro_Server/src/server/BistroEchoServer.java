package server;

import ocsf.server.*;

import requests.*;
import responses.*;
import serverGUI.ServerMainScreenControl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import controllers.*;
import kryo.KryoUtil;


/**
 * All client-server communication is performed using @code byte
 * serialized and deserialized  via kryo 
 * 
 * the server handles:
 * 1.client login
 */
public class BistroEchoServer extends AbstractServer {
	
	private ServerSession serverSession;
	private final Map<ConnectionToClient,ClientSession> loggedUsers = new ConcurrentHashMap<>();
	private final ServerMainScreenControl serverGUI;
	
	//controllers
	private final UserControl userControl;
	private final ReservationControl reservationControl;
	 
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
	}
	
	/**
	 * 
	 * @return object containing server metadata
	 */
	public ServerSession getCurrentSession() {
		return this.serverSession;
	}
	
	
	/**
	 * ClientSession stores the meta data of the connected client
	 * the server gui is updated using this
	 */
	@Override
	protected void clientConnected(ConnectionToClient client) {
		 String clientIP = client.getInetAddress().getHostAddress();
		 ClientSession clientSession = new ClientSession(clientIP);
		 System.out.println("Client connected from: " + clientIP);
		 loggedUsers.put(client,clientSession);
		 serverGUI.onClientConnected(clientIP);
	}
	
	
	
	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		System.out.println("SERVER clientDisconnected fired!");
		ClientSession clientSession = loggedUsers.remove(client);
		if(clientSession!=null) {
			System.out.println("Client disconnected: " + clientSession.getIp());
			serverGUI.onClientDisconnected(clientSession.getIp());
		}
	}
	
	@Override
	protected void clientException(ConnectionToClient client, Throwable exception) {
	    System.out.println("Client exception (treat as disconnect): " + exception.getMessage());
	    clientDisconnected(client); // cleanup + GUI update
	}
	
	
	/**
	 * The incoming message is expected to be a @code byte containing kyro serialized request object
	 * The outcoming message is @code byte containing kyro serialized response object
	 * 
	 * flow of events:
	 * Deserialize the incoming byte array using kryo
	 * Validate that the object is a request object
	 * Dispatch the request based on its command (enum)
	 * Invoke the appropriate controller logic
	 * Update client session and GUI state if necessary
	 * Serialize and send a Response object back to client 
	 */
	@Override
	protected void handleMessageFromClient(Object msg,ConnectionToClient client) {
		
		//the response that will be sent to client (use downcast)
		Response<?> response = null;
		ClientSession session = loggedUsers.get(client);//session holds the user info
		
	    try {
	        Object decoded = msg;
	        if (msg instanceof byte[] bytes)  decoded = KryoUtil.deserialize(bytes);	
	        System.out.println("SERVER decoded = " + decoded.getClass().getName()
	                + " | module=" + decoded.getClass().getModule().getName());


	        if (!(decoded instanceof Request<?> request)) {response = new Response<>(false, "Invalid request type", null);}
	            
	         else {

	            switch (request.getCommand()) {

	                case USER_REQUEST -> {
	                    LoginRequest loginReq = (LoginRequest) request.getData();	                    
	                    Response<LoginResponse> loginResp = userControl.login(loginReq);
	                    response = loginResp;
	                    handleLoginSuccess(client, loginReq, loginResp);
	                    System.out.println(session != null ? session.getUserId() : "session=null");
	                    
	                }
	                case RESERVATION_REQUEST->{
	                	ReservationRequest reservationReq = (ReservationRequest) request.getData();
	                	reservationReq.setUserID(loggedUsers.get(client).getUserId());
	                	Response<ReservationResponse> reservationResp = reservationControl.handleReservationRequest(reservationReq);
	                	response = reservationResp;	                	
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
	 * method to update the client session 
	 * @param client current client that logged in
	 * @param logReq the deatils the client entered to login
	 * @param logRes the response that was handled by UserControl
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
	}
	
	
}
