package dao_stubs;

import database.ReservationDAO;
import entities.Reservation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory stub for ReservationDAO (no DB).
 */
public class ReservationDAOStub extends ReservationDAO {

    // confirmationCode -> Reservation
    private final Map<Integer, Reservation> byCode = new HashMap<>();

    // (date,start,end) -> booked map (allocatedCapacity -> bookedCount)
    private final Map<String, Map<Integer, Integer>> bookedBySlot = new HashMap<>();

    private int nextReservationId = 1;

    /** Pre-load a reservation into the stub (so edit/cancel/show can find it). */
    public void addReservation(Reservation r) {
        byCode.put(r.getConfirmationCode(), r);
    }

    /** Configure what getBookedTablesByCapacity returns for a slot. */
    public void setBooked(LocalDate date, LocalTime start, LocalTime end, Map<Integer, Integer> booked) {
        bookedBySlot.put(key(date, start, end), new HashMap<>(booked));
    }

    @Override
    public int insertNewReservation(LocalDate reservationDate, int numberOfGuests, int allocatedCapacity,
                                        int confirmationCode, String userID, LocalTime startTime,
                                        String status, String guest) throws SQLException {

        if (byCode.containsKey(confirmationCode)) return 100;

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
        return 0;
    }

    @Override
    public Reservation getReservationByConfirmationCode(int code) throws SQLException {
        return byCode.get(code);
    }

    @Override
    public Reservation getReservationByConfirmationCode(Connection conn, int confirmationCode) throws SQLException {
        // connection is irrelevant in stub
        return byCode.get(confirmationCode);
    }

    @Override
    public boolean updateReservation(LocalDate reservationDate, String status, int partySize, int confirmationCode,
                                     String guestContact, String userID, LocalTime startTime) throws SQLException {

        Reservation existing = byCode.get(confirmationCode);
        if (existing == null) return false;

        // allocatedCapacity is not part of your DAO update signature -> keep existing allocation
        Reservation updated = new Reservation(
                existing.getReservationID(),
                reservationDate,
                status,
                partySize,
                existing.getAllocatedCapacity(),
                confirmationCode,
                guestContact,
                userID,
                startTime
        );

        byCode.put(confirmationCode, updated);
        return true;
    }

    @Override
    public boolean updateStatus(int confirmationCode, String status) throws SQLException {
        Reservation existing = byCode.get(confirmationCode);
        if (existing == null) return false;

        Reservation updated = new Reservation(
                existing.getReservationID(),
                existing.getReservationDate(),
                status,
                existing.getPartySize(),
                existing.getAllocatedCapacity(),
                existing.getConfirmationCode(),
                existing.getGuestContact(),
                existing.getUserID(),
                existing.getStartTime()
        );

        byCode.put(confirmationCode, updated);
        return true;
    }

    @Override
    public boolean updateStatus(Connection conn, int confirmationCode, String status) throws SQLException {
        // delegate to the no-conn version
        return updateStatus(confirmationCode, status);
    }

    @Override
    public Map<Integer, Integer> getBookedTablesByCapacity(LocalDate date, LocalTime start, LocalTime end)
            throws SQLException {

        Map<Integer, Integer> configured = bookedBySlot.get(key(date, start, end));
        if (configured != null) return new HashMap<>(configured);

        // default compute from stored reservations
        Map<Integer, Integer> counts = new HashMap<>();

        for (Reservation r : byCode.values()) {
            if (!date.equals(r.getReservationDate())) continue;
            if (!isActive(r.getStatus())) continue;

            LocalTime rStart = r.getStartTime();
            LocalTime rEnd = rStart.plusHours(2);

            boolean overlap = start.isBefore(rEnd) && end.isAfter(rStart);
            if (!overlap) continue;

            counts.merge(r.getAllocatedCapacity(), 1, Integer::sum);
        }

        return counts;
    }

    private boolean isActive(String status) {
        if (status == null) return false;
        String s = status.toUpperCase();
        return s.equals("NEW") || s.equals("CONFIRMED") || s.equals("SEATED");
    }

    private String key(LocalDate d, LocalTime s, LocalTime e) {
        return d + "|" + s + "|" + e;
    }
}
