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
		 CANCEL_RESERVATION;
	}
	
	private LocalDate newDate;
	private int newPartySize;
	private LocalTime newTime;
	private String newGuestContact;
	
    private ReservationResponseType type;
    
    // Used when type == SHOW_AVAILABILITY
    private List<LocalTime> availableTimes;
    
    // Used when type == SHOW_SUGGESTIONS
    private Map<LocalDate, List<LocalTime>> suggestedDates;
    private Integer confirmationCode;
    
    public ReservationResponse() {}
    
    public ReservationResponse(LocalDate newDate,int newPartySize,LocalTime newTime,int confirmationCode,String newGuestContact,ReservationResponseType type) {
    	this.newDate= newDate;
    	this.newPartySize=newPartySize;
    	this.newTime = newTime;
    	this.confirmationCode = confirmationCode;
    	this.newGuestContact = newGuestContact;
    	this.type = type;
    }
    
    public ReservationResponse(ReservationResponseType type,
                               List<LocalTime> availableTimes,
                               Map<LocalDate, List<LocalTime>> suggestedDates,
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
