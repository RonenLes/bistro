package requests;

import java.io.Serializable;
import java.time.LocalDate;

public class ReservationRequest implements Serializable{
	
	private int subscriberId;
	private LocalDate dateOfRequest;
	private int dinersCount;
	private LocalDate dateOfplacingRequest;
	
	public ReservationRequest(int subscriberId, LocalDate dateOfRequest, int dinersCount) {
		this.subscriberId = subscriberId;
		this.dateOfRequest = dateOfRequest;
		this.dinersCount = dinersCount;
		this.dateOfplacingRequest = LocalDate.now();
	}
	
	public int getSubscriberId() {
		return this.subscriberId;
	}
	
	public LocalDate getDateofRequest() {
		return this.dateOfRequest;
	}
	
	public int getDinersCount() {
		return this.dinersCount;
	}
	
	public LocalDate getDateOfPlacingRequest() {
		return this.dateOfplacingRequest;
	}
}
