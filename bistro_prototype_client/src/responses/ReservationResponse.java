package responses;

import java.io.Serializable;

import entities.Reservation;

public class ReservationResponse implements Serializable {

	private boolean isReservationSuccess;
	private String msg;
	private int code;
	
	
	public ReservationResponse(boolean isReservationSuccess, String msg, int code) {
		this.isReservationSuccess= isReservationSuccess;
		this.msg= msg;
		this.code = code;
	}
	
	public boolean getIsReservationSuccess() {
		return this.isReservationSuccess;
	}
	
	public String getMsg() {
		return this.msg;
	}
	
	public int getConfirmationCode() {
		return this.code;
	}
}
