package requests;

import java.time.LocalDate;

public class ReservationRequest {
	
	
	private LocalDate dateToReserve;
	private int partySize;
	public ReservationPhase phase;
	
	public enum ReservationPhase{
		FIRST_PHASE,SECOND_PHASE;
	}
	
	public ReservationRequest() {
		
	}

	public ReservationRequest(LocalDate dateToReserve, int partySize) {
		this.dateToReserve = dateToReserve;
		this.partySize = partySize;
	}

	public LocalDate getDateToReserve() {
		return dateToReserve;
	}

	public int getPartySize() {
		return partySize;
	}

	public ReservationPhase getPhase() {
		return phase;
	}

	public void setPhase(ReservationPhase phase) {
		this.phase = phase;
	}
	
	
	
	
}
