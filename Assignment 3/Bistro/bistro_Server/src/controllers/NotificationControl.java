package controllers;

import entities.User;

public class NotificationControl {

    /**
     * Sends the confirmation code to a user via both email and SMS (if present).
     */
    public boolean sendConfirmationToUser(User user, int confirmationCode) {
        if (user == null) return false;

        String subject = "Reservation Confirmation";
        String message = "Your reservation confirmation code is: " + confirmationCode;

        String email = user.getEmail();
        if (email != null && !email.isBlank()) {
            return sendEmail(email, subject, message);
        }
        String phone = user.getPhone();
        if (phone != null && !phone.isBlank()) {
            return sendSms(phone, message);
        }
        return false;
    }

    /**
     * Sends the confirmation code to a guest via the contact method they provided.
     * guestContact may be an email or a phone number.
     */
    public boolean sendConfirmationToGuest(String guestContact, int confirmationCode) {
        if (guestContact == null || guestContact.isBlank()) return false;

        String subject = "Reservation Confirmation";
        String message = "Your reservation confirmation code is: " + confirmationCode;

        if (looksLikeEmail(guestContact)) {
            return sendEmail(guestContact, subject, message);
        } else {
            return sendSms(guestContact, message);
        }
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
}
