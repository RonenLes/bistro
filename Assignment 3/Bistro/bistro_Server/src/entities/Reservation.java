package entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
	
	private int reservationID;
	private LocalDate reservationDate;
	private String status; //'NEW', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW,'SEATING'
	private int partySize;
	private int allocatedCapacity;
	private int confirmationCode;
	private String guestContact;
	private String userID;
	private LocalTime startTime;
	private LocalDateTime timeOfCreation;
	
	
	
	public Reservation(int reservationID, LocalDate reservationDate, String status, int partySize,int allocatedCapacity,
			int confirmationCode, String guestContact, String userID, LocalTime startTime,LocalDateTime timeOfCreation) {
		this.reservationID = reservationID;
		this.reservationDate = reservationDate;
		this.status = status;
		this.partySize = partySize;
		this.allocatedCapacity= allocatedCapacity;
		this.confirmationCode = confirmationCode;
		this.guestContact = guestContact;
		this.userID = userID;
		this.startTime = startTime;
		this.timeOfCreation = timeOfCreation;
		
	}

	public Reservation(int reservationID,String guestContact,String userID,String status) {
		this.reservationID = reservationID;
		this.guestContact = guestContact;
		this.userID = userID;
		this.status = status;
	}
	
	
	public LocalDateTime getTimeOfCreation() {
		return timeOfCreation;
	}




	public int getReservationID() {
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




	public int getAllocatedCapacity() {
		return allocatedCapacity;
	}


	
	
}
