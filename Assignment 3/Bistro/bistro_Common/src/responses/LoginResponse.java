package responses;

import java.util.List;

public class LoginResponse {
	
	private String userID;
	private String role;
	private String username;
	
	//also for manager/ and for showing details
	private String email;
	private String phone;
	
	List<UserHistoryResponse> userHistory;
	private List<ReservationResponse> upcomingReservations;
	
	private UserReponseCommand responseCommand;
	
	public enum UserReponseCommand{
		LOGIN_RESPONSE,
		SHOW_DETAIL_RESPONSE,
		HISTORY_RESPONSE,
		EDIT_RESPONSE,
		UPCOMING_RESERVATIONS_RESPONSE
	}
	
	public LoginResponse() {}
	
	public LoginResponse(UserReponseCommand urc, List<ReservationResponse> upcomingReservations, boolean forUpcoming) {
		this.responseCommand = urc;
		this.upcomingReservations = upcomingReservations;
	}
	
	public LoginResponse(UserReponseCommand urc,List<UserHistoryResponse> userHistory) {
		this.responseCommand =urc;
		this.userHistory=userHistory;
	}
	
	public LoginResponse(UserReponseCommand urc,String email,String phone) {
		this.responseCommand=urc;
		this.email=email;
		this.phone=phone;
	}
	
	public LoginResponse(String userID,String username,String email,String phone) {
		this.userID=userID;
		this.username=username;
		this.email=email;
		this.phone=phone;
	}
	
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

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public List<UserHistoryResponse> getUserHistory() {
		return userHistory;
	}

	public List<ReservationResponse> getUpcomingReservations() {
		return upcomingReservations;
	}
	
	
}
