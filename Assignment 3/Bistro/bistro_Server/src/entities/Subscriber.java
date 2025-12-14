package entities;

import java.time.LocalDate;

/**
 * class to store subscriber details that will be fetched from the database
 */

public class Subscriber {
	
	private String subscriberID;
	private String username;
	private String email;
	private String phoneNumber;
	private int cardCode;
	private LocalDate dateOfJoining;
	
	

	public Subscriber(String subscriberID, String username, String email, String phoneNumber, int cardCode,
			LocalDate dateOfJoining) {
		super();
		this.subscriberID = subscriberID;
		this.username = username;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.cardCode = cardCode;
		this.dateOfJoining = dateOfJoining;
	}
	public String getSubscriberID() {
		return subscriberID;
	}
	public void setSubscriberID(String subscriberID) {
		this.subscriberID = subscriberID;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public int getCardCode() {
		return cardCode;
	}
	public void setCardCode(int cardCode) {
		this.cardCode = cardCode;
	}
	public LocalDate getDateOfJoining() {
		return dateOfJoining;
	}
	public void setDateOfJoining(LocalDate dateOfJoining) {
		this.dateOfJoining = dateOfJoining;
	}
}
