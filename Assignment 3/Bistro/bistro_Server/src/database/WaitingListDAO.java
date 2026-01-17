package database;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import entities.Reservation;
import entities.Table;
import entities.WaitingList;
import entities.Seating;

/**
 * DAO for the {@code waiting_list} table.
 *
 * <p>Main idea: manages the restaurant waiting list lifecycle for reservations when no table is available.
 * This DAO supports:
 * <ul>
 *   <li>Inserting new waiting-list entries</li>
 *   <li>Fetching the next waiting entry that can fit a given table capacity (with row locking)</li>
 *   <li>Updating waiting-list status transitions (WAITING -> CALLED -> ASSIGNED / CANCELLED)</li>
 *   <li>Looking up waiting-list entries by reservation</li>
 *   <li>Reporting helpers (daily counts for a time window)</li>
 *   <li>Queries for operational flows (today's waiting list, expired CALLED entries)</li>
 * </ul>
 *
 * <p>Status usage in this DAO commonly includes: {@code WAITING}, {@code CALLED}, {@code ASSIGNED}, {@code CANCELLED}.
 */
public class WaitingListDAO {
	
	
	//INSERT
	private final String INSERT_NEW_WAIT = "INSERT INTO waiting_list (reservationID, status, priority, createdAt, assignedAt) " +
	        "VALUES (?, ?, ?, ?, NULL)";
	
	//SELECT
	private final String SELECT_NEXT_IN_LINE =
	        "SELECT w.waitID, w.reservationID, w.priority, w.status, w.createdAt " +
	        "FROM waiting_list w " +
	        "JOIN reservation r ON r.reservationID = w.reservationID " +
	        "WHERE w.status = 'WAITING' AND r.allocatedCapacity <= ? " +
	        "ORDER BY w.priority DESC, w.createdAt ASC " +
	        "LIMIT 1 FOR UPDATE";

	private static final String SELECT_RESERVATION_BY_WAIT_ID =
	        "SELECT r.* " +
	        "FROM waiting_list w " +
	        "JOIN reservation r ON w.reservationID = r.reservationID " +
	        "WHERE w.waitID = ?";
	private static final String SELECT_WAITINGLIST_BY_RESERVATION_ID =
	        "SELECT waitID, reservationID, status, priority, createdAt, assignedAt " +
	        "FROM waiting_list " +
	        "WHERE reservationID = ? " +
	        "ORDER BY waitID DESC " +
	        "LIMIT 1";
	//UPDATE
	private final String UPDATE_WAITLIST_STATUS = "UPDATE `waiting_list` SET status = ? WHERE reservationID = ?";
	private static final String UPDATE_WAITINGLIST_TO_ASSIGNED =
	        "UPDATE waiting_list SET status='ASSIGNED' " +
	        "WHERE reservationID = ? AND status='CALLED'";

	private final String UPDATE_STATUS_TO_CALLED = "UPDATE `waiting_list` SET status='CALLED', assignedAt=NOW() "+
			 "WHERE waitID = ? AND status = 'WAITING'";
	

	// SELECT (put near the top of WaitingListDAO)
	private static final String SELECT_WAITING_COUNTS_BY_DAY_BETWEEN =
	        "SELECT DAY(createdAt) AS dayOfMonth, COUNT(*) AS cnt " +
	        "FROM waiting_list " +
	        "WHERE createdAt >= ? AND createdAt < ? " +
	        "GROUP BY DAY(createdAt)";
	private static final String SELECT_WAITINGLIST_TODAY =
	        "SELECT * FROM waiting_list " +
	        "WHERE status = 'WAITING' " +
	        "AND assignedAt IS NULL " +
	        "AND DATE(createdAt) = CURRENT_DATE";
	private static final String SELECT_EXPIRED_CALLED_WAITINGLIST =
	        "SELECT waitID, reservationID, status, priority, createdAt, assignedAt " +
	        "FROM waiting_list " +
	        "WHERE status = 'CALLED' " +
	        "AND assignedAt IS NOT NULL " +
	        "AND assignedAt <= NOW() - INTERVAL 15 MINUTE";


