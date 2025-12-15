package server;

public class ClientSession {
	
	private String ip;
	private String userID;
	
	public ClientSession() {}

	public ClientSession(String ip) {
		this.ip = ip;
		this.userID =null;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getIp() {
		return ip;
	}

	public String getUserID() {
		return userID;
	}
	
	public String toString() {
		return ip;
	}
	
}
