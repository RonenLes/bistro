package entities;

import java.time.LocalDate;

/**
 * class to store subscriber details that will be fetched from the database
 */

public class Subscriber {
	
	private String subscriberID;
	private String userID;	
	private int cardCode;
	private String phoneNumber;
	private String email;
	private LocalDate dateOfJoining;
	public Subscriber() {
		
	}
	public Subscriber(String subscriberID, String userID, int cardCode, String phoneNumber, String email,
			LocalDate dateOfJoining) {
		super();
		this.subscriberID = subscriberID;
		this.userID = userID;
		this.cardCode = cardCode;
		this.phoneNumber = phoneNumber;
		this.email = email;
		this.dateOfJoining = dateOfJoining;
	}
	public String getSubscriberID() {
		return subscriberID;
	}
	public void setSubscriberID(String subscriberID) {
		this.subscriberID = subscriberID;
	}
	public String getUserID() {
		return userID;
	}
	public void setUserID(String userID) {
		this.userID = userID;
	}
	public int getCardCode() {
		return cardCode;
	}
	public void setCardCode(int cardCode) {
		this.cardCode = cardCode;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public LocalDate getDateOfJoining() {
		return dateOfJoining;
	}
	public void setDateOfJoining(LocalDate dateOfJoining) {
		this.dateOfJoining = dateOfJoining;
	}
	
	
	
	
}
