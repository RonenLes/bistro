package entities;

import java.io.Serializable;
import java.time.LocalDate;

public class Reservation implements Serializable{
	
	private long reservationID;
	private LocalDate reservationDate;
	private int numberOfGuests;
	private int confirmationCode;
	private int subscriberId;
	private LocalDate dateOfPlacingOrder;
	
	public Reservation(long reservationID,LocalDate reservationDate,int numberOfGuests,int confirmationCode,int subscriberId,LocalDate dateOfPlacingOrder) {
		this.reservationID = reservationID;
		this.reservationDate = reservationDate;
		this.numberOfGuests = numberOfGuests;
		this.confirmationCode = confirmationCode;
		this.subscriberId = subscriberId;
		this.dateOfPlacingOrder =dateOfPlacingOrder;
	}
	
	
	//getters----------------------------------------------------------------
	public long getReservationID() {
		return this.reservationID;
	}
	
	public LocalDate getReservationDate() {
		return this.reservationDate;
	}
	
	public int getNumberOfGuests() {
		return this.numberOfGuests;
	}
	
	public int getConfirmationCode() {
		return this.confirmationCode;
	}
	
	public int getSubscriberId() {
		return this.subscriberId;
	}
	
	public LocalDate getDateOfPlacingOrder() {
		return this.dateOfPlacingOrder;
	}
	
	//setters------------------------------------------------------------------
	public void setReservationID(long id) {
		this.reservationID=id;
	}
	
	public void setReservationDate(LocalDate date) {
		this.reservationDate=date;
	}
	
	public void setNumberOfGuests(int guests) {
		this.numberOfGuests=guests;
	}
	
	public void setConfirmationCode(int code) {
		this.confirmationCode=code;
	}
	
	public void setSubscriberId(int id) {
		this.subscriberId=id;
	}
	
	public void setDateOfPlacingOrder(LocalDate date) {
		this.dateOfPlacingOrder=date;
	}
	
	//toString
	public String toString() {
		return "reservationID: " + reservationID +
				" reservation date: " + reservationDate.toString() + 
				" number of guests: " + numberOfGuests +
				" confrimation code: " + confirmationCode + 
				" subscriber id: " + subscriberId +
				" date of placing the order: " + dateOfPlacingOrder.toString();
	}
}
