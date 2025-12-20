package dao_stubs;

import database.OpeningHoursDAO;
import entities.OpeningHours;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Stub DAO for OpeningHoursDAO (no DB).
 * Lets tests predefine opening hours per date.
 */
public class OpeningHoursDAOStub extends OpeningHoursDAO {

    private final Map<LocalDate, OpeningHours> openings = new HashMap<>();

    /** Add/override opening hours for a specific date. */
    public void put(LocalDate date, OpeningHours hours) {
        openings.put(date, hours);
    }

    /** Convenience: create and add OpeningHours for a date. */
    public void put(LocalDate date, String day, java.time.LocalTime open, java.time.LocalTime close, String occasion) {
        openings.put(date, new OpeningHours(date, day, open, close, occasion));
    }

    @Override
    public OpeningHours getOpeningHour(LocalDate date) throws SQLException {
        return openings.get(date); // can be null -> represents "closed / not found"
    }
}
