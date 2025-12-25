package database;

import java.sql.*;

public class SeatingDAO {

    // INSERT
    private static final String INSERT_SEATING =
            "INSERT INTO seating (tableID, reservationID, checkInTime, checkOutTime) " +
            "VALUES (?, ?, NOW(), NULL)";

    // UPDATE
    private static final String UPDATE_CHEKOT_BY_SEATING_ID ="UPDATE `seating` SET checkOutTime = NOW() WHERE seatingID = ?";
            
            
    
    //SELECT
    private final String SELECT_OPEN_SEATING_TO_UPDATE = "SELECT seatingID, reservationID "+
    													 "FROM seating "+
    													 "WHERE tableID = ? AND checkOutTime IS NULL "+
    													 "ORDER BY checkInTime DESC "+
    													 "LIMIT 1 FOR UPDATE";

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

   
    public Integer fetchOpenSeatingID(Connection conn,int tableID)throws SQLException {
    	try(PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_SEATING_TO_UPDATE)){
    		ps.setInt(1, tableID);
    		ResultSet rs = ps.executeQuery();
    		if(!rs.next()) return null;
    		return rs.getInt("seatingID");
    	}
    }
    
    public Integer findOpenSeatingReservationId(Connection conn, int tableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_SEATING_TO_UPDATE)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt("reservationID");
            }
        }
    }
    
    public boolean checkOutBySeatingId(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_CHEKOT_BY_SEATING_ID)) {
            ps.setInt(1, seatingId);
            return ps.executeUpdate() == 1;
        }
    }
}
