package requests;


/**
 * Request payload for user-related operations.
 *
 * <p>Main idea:
 * Carries a {@link UserCommand} plus the relevant data for that command:
 * login (username/password), view/edit contact details, history, and upcoming reservations.</p>
 *
 * <p>Main parts:
 * <ul>
 *   <li>{@link UserCommand} - which user operation to perform</li>
 *   <li>{@code username}/{@code password} - used for login</li>
 *   <li>{@code phone}/{@code email} - used for editing details</li>
 * </ul>
 */
public class LoginRequest {
	
	
	private UserCommand userCommand;
	
	//for logging in
	private String username;//make it hold guest contact info
	private String password;
	
	//for editing
	private String phone;
	private String email;
	
	
	public enum UserCommand{
		LOGIN_REQUEST,
		SHOW_DETAILS_REQUEST,
		EDIT_DETAIL_REQUEST,
		HISTORY_REQUEST,
		UPCOMING_RESERVATIONS_REQUEST,
		
		
	}
	public LoginRequest() {
		
	}
	public LoginRequest(String username, String phone, String email, UserCommand userCommand) {
		this.username = username;
		this.phone = phone;
		this.email = email;
		this.userCommand = userCommand;
	}
	
	public LoginRequest(String phone,String email) {
		this.phone = phone;
		this.email = email;
	}

	public LoginRequest(String username, String password,UserCommand userCommand) {
		this.username = username;
		this.password = password;
		this.userCommand = userCommand;
	}

	public void setUserCommand(UserCommand userCommand) {
		this.userCommand = userCommand;
	}

	public String getUsername() {
		return username;
	}

	public UserCommand getUserCommand() {
		return userCommand;
	}

	public String getPassword() {
		return password;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}
	
	
}
