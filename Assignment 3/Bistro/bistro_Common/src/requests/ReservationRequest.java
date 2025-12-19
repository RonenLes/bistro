package requests;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationRequest   {
	public enum ReservationRequestType{
		FIRST_PHASE,
		SECOND_PHASE
	}
	private ReservationRequestType type;
    private LocalDate reservationDate;  // Client-selected date
    private LocalTime startTime;		//Client-selected hour
    private int partySize;               // Client-selected party size
    private String userID;               // Subscriber ID (null for guest)
    private String guestContact;         // Email/phone for guest (null for subscriber)
    
    public ReservationRequest() {}

    public ReservationRequest(ReservationRequestType type,LocalDate reservationDate,LocalTime startTime,int partySize,String userID,String guestContact) {
       this.type=type;
    	this.reservationDate = reservationDate;
        this.startTime = startTime;
        this.partySize = partySize;
        this.userID = userID;
        this.guestContact = guestContact;
    }
    
    public ReservationRequestType getType() {
        return type;
    }

    public void setType(ReservationRequestType type) {
        this.type = type;
    }
    
    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }
    
    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = partySize;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getGuestContact() {
        return guestContact;
    }

    public void setGuestContact(String guestContact) {
        this.guestContact = guestContact;
    }
    public LocalTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
}
