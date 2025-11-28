package application;   // or whatever your main package is

import controllers.ReservationController;

import java.sql.Date;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {

        ReservationController controller = new ReservationController();

        try {
            System.out.println("=== BEFORE UPDATE ===");
            controller.printDB();

            int orderNumber = 1;          // <-- put an existing reservationID here
            int newGuests   = 5;
            Date newDate    = Date.valueOf("2025-01-30"); // yyyy-MM-dd

            boolean guestsOk = controller.updateNumberOfGuests(orderNumber, newGuests);
            System.out.println("Update guests result: " + guestsOk);

            boolean dateOk = controller.updateOrderDate(orderNumber, newDate);
            System.out.println("Update date result: " + dateOk);

            System.out.println("\n=== AFTER UPDATE ===");
            controller.printDB();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