	/**
     * Fetches the {@link Reservation} linked to a waiting-list entry by waitID.
     *
     * @param conn active JDBC connection
     * @param waitID waiting list primary key
     * @return reservation if found; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
	public Reservation getReservationByWaitingID(Connection conn,int waitID) throws SQLException {
		try(PreparedStatement ps=conn.prepareStatement(SELECT_RESERVATION_BY_WAIT_ID)){
			ps.setInt(1,waitID);
			try(ResultSet rs=ps.executeQuery()){
				if(!rs.next())return null;
				Reservation r= new Reservation(rs.getInt("reservationID"),
	                rs.getDate("reservationDate").toLocalDate(),
	                rs.getString("status"),
	                rs.getInt("partySize"),
	                rs.getInt("allocatedCapacity"),
	                rs.getInt("confirmationCode"),
	                rs.getString("guestContact"),  // may be null
	                rs.getString("userID"),        // may be null
	                rs.getTime("startTime") != null? rs.getTime("startTime").toLocalTime(): null,	                        	                        
	                rs.getTimestamp("timeOfCreation").toLocalDateTime());
				return r;
			}
			
		}
	}
	
	/**
     * Returns the next WAITING list entry that can fit a given table capacity.
     *
     * <p>This query uses {@code FOR UPDATE} to safely support "pick next in line" in concurrent flows.</p>
     *
     * @param conn active JDBC connection
     * @param tableCapacity the table capacity available
     * @return the next {@link WaitingList} entry that fits; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     */
	public WaitingList getNextWaitingThatFits(Connection conn, int tableCapacity) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_NEXT_IN_LINE)) {
	        ps.setInt(1, tableCapacity);
	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next()) return null;

	            return new WaitingList(rs.getInt("waitID"),rs.getInt("reservationID"),rs.getString("status"),rs.getInt("priority"),rs.getTimestamp("createdAt").toLocalDateTime(),null);
	        }
	    }
	}

	

	
	/**
     * Convenience wrapper that opens its own connection and inserts a waiting-list entry.
     *
     * @param reservationID reservation foreign key
     * @param status initial status (typically {@code WAITING})
     * @param priority priority value used for ordering
     * @return {@code true} if inserted successfully
     * @throws SQLException if a DB error occurs
     */
	public boolean insertNewWait(int reservationID,String status,int priority) throws SQLException{
		try(Connection conn =DBManager.getConnection()){
			return insertNewWait(conn, reservationID, status, priority);
		}
	}
	/**
     * Inserts a new waiting-list row (transaction-friendly).
     *
     * <p>Used when there is no available table to seat a reservation.</p>
     *
     * @param conn active JDBC connection
     * @param reservationId reservation foreign key
     * @param status initial status (typically {@code WAITING})
     * @param priority priority value (higher usually means earlier selection)
     * @return {@code true} if inserted successfully
     * @throws SQLException if a DB error occurs
     */
	public boolean insertNewWait(Connection conn, int reservationId, String status, int priority) throws SQLException {

	    try (PreparedStatement ps = conn.prepareStatement(INSERT_NEW_WAIT)) {
	        ps.setInt(1, reservationId);
	        ps.setString(2, status);
	        ps.setInt(3, priority);
	        ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now())); 

	        return ps.executeUpdate() == 1;
	    }
	}
	
	/**
     * Updates waiting-list status by reservationID.
     *
     * @param conn active JDBC connection (must not be null)
     * @param reservationID reservation foreign key
     * @param status new status string
     * @return {@code true} if at least one row was updated
     * @throws SQLException if a DB error occurs
     * @throws IllegalArgumentException if {@code conn} is null
     */
	public boolean updateWaitingStatus(Connection conn, int reservationID, String status) throws SQLException {
	    if (conn == null) {
	        throw new IllegalArgumentException("conn is null (updateWaitingStatus)");
	    }

	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_WAITLIST_STATUS)) {
	        ps.setString(1, status);
	        ps.setInt(2, reservationID);

	        int affected = ps.executeUpdate();
	        return affected >= 1; // use == 1 if reservationID is guaranteed unique in waiting_list
	    }
	}
	
	/**
     * Fetches the most recent waiting-list entry for a reservation.
     *
     * @param conn active JDBC connection (must not be null)
     * @param reservationID reservation foreign key
     * @return waiting-list entry if exists; otherwise {@code null}
     * @throws SQLException if a DB error occurs
     * @throws IllegalArgumentException if {@code conn} is null
     */
	public WaitingList getWaitingListByReservationId(Connection conn, int reservationID) throws SQLException {
	    if (conn == null) {
	        throw new IllegalArgumentException("conn is null (getWaitingListByReservationId)");
	    }

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_WAITINGLIST_BY_RESERVATION_ID)) {
	        ps.setInt(1, reservationID);

	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next()) return null;

	            return new WaitingList(
	                    rs.getInt("waitID"),
	                    rs.getInt("reservationID"),
	                    rs.getString("status"),
	                    rs.getInt("priority"),
	                    rs.getTimestamp("createdAt") != null
	                            ? rs.getTimestamp("createdAt").toLocalDateTime()
	                            : null,
	                    rs.getTimestamp("assignedAt") != null
	                            ? rs.getTimestamp("assignedAt").toLocalDateTime()
	                            : null
	            );
	        }
	    }
	}
	
	/**
     * Marks a waiting-list entry as ASSIGNED if it is currently CALLED (by reservationID).
     *
     * @param conn active JDBC connection
     * @param reservationId reservation foreign key
     * @return {@code true} if exactly one row was updated
     * @throws SQLException if a DB error occurs
     */
	public boolean markAssignedIfCalled(Connection conn, int reservationId) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_WAITINGLIST_TO_ASSIGNED)) {
	        ps.setInt(1, reservationId);
	        return ps.executeUpdate() == 1;
	    }
	}
	
	/**
     * Marks a WAITING entry as CALLED and sets {@code assignedAt = NOW()} (by waitID).
     *
     * @param conn active JDBC connection
     * @param waitId waiting list primary key
     * @return {@code true} if exactly one row was updated
     * @throws SQLException if a DB error occurs
     */
	public boolean markCalled(Connection conn, int waitId) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_TO_CALLED)) {
	        ps.setInt(1, waitId);
	        return ps.executeUpdate() == 1;
	    }
	}
	
	/**
     * Computes the count of waiting-list entries per day-of-month between two timestamps.
     *
     * @param conn active JDBC connection
     * @param start start timestamp (inclusive)
     * @param end end timestamp (exclusive)
     * @return array of size 31 indexed by day-of-month minus one (0..30)
     * @throws SQLException if a DB error occurs
     */
	public Integer[] getCountOfWaitingListBetween(Connection conn,LocalDateTime start,LocalDateTime end) throws SQLException {

	    Integer[] counts = new Integer[31];
	    for (int i = 0; i < counts.length; i++) counts[i] = 0;
	    if (start == null || end == null) {
	        return counts; 
	    }
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_WAITING_COUNTS_BY_DAY_BETWEEN)) {
	        ps.setTimestamp(1, Timestamp.valueOf(start));
	        ps.setTimestamp(2, Timestamp.valueOf(end));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                int day = rs.getInt("dayOfMonth"); // 1..31
	                int cnt = rs.getInt("cnt");

	                if (day >= 1 && day <= 31) {
	                    counts[day - 1] = cnt;
	                }
	            }
	        }
	    }
	    return counts;
	}
	
	/**
     * Fetches all WAITING entries created today (not yet assigned/called).
     *
     * @param conn active JDBC connection
     * @return list of today's waiting list entries (possibly empty)
     * @throws SQLException if a DB error occurs
     */
	public List<WaitingList> fetchWaitingListByCurrentDate(Connection conn) throws SQLException {
	    List<WaitingList> waitingList = new ArrayList<>();

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_WAITINGLIST_TODAY);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            WaitingList w = new WaitingList(
	                rs.getInt("waitID"),
	                rs.getInt("reservationID"),
	                rs.getString("status"),
	                rs.getInt("priority"),
	                rs.getTimestamp("createdAt").toLocalDateTime(),
	                null
	            );

	            waitingList.add(w);
	        }
	    }

	    return waitingList;
	}
	
	/**
     * Fetches CALLED entries that have expired (called more than 15 minutes ago).
     *
     * @param conn active JDBC connection
     * @return list of expired CALLED entries (possibly empty)
     * @throws SQLException if a DB error occurs
     */
	public List<WaitingList> fetchExpiredCalled(Connection conn) throws SQLException {
	    List<WaitingList> out = new ArrayList<>();

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_EXPIRED_CALLED_WAITINGLIST);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            WaitingList w = new WaitingList(
	                    rs.getInt("waitID"),
	                    rs.getInt("reservationID"),
	                    rs.getString("status"),
	                    rs.getInt("priority"),
	                    rs.getTimestamp("createdAt").toLocalDateTime(),
	                    rs.getTimestamp("assignedAt").toLocalDateTime() // assignedAt is your "called time"
	            );
	            out.add(w);
	        }
	    }
	    return out;
	}

}