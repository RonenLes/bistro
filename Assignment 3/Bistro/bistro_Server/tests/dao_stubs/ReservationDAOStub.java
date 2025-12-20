package dao_stubs;

import database.ReservationDAO;
import entities.Reservation;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Stub DAO for ReservationDAO (no DB).
 * Stores reservations in-memory and simulates queries.
 */
public class ReservationDAOStub extends ReservationDAO {

    // confirmationCode -> Reservation
    private final Map<Integer, Reservation> byCode = new HashMap<>();

    // (date,start,end) -> booked map (allocatedCapacity -> bookedCount)
    private final Map<String, Map<Integer, Integer>> bookedBySlot = new HashMap<>();

    private int nextReservationId = 1;

    /** Add a reservation record (used by tests to pre-load state). */
    public void addReservation(Reservation r) {
        byCode.put(r.getConfirmationCode(), r);
    }

    /** Configure what getBookedTablesByCapacity should return for a specific slot. */
    public void setBooked(LocalDate date, LocalTime start, LocalTime end, Map<Integer, Integer> booked) {
        bookedBySlot.put(key(date, start, end), new HashMap<>(booked));
    }

    @Override
    public boolean insertNewReservation(LocalDate reservationDate, int numberOfGuests, int allocatedCapacity,
                                        int confirmationCode, String userID, LocalTime startTime,
                                        String status, String guest) throws SQLException {

        // simulate "unique confirmation code" constraint
        if (byCode.containsKey(confirmationCode)) return false;

        Reservation r = new Reservation(
                nextReservationId++,
                reservationDate,
                status,
                numberOfGuests,
                allocatedCapacity,
                confirmationCode,
                guest,
                userID,
                startTime
        );

        byCode.put(confirmationCode, r);
        return true;
    }

    @Override
    public boolean updateReservation(int newGuests, LocalDate newDate, int confirmationCode,
                                     LocalTime newTime, int newAllocation) throws SQLException {

        Reservation existing = byCode.get(confirmationCode);
        if (existing == null) return false;

        // simplest approach: replace with new Reservation instance (immutable-style)
        Reservation updated = new Reservation(
                existing.getReservationID(),
                newDate,
                existing.getStatus(),
                newGuests,
                newAllocation,
                existing.getConfirmationCode(),
                existing.getGuestContact(),
                existing.getUserID(),
                newTime
        );

        byCode.put(confirmationCode, updated);
        return true;
    }

    @Override
    public Reservation isConfirmationCodeUsed(int confirmationCode) throws SQLException {
        return byCode.get(confirmationCode);
    }

    @Override
    public Map<Integer, Integer> getBookedTablesByCapacity(LocalDate date, LocalTime start, LocalTime end)
            throws SQLException {

        Map<Integer, Integer> configured = bookedBySlot.get(key(date, start, end));
        if (configured != null) return new HashMap<>(configured);

        // default behavior (if you didn't setBooked): compute counts from stored reservations
        Map<Integer, Integer> counts = new HashMap<>();

        for (Reservation r : byCode.values()) {
            if (!r.getReservationDate().equals(date)) continue;
            if (!isActive(r.getStatus())) continue;

            // overlap: [start,end) with [r.start, r.start+2h)
            LocalTime rStart = r.getStartTime();
            LocalTime rEnd = rStart.plusHours(2);

            boolean overlap = start.isBefore(rEnd) && end.isAfter(rStart);
            if (!overlap) continue;

            counts.merge(r.getAllocatedCapacity(), 1, Integer::sum);
        }

        return counts;
    }

    private boolean isActive(String status) {
        return "NEW".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);
    }

    private String key(LocalDate d, LocalTime s, LocalTime e) {
        return d + "|" + s + "|" + e;
    }
}
