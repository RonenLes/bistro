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

    // --- For assertions in tests (editReservation) ---
    public String lastUpdate_status;
    public int lastUpdate_partySize;
    public int lastUpdate_confirmationCode;
    public String lastUpdate_guestContact;
    public String lastUpdate_userID;
    public LocalDate lastUpdate_date;
    public LocalTime lastUpdate_startTime;

    /** Add a reservation record (used by tests to pre-load state). */
    public void addReservation(Reservation r) {
        byCode.put(r.getConfirmationCode(), r);
    }

    /** Alias helper: used by editReservation tests. */
    public void putExistingReservation(int confirmationCode, Reservation r) {
        byCode.put(confirmationCode, r);
    }

    /** Configure what getBookedTablesByCapacity should return for a specific slot. */
    public void setBooked(LocalDate date, LocalTime start, LocalTime end, Map<Integer, Integer> booked) {
        bookedBySlot.put(key(date, start, end), new HashMap<>(booked));
    }

    /** Used by confirmation-code generator logic. */
    public Reservation isConfirmationCodeUsed(int confirmationCode) throws SQLException {
        return byCode.get(confirmationCode);
    }

    /** Used by editReservation: fetch reservation by confirmation code. */
    public Reservation getReservationByConfirmationCode(int confirmationCode) throws SQLException {
        return byCode.get(confirmationCode);
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

    /**
     * Stub for editReservation update.
     * Matches the signature your ReservationControl calls:
     * updateReservation(newDate, status, newPartySize, confirmationCode, guestContact, userID, newStartTime)
     */
    public boolean updateReservation(LocalDate newDate,
                                     String status,
                                     int newPartySize,
                                     int confirmationCode,
                                     String guestContact,
                                     String userID,
                                     LocalTime newStartTime) throws SQLException {

        // record for test asserts
        lastUpdate_date = newDate;
        lastUpdate_status = status;
        lastUpdate_partySize = newPartySize;
        lastUpdate_confirmationCode = confirmationCode;
        lastUpdate_guestContact = guestContact;
        lastUpdate_userID = userID;
        lastUpdate_startTime = newStartTime;

        // simulate DB update: update the in-memory object if exists
        Reservation existing = byCode.get(confirmationCode);
        if (existing == null) return false;

        
        Reservation updated = new Reservation(
                existing.getReservationID(),
                newDate,
                status,
                newPartySize,
                existing.getAllocatedCapacity(), // capacity typically recomputed elsewhere
                confirmationCode,
                guestContact,
                userID,
                newStartTime
        );

        byCode.put(confirmationCode, updated);
        return true;
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
