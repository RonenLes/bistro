package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class Reservation {
	
	private String reservationID;
	private LocalDate reservationDate;
	private String status; //'NEW', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW
	private int partySize;
	private int confirmationCode;
	private String guestContact;
	private String userID;
	private LocalTime startTime;
	
	
	
	public Reservation(String reservationID, LocalDate reservationDate, String status, int partySize,
			int confirmationCode, String guestContact, String userID, LocalTime startTime) {
		super();
		this.reservationID = reservationID;
		this.reservationDate = reservationDate;
		this.status = status;
		this.partySize = partySize;
		this.confirmationCode = confirmationCode;
		this.guestContact = guestContact;
		this.userID = userID;
		this.startTime = startTime;
	}


	public String getReservationID() {
		return reservationID;
	}


	public LocalDate getReservationDate() {
		return reservationDate;
	}


	public String getStatus() {
		return status;
	}


	public int getPartySize() {
		return partySize;
	}


	public int getConfirmationCode() {
		return confirmationCode;
	}


	public String getGuestContact() {
		return guestContact;
	}


	public String getUserID() {
		return userID;
	}


	public LocalTime getStartTime() {
		return startTime;
	}


	
	
}
