package responses;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class ReservationResponse  {
	public enum ReservationResponseType{
		FIRST_PHASE_SHOW_AVAILABILITY,
		FIRST_PHASE_SHOW_SUGGESTIONS,
		FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS,
		 SECOND_PHASE_CONFIRMED,
		 EDIT_RESERVATION,
		 SHOW_RESERVATION,
		 CANCEL_RESERVATION,
		 WALKIN_SEATED,
		 WALKIN_WAITING
	}
	
	
	private LocalDate date;
	private int partySize;
	private LocalTime time;
	private String userID;
	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}
	private String guestContact;
	
	
	private int tableNumber;
	
    private ReservationResponseType type;
    
    // Used when type == SHOW_AVAILABILITY
    private List<LocalTime> availableTimes;
    
    // Used when type == SHOW_SUGGESTIONS
    private Map<LocalDate, List<LocalTime>> suggestedDates;
    private Integer confirmationCode;
    
    public ReservationResponse() {}
    
    public ReservationResponse(int tableNumber) {
    	this.tableNumber = tableNumber;
    }
    
    //constructor for manager to use
    public ReservationResponse(String guestContact,LocalTime startTime,int partySize,int confirmationCode) {
    	this.confirmationCode=confirmationCode;
    	this.guestContact=guestContact;
    	this.time = startTime;
    	this.partySize = partySize;
    }
    
    public ReservationResponse(LocalDate date,int partySize,LocalTime time,int confirmationCode,String userID,String guestContact,ReservationResponseType type) {
    	this.date= date;
    	this.partySize=partySize;
    	this.time = time;
    	this.confirmationCode = confirmationCode;
    	this.userID=userID;
    	this.guestContact = guestContact;
    	this.type = type;
    }
    
    public int getTableNumber() {
    	return this.tableNumber;
    }
    
    public LocalDate getNewDate() {
		return date;
	}

	public int getNewPartySize() {
		return partySize;
	}

	public LocalTime getNewTime() {
		return time;
	}

	public String getNewGuestContact() {
		return guestContact;
	}

	public ReservationResponse(ReservationResponseType type,List<LocalTime> availableTimes,Map<LocalDate, List<LocalTime>> suggestedDates,                                                           
                               Integer confirmationCode) {
        this.type = type;
        this.availableTimes = availableTimes;
        this.suggestedDates = suggestedDates;
        this.confirmationCode=confirmationCode;
    }
    

    public ReservationResponseType getType() {
        return type;
    }

    public List<LocalTime> getAvailableTimes() {
        return availableTimes;
    }

    public Map<LocalDate, List<LocalTime>> getSuggestedDates() {
        return suggestedDates;
    }
    public Integer getConfirmationCode() {
        return confirmationCode;
    }
}