package responses;

import java.time.LocalDateTime;
import java.time.LocalTime;

import requests.TableInfo;

public class CurrentSeatingResponse {
	
	
	private String userID;
	private String username;
	private TableInfo table;           
    private String guestContact; 
    private int partySize;
    private int seatingID;
    private LocalDateTime checkInTime;
    private LocalDateTime estimatedCheckOut;
    private Integer confrimationCode;
    
    public CurrentSeatingResponse() {}
    
	public CurrentSeatingResponse(String userID,String username, TableInfo table, String guestContact, int partySize, int seatingID,LocalDateTime checkOutTime,int confirmationCode,
			LocalDateTime checkIn) {
		this.userID = userID;
		this.username = username;
		this.table = table;
		this.guestContact = guestContact;
		this.partySize = partySize;
		this.seatingID = seatingID;
		this.estimatedCheckOut = checkOutTime;
		this.confrimationCode = confirmationCode;
		this.checkInTime = checkIn;
	}
	public LocalDateTime getEstimatedCheckOut() {
		return estimatedCheckOut;
	}

	public String getUsername() {
		return username;
	}
	public TableInfo getTable() {
		return table;
	}
	public String getGuestContact() {
		return guestContact;
	}
	public int getPartySize() {
		return partySize;
	}
	public int getSeatingID() {
		return seatingID;
	}

	public String getUserID() {
		return userID;
	}

	public int getConfrimationCode() {
		return confrimationCode;
	}

	public LocalDateTime getCheckInTime() {
		return checkInTime;
	}
		       
    
}
