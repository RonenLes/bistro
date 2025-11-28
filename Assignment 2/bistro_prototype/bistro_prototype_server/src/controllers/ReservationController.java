package controllers;
import db.ReservationDB;
import entities.Reservation;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.ReservationRequest;

public class ReservationController {

    private ReservationDB reservationDB;

    public ReservationController() {
        this.reservationDB = new ReservationDB();
    }

    // Print all reservations from DB
    public void printDB() { 
        List<Reservation> reservationsList = new ArrayList<>();
        
        // Attempt to get all reservations
        try {
            reservationsList = this.reservationDB.readAllReservations();

            for (Reservation res : reservationsList) {
                System.out.printf(
                        "Reservation ID: %d | Arrival: %s | Guests: %d | Confirmation: %d | Subscriber: %d | Placed On: %s%n",
                        res.getReservationID(),
                        res.getReservationDate(),
                        res.getNumberOfGuests(),
                        res.getConfirmationCode(),
                        res.getSubscriberId(),
                        res.getDateOfPlacingOrder()
                );
            }
        } 
        catch (SQLException e) {
            // Handle the error
            System.err.println("Error reading reservations from the database: " + e.getMessage());
            // Uncomment for easy debug
            // e.printStackTrace(); 
        }
    }
 
    public boolean updateNumberOfGuests(int orderNumber, int newGuestAmount) {
        try {
            return reservationDB.updateNumberOfGuests(orderNumber, newGuestAmount);
        } catch (SQLException e) {
            return false;
        }
    }
    
    public boolean updateOrderDate(int orderNumber, Date newDate) {
        try {
            return reservationDB.updateOrderDate(orderNumber, newDate);
        } catch (SQLException e) {
            return false;
        }
    }
    /**
     * Adds a reservation to the database.
     * (You can call checkReservation(reservation) here if you want to validate first)
     */
    public boolean addReservation(ReservationRequest reservation) {
        try {

            int reservationID = 0; // Placeholder: Needs proper ID generation logic
            int confirmationCode = 0; // Placeholder: Needs proper code generation logic

            boolean inserted = reservationDB.insertNewReservation(
                    reservationID, 
                    reservation.getDateofRequest(), 
                    reservation.getDinersCount(), 
                    confirmationCode, 
                    reservation.getCustomerInfo(), 
                    reservation.getDateOfPlacingRequest()
            );

            if (!inserted) {
                System.err.println("Insert failed – no row affected.");
            }

            return inserted;

        } catch (SQLException e) {
            // The catch block handles the checked SQLException
            System.err.println("Database error while inserting reservation:");
            e.printStackTrace(); // Prints the full error stack trace for debugging
            return false;
        }
    }
}
