package responses;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservationResponse {

	
	private int confirmationCode;
	private LocalDate reservationDate;
	private LocalTime reservationTime;
	
	public ReservationResponse() {
		
	}
	
	public ReservationResponse(int confirmationCode, LocalDate reservationDate, LocalTime reservationTime) {
		this.confirmationCode = confirmationCode;
		this.reservationDate = reservationDate;
		this.reservationTime = reservationTime;
	}
	public int getConfirmationCode() {
		return confirmationCode;
	}
	public LocalDate getReservationDate() {
		return reservationDate;
	}
	public LocalTime getReservationTime() {
		return reservationTime;
	}
	
	
}
