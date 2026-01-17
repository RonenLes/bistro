package database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import requests.TableInfo;
import responses.CurrentSeatingResponse;

/**
 * DAO for the {@code seating} table.
 *
 * <p>Main idea: manages the "seating session" of a reservation on a physical table:
 * inserting check-ins / held seatings, marking check-in time, marking check-out time,
 * fetching current open seatings, and supporting billing automation.</p>
 *
 * <p>Common concepts:
 * <ul>
 *   <li><b>Open seating</b>: a seating row where {@code checkOutTime IS NULL}</li>
 *   <li><b>Held seating</b>: a seating row created with {@code checkInTime = NULL} (reserved/held but not checked in yet)</li>
 *   <li><b>Bill automation</b>: {@code billSent} is used to track whether an auto-bill action was triggered</li>
 * </ul>
 */
public class SeatingDAO {
    // INSERT
    private static final String INSERT_SEATING =
            "INSERT INTO seating (tableID, reservationID, checkInTime, checkOutTime,billSent) " +
            "VALUES (?, ?, NOW(), NULL,0)";
    private static final String INSERT_HELD_SEATING =
    	    "INSERT INTO seating (tableID, reservationID, checkInTime, checkOutTime) " +
    	    "VALUES (?, ?, NULL, NULL)";

    // UPDATE
    private static final String UPDATE_CHEKOUT_BY_SEATING_ID ="UPDATE `seating` SET checkOutTime = NOW() WHERE seatingID = ?";
    private static final String CLAIM_AUTO_BILL_SEND ="UPDATE seating SET billSent = 2 " +"WHERE seatingID = ? AND billSent = 0 AND checkOutTime IS NULL " +
    	    "AND checkInTime <= DATE_SUB(NOW(), INTERVAL 2 HOUR)";
    private static final String UPDATE_BILL_SENT =
    	    "UPDATE seating SET billSent = ? WHERE seatingID = ?";    
        
    private static final String UPDATE_CHECKIN_TIME_NOW =
            "UPDATE seating " +
            "SET checkInTime = NOW() " +
            "WHERE seatingID = ? AND checkOutTime IS NULL AND checkInTime IS NULL";
    
    //SELECT
    private static final String SELECT_TABLE_ID_BY_SEATING_ID ="SELECT tableID FROM seating WHERE seatingID = ?";        
    private static final String SELECT_VISIT_TIMES_BETWEEN ="SELECT checkInTime, checkOutTime " +"FROM seating " +"WHERE checkInTime >= ? " +
    														"AND checkInTime < ? " +
    														"AND checkOutTime IS NOT NULL " +
    														"ORDER BY checkInTime";
    private final String SELECT_CURRENT_SEATINGS ="SELECT s.seatingID,t.tableNumber, t.capacity, s.checkInTime, s.checkOutTime, "+
    											  "DATE_ADD(s.checkInTime, INTERVAL 2 HOUR) AS estimatedCheckOutTime, r.reservationID, r.confirmationCode, r.partySize, "+
    											  "r.userID, r.guestContact, u.username "+
    											  "FROM seating s JOIN restaurant_table t ON t.tableID = s.tableID "+
    											  "JOIN reservation r ON r.reservationID = s.reservationID "+
    											  "LEFT JOIN user u ON u.userID = r.userID "+
    											  "WHERE s.checkOutTime IS NULL "+
    											  "ORDER BY t.tableNumber ASC";
    private final String SELECT_WHERE_CHECKIN_NULL= "SELECT checkInTime FROM seating WHERE seatingID = ?";
    
    private final String SELECT_OPEN_SEATING_TO_UPDATE = "SELECT seatingID, reservationID "+
    													 "FROM seating "+
    													 "WHERE tableID = ? AND checkOutTime IS NULL "+
    													 "ORDER BY checkInTime DESC "+
    													 "LIMIT 1 FOR UPDATE";
    private static final String SELECT_SEATINGS_DUE_FOR_BILL ="SELECT seatingID " +"FROM seating " +"WHERE checkOutTime IS NULL " +"AND billSent = 0 " +"AND checkInTime <= DATE_SUB(NOW(), INTERVAL 2 HOUR)";
    private static final String SELECT_RESERVATION_ID_BY_SEATING_ID = "SELECT reservationID FROM seating WHERE seatingID = ?";
    private static final String SELECT_SEATING_ID_BY_RESERVATION_ID ="SELECT seatingID " +"FROM seating " +"WHERE reservationID = ? " +"AND checkOutTime IS NULL " +
    																 "ORDER BY checkInTime DESC " +
    																 "LIMIT 1";
    
    
    /**
     * Inserts a "held seating" row for a table/reservation pair.
     *
     * <p>A held seating means {@code checkInTime} is NULL (the table is held but not checked-in yet).</p>
     *
     * @param conn active JDBC connection
     * @param tableId table foreign key
     * @param reservationId reservation foreign key
     * @return generated {@code seatingID} on success, or {@code -1} if insert failed
     * @throws SQLException if a DB error occurs
     */
    public int insertHeldSeating(Connection conn, int tableId, int reservationId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_HELD_SEATING, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tableId);
            ps.setInt(2, reservationId);

