package dao_stubs;

import controllers.NotificationControl;
import entities.User;

public class NotificationControlStub extends NotificationControl {

    public int lastCode = -1;
    public String lastGuestContact = null;
    public String lastUserId = null;

    // New: store the full user object for stronger assertions
    public User lastUser = null;

    @Override
    public boolean sendConfirmationToUser(User user, int confirmationCode) {
        lastUser = user;
        lastUserId = (user != null ? user.getUserID() : null);
        lastCode = confirmationCode;
        System.out.println("sendConfirmationToUser was used: " +
                (user != null ? user.getUsername() : "null") + " " + confirmationCode + "\n");
        return true;
    }

    @Override
    public boolean sendConfirmationToGuest(String guestContact, int confirmationCode) {
        lastGuestContact = guestContact;
        lastCode = confirmationCode;
        System.out.println("sendConfirmationToGuest was used: " +
                lastGuestContact + " " + confirmationCode + "\n");
        return true;
    }
}
