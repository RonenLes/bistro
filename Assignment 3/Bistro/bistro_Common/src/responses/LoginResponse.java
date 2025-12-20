package responses;

public class LoginResponse {
	
	private String userID;
	private String role;
	private String username;
	
	
	
	public LoginResponse() {}
	
	public LoginResponse(String userID, String role,String username) {		
		this.userID = userID;
		this.role = role;
		this.username = username;
		
	}

	public String getUserID() {
		return userID;
	}

	public String getRole() {
		return role;
	}

	public String getUsername() {
		return username;
	}

	
	
	
}