            int affected = ps.executeUpdate();
            if (affected != 1) return -1;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }
    
    /**
     * Checks whether {@code checkInTime} is NULL for a given seating row.
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @return {@code true} if {@code checkInTime} is NULL; otherwise {@code false}
     * @throws SQLException if a DB error occurs
     */
    public boolean isCheckInNull(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_WHERE_CHECKIN_NULL)) {
            ps.setInt(1, seatingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getTimestamp(1) == null;
            }
        }
    }
    
    /**
     * Claims an automatic bill-send for a seating row (idempotent-ish).
     *
     * <p>Updates {@code billSent} from 0 to 2 only if:
     * <ul>
     *   <li>{@code checkOutTime IS NULL}</li>
     *   <li>{@code billSent = 0}</li>
     *   <li>{@code checkInTime <= NOW() - 2 hours}</li>
     * </ul>
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @return {@code true} if exactly one row was updated (claim succeeded)
     * @throws SQLException if a DB error occurs
     */
    public boolean claimAutoBillSend(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(CLAIM_AUTO_BILL_SEND)) {
            ps.setInt(1, seatingId);
            return ps.executeUpdate() == 1;
        }
    }
    
    /**
     * Updates the {@code billSent} flag for a seating.
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @param billSent new bill-sent state value
     * @return {@code true} if exactly one row was updated
     * @throws SQLException if a DB error occurs
     */
    public boolean updateBillSent(Connection conn, int seatingId, int billSent) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_BILL_SENT)) {
            ps.setInt(1, billSent);
            ps.setInt(2, seatingId);
            return ps.executeUpdate() == 1;
        }
    }
    
    /**
     * Fetches the reservation ID associated with a seating.
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @return reservation ID if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
    public Integer getReservationIdBySeatingId(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATION_ID_BY_SEATING_ID)) {
            ps.setInt(1, seatingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("reservationID") : null;
            }
        }
    }
    
    /**
     * Finds the latest open seating ID for a reservation (where {@code checkOutTime IS NULL}).
     *
     * @param conn active JDBC connection (must not be null)
     * @param reservationId reservation primary key
     * @return seating ID if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     * @throws IllegalArgumentException if {@code conn} is null
     */
    public Integer getSeatingIdByReservationId(Connection conn, int reservationId) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("conn is null");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_SEATING_ID_BY_RESERVATION_ID)) {
            ps.setInt(1, reservationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getInt("seatingID");
            }
        }
    }
    
    
    /**
     * Inserts a check-in row (opens its own DB connection).
     *
     * @param tableID table foreign key
     * @param reservationID reservation foreign key
     * @return generated seating ID on success, or {@code -1} if failed
     * @throws SQLException if a DB error occurs
     */
    public int checkIn(int tableID, int reservationID) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            return checkIn(conn, tableID, reservationID);
        }
    }
    
    /**
     * Inserts a check-in row using an existing connection (transaction-friendly).
     *
     * <p>Creates a seating row with {@code checkInTime = NOW()}, {@code checkOutTime = NULL},
     * and initializes {@code billSent = 0}.</p>
     *
     * @param conn active JDBC connection
     * @param tableID table foreign key
     * @param reservationID reservation foreign key
     * @return generated seating ID on success, or {@code -1} if failed
     * @throws SQLException if a DB error occurs
     */
    public int checkIn(Connection conn, int tableID, int reservationID) throws SQLException {
        try (PreparedStatement ps =conn.prepareStatement(INSERT_SEATING, Statement.RETURN_GENERATED_KEYS)) {

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
     * Converts a held seating to an actual check-in by setting {@code checkInTime = NOW()}.
     *
     * <p>Only applies if {@code checkOutTime IS NULL} and {@code checkInTime IS NULL}.</p>
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @return {@code true} if exactly one row was updated
     * @throws SQLException if a DB error occurs
     */
    public boolean markCheckInNow(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_CHECKIN_TIME_NOW)) {
            ps.setInt(1, seatingId);
            return ps.executeUpdate() == 1;
        }
    }
    
    /**
     * Fetches the most recent open seating ID for a table (row-locked with {@code FOR UPDATE}).
     *
     * @param conn active JDBC connection
     * @param tableID table primary key
     * @return open seating ID if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
    public Integer fetchOpenSeatingID(Connection conn,int tableID)throws SQLException {
    	try(PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_SEATING_TO_UPDATE)){
    		ps.setInt(1, tableID);
    		ResultSet rs = ps.executeQuery();
    		if(!rs.next()) return null;
    		return rs.getInt("seatingID");
    	}
    }
    
    /**
     * Finds the reservation ID of the most recent open seating for a table.
     *
     * @param conn active JDBC connection
     * @param tableId table primary key
     * @return reservation ID if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
    public Integer findOpenSeatingReservationId(Connection conn, int tableId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_SEATING_TO_UPDATE)) {
            ps.setInt(1, tableId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt("reservationID");
            }
        }
    }
    
    /**
     * Marks a seating as checked out by setting {@code checkOutTime = NOW()}.
     *
     * @param conn active JDBC connection
     * @param seatingId seating primary key
     * @return {@code true} if exactly one row was updated
     * @throws SQLException if a DB error occurs
     */
    public boolean checkOutBySeatingId(Connection conn, int seatingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_CHEKOUT_BY_SEATING_ID)) {
            ps.setInt(1, seatingId);
            return ps.executeUpdate() == 1;
        }
    }
    
    /**
     * Returns seating IDs that are due for bill processing.
     *
     * <p>Definition: open seatings where {@code billSent = 0} and check-in was at least 2 hours ago.</p>
     *
     * @param conn active JDBC connection (must not be null)
     * @return list of seating IDs that match the criteria (possibly empty)
     * @throws SQLException if a DB error occurs
     * @throws IllegalArgumentException if {@code conn} is null
     */
    public List<Integer> getSeatingsDueForBill(Connection conn) throws SQLException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection is null");
        }
        List<Integer> seatingIds = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_SEATINGS_DUE_FOR_BILL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                seatingIds.add(rs.getInt("seatingID"));
            }
        }

        return seatingIds;
    }
    
    /**
     * Fetches current open seatings and joins table/reservation/user data for UI display.
     *
     * @param conn active JDBC connection
     * @return list of {@link CurrentSeatingResponse} rows (possibly empty)
     * @throws SQLException if a DB error occurs
     */
    public List<CurrentSeatingResponse> fetchCurrentSeating(Connection conn) throws SQLException {
    	List<CurrentSeatingResponse> list = new ArrayList<>();
    	try( PreparedStatement ps = conn.prepareStatement(SELECT_CURRENT_SEATINGS);
    		ResultSet rs = ps.executeQuery();){
    		  		
    		
    		while(rs.next()) {
    			LocalDateTime checkIn = rs.getTimestamp("checkInTime").toLocalDateTime();
    			LocalDateTime estimated = rs.getTimestamp("estimatedCheckOutTime").toLocalDateTime();
    			
    			TableInfo ti = new TableInfo(rs.getInt("tableNumber"), rs.getInt("capacity"));
    			
    			CurrentSeatingResponse cs = new CurrentSeatingResponse(
    					rs.getString("userID"),
    					rs.getString("username"),
    					ti,
    					rs.getString("guestContact"),
    					rs.getInt("partySize"),
    					rs.getInt("seatingID"),
    					estimated,
    					rs.getInt("confirmationCode"),
    					checkIn);
    			list.add(cs);
    		}
    				
    	}
    	return list;
    }
    
    /**
     * Returns visit intervals (check-in/check-out pairs) within a time range.
     *
     * <p>Only returns rows where {@code checkOutTime IS NOT NULL}.</p>
     *
     * @param conn active JDBC connection
     * @param startInclusive inclusive start timestamp
     * @param endExclusive exclusive end timestamp
     * @return list of {@code LocalDateTime[2]} arrays: index 0 = check-in, index 1 = check-out
     * @throws SQLException if a DB error occurs
     */
    public List<LocalDateTime[]> getVisitTimesBetween(Connection conn,LocalDateTime startInclusive,LocalDateTime endExclusive) throws SQLException {
        List<LocalDateTime[]> visits = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(SELECT_VISIT_TIMES_BETWEEN)) {
            ps.setTimestamp(1, Timestamp.valueOf(startInclusive));
            ps.setTimestamp(2, Timestamp.valueOf(endExclusive));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime checkIn =rs.getTimestamp("checkInTime").toLocalDateTime();
                    LocalDateTime checkOut =rs.getTimestamp("checkOutTime").toLocalDateTime();

                    // index 0 = check-in, index 1 = check-out
                    visits.add(new LocalDateTime[] { checkIn, checkOut });
                }
            }
        }
        return visits;
    }
    
    /**
     * Fetches the table ID for a given seating ID.
     *
     * @param conn active JDBC connection
     * @param seatingID seating primary key
     * @return table ID if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
    public Integer getTableIDBySeatingID(Connection conn, int seatingID) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TABLE_ID_BY_SEATING_ID)) {

            ps.setInt(1, seatingID);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; 
                }
                return rs.getInt("tableID");
            }
        }
    }

}