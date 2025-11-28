package controllers;
import db.ReservationDB;
import entities.Reservation;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import common.ReservationRequest;
import java.util.concurrent.ThreadLocalRandom;

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
     * Helper method to generate a random 10-digit long ID.
     */
    private long generateRandom10DigitId() {
        // ➡️ ACTION 5: Define the 10-digit range
        long min = 1_000_000_000L; 
        long max = 9_999_999_999L; 
        
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }
    
    /**
     * Adds a reservation to the database.
     * (You can call checkReservation(reservation) here if you want to validate first)
     */
    public boolean addReservation(ReservationRequest reservation) {
        
        long reservationID = 0; // Initialize outside the loop
        final int MAX_ATTEMPTS = 5; 
        int attempts = 0;
        boolean unique = false;

        // Loop to generate and verify a unique ID
        do {
            reservationID = generateRandom10DigitId();
            try {
                // Check the database for existence using the new ReservationDB method
                if (!reservationDB.doesReservationIdExist(reservationID)) { 
                    unique = true;
                }
            } catch (SQLException e) {
                // If the DB connection fails during the uniqueness check, abort.
                System.err.println("CRITICAL: Failed to check ID uniqueness against DB. Aborting.");
                // We return false here because we can't trust the uniqueness check.
                return false;
            }
            attempts++;
            // attempt to find unique id
            if (attempts >= MAX_ATTEMPTS && !unique) {
                System.err.println("Failed to find a unique 10-digit ID after " + MAX_ATTEMPTS + " attempts.");
                return false;
            }
        } while (!unique);
        
        // We are keeping this separate from the primary ID for good structure, for now TODO
        int confirmationCode = 0; 

        //Attempt to insert the reservation with the verified unique ID
        try {
            boolean inserted = reservationDB.insertNewReservation(
                reservationID, // Verified unique long ID
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
            // This catch handles errors during the final INSERT (e.g., connection lost)
            System.err.println("Database error while inserting reservation:");
            e.printStackTrace(); 
            return false;
        }
    }

}
