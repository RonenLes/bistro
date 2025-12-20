package dao_stubs;

import controllers.NotificationControl;
import entities.User;

public class NotificationControlStub extends NotificationControl {

    public int lastCode = -1;
    public String lastGuestContact = null;
    public String lastUserId = null;

    @Override
    public void sendConfirmationToUser(User user, int confirmationCode) {
        lastUserId = user.getUserID();
        lastCode = confirmationCode;
        System.out.println("sendConfirmationToUser was used:"+user.getUsername()+" "+confirmationCode+"\n");
    }

    @Override
    public void sendConfirmationToGuest(String guestContact, int confirmationCode) {
        lastGuestContact = guestContact;
        lastCode = confirmationCode;
        System.out.println("sendConfirmationToGuest was used:"+lastGuestContact+" "+confirmationCode+"\n");
    }
}
