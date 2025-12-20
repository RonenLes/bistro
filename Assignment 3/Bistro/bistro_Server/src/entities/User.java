package entities;

public class User {
	private String userID;
	private String username;
	private String password;
	private String role;
	private String email;
	private String phone;
	public User() {
		
	}

	public User(String userID, String username, String password, String role,String phone,String email) {
		
		this.userID = userID;
		this.username = username;
		this.password = password;
		this.role = role;
		this.email=email;
		this.phone=phone;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
	
	public String getEmail() {
		return this.email;
	}
	public void setEmail(String email) {
		this.email=email;
	}
	public String getPhone() {
		return this.phone;
	}
	public void setPhone(String phone) {
		this.phone=phone;
	}
}
