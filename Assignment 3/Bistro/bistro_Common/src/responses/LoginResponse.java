package responses;

public class LoginResponse {
	
	private String userID;
	private String role;
	private String username;
	private UserReponseCommand responseCommand;
	
	public enum UserReponseCommand{
		LOGIN_RESPONSE,
		EDIT_RESPONSE
	}
	
	public LoginResponse() {}
	
	public LoginResponse(String userID, String role,String username,UserReponseCommand responseCommand) {		
		this.userID = userID;
		this.role = role;
		this.username = username;
		this.responseCommand = responseCommand;
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

	public UserReponseCommand getResponseCommand() {
		return responseCommand;
	}

	
	
	
}
