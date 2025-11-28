package responses;

import java.io.Serializable;

import entities.Reservation;

public class ReservationResponse implements Serializable {

	private boolean isReservationSuccess;
	private String msg;
	private Reservation reservation;
	
	
	public ReservationResponse(boolean isReservationSuccess, String msg, Reservation reservation) {
		this.isReservationSuccess= isReservationSuccess;
		this.msg= msg;
		this.reservation = reservation;
	}
	
	public boolean getIsReservationSuccess() {
		return this.isReservationSuccess;
	}
	
	public String msg() {
		return this.msg;
	}
	
	public int getConfirmationCode() {
		return this.reservation.getConfirmationCode();
	}
}
