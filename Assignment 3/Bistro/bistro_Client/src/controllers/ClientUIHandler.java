package controllers;

//Interface for desktopUI or terminalUI to implement
public interface ClientUIHandler {
    void showInfo(String title, String message);
    void showWarning(String title, String message);
    void showError(String title, String message);

    // Optional: if you want a generic “data screen” hook (?) do we want it Ronen? DO WE?
    
    void showPayload(Object payload);
}
