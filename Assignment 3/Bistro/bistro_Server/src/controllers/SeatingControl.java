package controllers;
import java.sql.Connection;

import database.DBManager;
import database.ReservationDAO;
import database.SeatingDAO;
import database.TableDAO;
import entities.Reservation;
import entities.Table;
import responses.Response;

public class SeatingControl {

    private final ReservationDAO reservationDAO;
    private final TableDAO tableDAO;
    private final SeatingDAO seatingDAO;

    public SeatingControl() {
        this(new ReservationDAO(), new TableDAO(), new SeatingDAO());
    }

    public SeatingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO) {
        this.reservationDAO = reservationDAO;
        this.tableDAO = tableDAO;
        this.seatingDAO = seatingDAO;
    }

    public Response<Table> checkInByConfirmationCode(int confirmationCode) {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            // 1) Get reservation
            Reservation r = reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
            if (r == null) {
                conn.rollback();
                return new Response<>(false, "Reservation not found", null);
            }

            // 2) Validate status 
            String st = r.getStatus();
            if (!"NEW".equalsIgnoreCase(st) && !"CONFIRMED".equalsIgnoreCase(st)) {
                conn.rollback();
                return new Response<>(false, "Reservation cannot be checked-in (status=" + st + ")", null);
            }
          
            // 3) Find a free table now (only seating occupancy matters)
            Table table = tableDAO.findAvailableTable(conn, r.getAllocatedCapacity());
            if (table == null) {
                conn.rollback();
                return new Response<>(false, "No available table right now", null);
            }

            // 4) Insert seating (occupy the table)
            int seatingId = seatingDAO.checkIn(conn, table.getTableID(), r.getReservationID());
            if (seatingId == -1) {
                conn.rollback();
                return new Response<>(false, "Failed to create seating record", null);
            }

            // 5) Update reservation status
            reservationDAO.updateStatus(conn, r.getReservationID(), "SEATING");

            conn.commit();
            return new Response<>(true, "Checked-in successfully", table);

        } catch (Exception e) {
            return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
        }
    }

    // Optional helper
    // private boolean isWithinWindow(Reservation r) { ... }
}
