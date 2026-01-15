package controllers;

// dependency injection interface for UI controllers
// allows screens to receive controller reference and connection status
public interface ClientControllerAware {
    // called during screen initialization to wire up controller
    void setClientController(ClientController controller, boolean connected);
}