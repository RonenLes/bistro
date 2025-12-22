package database;

import java.sql.*;

public class SeatingDAO {

    // INSERT
    private static final String INSERT_SEATING =
            "INSERT INTO seating (tableID, reservationID, checkInTime, checkOutTime) " +
            "VALUES (?, ?, NOW(), NULL)";

    // UPDATE
    private static final String UPDATE_SEATING_CHECKOUT_BY_TABLE =
            "UPDATE seating SET checkOutTime = NOW() " +
            "WHERE tableID = ? AND checkOutTime IS NULL " +
            "LIMIT 1";

    /**
     * Inserts a check-in row (opens its own connection).
     * @return seatingID (generated key) or -1 if failed.
     */
    public int checkIn(int tableID, int reservationID) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            return checkIn(conn, tableID, reservationID);
        }
    }

    /**
     * Inserts a check-in row using an existing connection (for transactions).
     * @return seatingID (generated key) or -1 if failed.
     */
    public int checkIn(Connection conn, int tableID, int reservationID) throws SQLException {
        try (PreparedStatement ps =
                     conn.prepareStatement(INSERT_SEATING, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, tableID);
            ps.setInt(2, reservationID); // if you want to allow walk-in: use ps.setNull(2, Types.INTEGER)

            int affected = ps.executeUpdate();
            if (affected != 1) return -1;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    /**
     * Checks out a table (opens its own connection).
     */
    public boolean checkOutByTable(int tableId) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            return checkOutByTable(conn, tableId);
        }
    }

    /**
     * Checks out a table using an existing connection (for transactions).
     */
    public boolean checkOutByTable(Connection conn, int tableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_SEATING_CHECKOUT_BY_TABLE)) {
            ps.setInt(1, tableId);
            return ps.executeUpdate() == 1;
        }
    }
}
