package controllers;

import entities.User;

/**
 * Notification service/controller used to simulate sending messages to users and guests.
 *
 * <p>Main idea:
 * Centralizes outgoing notifications (email/SMS/confirmation codes/billing/waiting-list updates).
 * In the current project stage, all methods are stubbed: they print to the console and return {@code true}.
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Automatic reminders (2 hours prior) via email/SMS</li>
 *   <li>Sending reservation confirmation codes to registered users or guests</li>
 *   <li>Sending bills and bill confirmations to users/guests</li>
 *   <li>Sending cancellation messages</li>
 *   <li>Inviting guests/users to a table</li>
 *   <li>Notifying about entering the waiting list</li>
 * </ul>
 *
 * <p>Includes private helper stubs for basic email detection and placeholder implementations
 * of email/SMS sending.
 */
public class NotificationControl {
	
	public boolean sendAutomaticEmailTwoHourPrior(String email) {
			
			System.out.println("Sending email reminder to "+email);
			return true;
	}
	public boolean sendAutomaticSMSTwoHourPrior(String phoneNumber) {
		
		System.out.println("Sending SMS reminder to "+phoneNumber);
		return true;
}
	
    /**
     * Sends the confirmation code to a user via both email and SMS (if present).
     */
    public boolean sendConfirmationToUser(User user, int confirmationCode) {
    	String username=user.getUsername();
    	System.out.println("Hey "+username+"\nYour confirmation code is: "+confirmationCode);
    	return true;
    }
    /**
     * Sends the confirmation code to a guest via the contact method they provided.
     * guestContact may be an email or a phone number.
     */
    public boolean sendConfirmationToGuest(String guestContact, int confirmationCode) {
    	System.out.println("Hey "+guestContact+ "\nYour confirmation code is: "+confirmationCode);
    	return true;
    }

    /**
     * Very lightweight email detection.
     * (No strict validation; just enough to route email vs SMS.)
     */
    private boolean looksLikeEmail(String value) {
        return value.contains("@") && value.contains(".");
    }

    /**
     * Email sending implementation.
     * Replace this with JavaMail / SMTP / provider API when needed.
     */
    private boolean sendEmail(String to, String subject, String body) {
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
        return true;
    }
    /**
     * SMS sending implementation.
     * Replace this with an SMS provider API when needed.
     */
    private boolean sendSms(String phoneNumber, String message) {
        System.out.println("[SMS] To: " + phoneNumber);
        System.out.println("[SMS] Message: " + message);
        return true;
    }

	public boolean sendBillToGuest(String guestContact, String billMessage,double bill) {
		System.out.println("SentBillToGuest");
		return true;
	}
	
	public boolean sendBillToUser(User user, String billMessage,double bill) {
		System.out.println("SentBillToUser");
		return true;
	}
	public boolean sendBillConfirmationToGuest(String guestContact, String billMessage) {
		System.out.println("SentBillConfirmationToGuest");
		return true;
	}
	public boolean sendBillConfirmationToUser(User user, String billMessage) {
		System.out.println("SentBillConfirmationToUser");
		return true;
	}
	
	public boolean sendCancelledReservation(String contact,String msg) {
		System.out.println(msg);
		return true;
	}
	public boolean sendInviteToTable(String email,String phone, String msg) {
		System.out.println(msg);
		return true;
		
	}
	public boolean sendInviteToTable(String guestContact, String msg) {
		System.out.println(msg);
		return true;
	}
	public boolean sendNotificationEnteringWaitingList(String guestContact,String msg) {
		System.out.println(msg);
		return true;
	}
	public boolean sendNotificationEnteringWaitingList(String email,String phone,String msg) {
		System.out.println(msg);
		return true;
	}
}
