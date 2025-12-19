package controllers;

import entities.Subscriber;

public class NotificationControl {

    /**
     * Sends the confirmation code to a subscriber via both email and SMS (if present).
     */
    public void sendConfirmationToSubscriber(Subscriber subscriber, int confirmationCode) {
        if (subscriber == null) return;

        String subject = "Reservation Confirmation";
        String message = "Your reservation confirmation code is: " + confirmationCode;

        String email = subscriber.getEmail();
        if (email != null && !email.isBlank()) {
            sendEmail(email, subject, message);
        }

        String phone = subscriber.getPhoneNumber();
        if (phone != null && !phone.isBlank()) {
            sendSms(phone, message);
        }
    }

    /**
     * Sends the confirmation code to a guest via the contact method they provided.
     * guestContact may be an email or a phone number.
     */
    public void sendConfirmationToGuest(String guestContact, int confirmationCode) {
        if (guestContact == null || guestContact.isBlank()) return;

        String subject = "Reservation Confirmation";
        String message = "Your reservation confirmation code is: " + confirmationCode;

        if (looksLikeEmail(guestContact)) {
            sendEmail(guestContact, subject, message);
        } else {
            sendSms(guestContact, message);
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
    private void sendEmail(String to, String subject, String body) {
        System.out.println("[EMAIL] To: " + to);
        System.out.println("[EMAIL] Subject: " + subject);
        System.out.println("[EMAIL] Body: " + body);
    }

    /**
     * SMS sending implementation.
     * Replace this with an SMS provider API when needed.
     */
    private void sendSms(String phoneNumber, String message) {
        System.out.println("[SMS] To: " + phoneNumber);
        System.out.println("[SMS] Message: " + message);
    }
}
