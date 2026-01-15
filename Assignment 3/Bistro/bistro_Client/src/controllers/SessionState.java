package controllers;

// holds current user session data
// tracks both subscriber sessions and guest sessions
public class SessionState {
    // true if user is browsing as guest (not logged in)
    private boolean guestSession;
    // phone or email used to identify guest
    private String guestContact;
    // unique subscriber ID from database
    private String currentUserId;
    // logged-in subscriber username
    private String currentUsername;
    // username from last login attempt (fallback)
    private String lastLoginUsername;
    // subscriber email
    private String currentEmail;
    // subscriber phone
    private String currentPhone;

    // clears all session data on logout
    public void reset() {
        guestSession = false;
        guestContact = null;
        currentUserId = null;
        currentUsername = null;
        lastLoginUsername = null;
        currentEmail = null;
        currentPhone = null;
    }

    // attempts to retrieve username with fallback logic
    // used when server doesn't return username in response
    public String resolveUsername() {
        if (currentUsername != null && !currentUsername.isBlank()) {
            return currentUsername.trim();
        }
        if (lastLoginUsername != null && !lastLoginUsername.isBlank()) {
            return lastLoginUsername.trim();
        }
        return null;
    }

    // standard getters and setters for session fields
    
    public boolean isGuestSession() {
        return guestSession;
    }

    public void setGuestSession(boolean guestSession) {
        this.guestSession = guestSession;
    }

    public String getGuestContact() {
        return guestContact;
    }

    public void setGuestContact(String guestContact) {
        this.guestContact = guestContact;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public String getLastLoginUsername() {
        return lastLoginUsername;
    }

    public void setLastLoginUsername(String lastLoginUsername) {
        this.lastLoginUsername = lastLoginUsername;
    }

    public String getCurrentEmail() {
        return currentEmail;
    }

    public void setCurrentEmail(String currentEmail) {
        this.currentEmail = currentEmail;
    }

    public String getCurrentPhone() {
        return currentPhone;
    }

    public void setCurrentPhone(String currentPhone) {
        this.currentPhone = currentPhone;
    }
}