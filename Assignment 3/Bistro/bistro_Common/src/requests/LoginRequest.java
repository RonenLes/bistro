package requests;

public class LoginRequest {
	
	private String username;//make it hold guest contact info
	private String password;
	
	public LoginRequest() {
		
	}

	public LoginRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
	
}
