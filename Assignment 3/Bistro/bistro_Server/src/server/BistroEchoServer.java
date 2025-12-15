package server;

import ocsf.server.*;

import requests.*;
import responses.*;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import controllers.*;

public class BistroEchoServer extends AbstractServer {
	
	private final Map<ConnectionToClient,ClientSession> loggedUsers = new ConcurrentHashMap<>();
	
	//controllers
	private final UserControl userControl;
	 
	
	public BistroEchoServer(int port) {
		super(port);
		
		ServerSession serverSession  = new ServerSession();
		
		userControl = new UserControl();
	}
		
	
	@Override
	protected void clientConnected(ConnectionToClient client) {
		 String clientIP = client.getInetAddress().getHostAddress();
		 ClientSession clientSession = new ClientSession(clientIP);
		 System.out.println("Client connected from: " + clientIP);
		 loggedUsers.put(client,clientSession);
	}
	
	
	
	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		ClientSession clientSession = loggedUsers.remove(client);
		if(clientSession!=null) {
			System.out.println("Client disconnected: " + clientSession.getIp());
		}
	}
	
	@Override
	protected void handleMessageFromClient(Object msg,ConnectionToClient client) {
		
		try {
			
			Request<?> request = (Request<?>)msg;
			
			switch(request.getCommand()) {
				
			case LOGIN->{
				
			}
			
			
			}//end switch
			
		}catch(Exception e) {
			
		}
	}
	
	
}
