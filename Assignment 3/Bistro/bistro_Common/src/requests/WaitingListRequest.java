package requests;

import java.time.LocalDate;
import java.time.LocalTime;

public class WaitingListRequest {


	private waitListCommand type;
	private LocalDate waitingForDate;   
    private LocalTime timeInDate;
    private String userID;              
    private String guestContact;   
	public WaitingListRequest() {}
	
	public enum waitListCommand{
		REQ_WAIT,
		CANCEL_WAIT
	}
	
	public WaitingListRequest(boolean wantToWait,waitListCommand type,LocalDate waitingForDate,LocalTime timeInDate,String userID,String guestContact) {
		
		this.type = type;
		this.waitingForDate=waitingForDate;
		this.timeInDate=timeInDate;
		this.userID=userID;
		this.guestContact=guestContact;
	}
	
	public waitListCommand getType() {
		return type;
	}

	public LocalDate getDate() {
		return waitingForDate;
	}
	public LocalTime getTime() {
		return timeInDate;
	}
	public String getUserID() {
		return this.userID;
	}
	public String getGuestContact() {
		return this.guestContact;
	}
	
}
