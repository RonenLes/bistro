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

import entities.Reservation;
import entities.Table;
import entities.WaitingList;
import entities.Seating;

public class WaitingListDAO {
	
	
	//INSERT
	private final String INSERT_NEW_WAIT = "INSERT INTO waiting_list (reservationID, status, priority, createdAt, assignedAt) " +
	        "VALUES (?, ?, ?, ?, NULL)";
	
	//SELECT
	private final String SELECT_NEXT_IN_LINE = "SELECT w.waitID, w.reservationID, w.priority, w.status "+
											   "FROM waiting_list w "+
											   "JOIN reservation r ON r.reservationID = w.reservationID "+
											   "WHERE w.status = 'WAITING' AND r.allocatedCapacity <= ? "+
											   "ORDER BY w.priority DESC, w.createdAt ASC "+
											   "LIMIT 1 FOR UPDATE";
	
	//UPDATE
	private final String UPDATE_WAITLIST_STATUS = "UPDATE `waiting_list` SET status = ? WHERE reservationID = ?";
	private final String UPDATE_STATUS_TO_ASSIGNED = "UPDATE `waiting_list` SET status='ASSIGNED', assignedAt=NOW() "+
													 "WHERE waitID = ? AND status = 'WAITING'";
	// SELECT (put near the top of WaitingListDAO)
	private static final String SELECT_WAITING_COUNTS_BY_DAY_BETWEEN =
	        "SELECT DAY(createdAt) AS dayOfMonth, COUNT(*) AS cnt " +
	        "FROM waiting_list " +
	        "WHERE createdAt >= ? AND createdAt < ? " +
	        "GROUP BY DAY(createdAt)";

	
	public WaitingList getNextWaitingThatFits(Connection conn, int tableCapacity) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_NEXT_IN_LINE)) {
	        ps.setInt(1, tableCapacity);
	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next()) return null;
	            WaitingList w = new WaitingList(rs.getInt("waitID"),rs.getInt("reservationID"),rs.getString("status"),
	            		rs.getInt("priority"),rs.getTimestamp("createdAt").toLocalDateTime(),null);	            
	            return w;
	        }
	    }
	}
	
	//wrapper for insertNewWait for using multi DAO objects
	public boolean insertNewWait(int reservationID,String status,int priority) throws SQLException{
		try(Connection conn =DBManager.getConnection()){
			return insertNewWait(conn, reservationID, status, priority);
		}
	}
	/**
	 * method to inset a new row of waiting into database
	 * used when there is now available table to seat 
	 * @param conn 
	 * @param reservationID
	 * @param status
	 * @param priority
	 * @return
	 * @throws SQLException
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
	
	public WaitingList updateWaitingStatus(Connection conn,int reservationID,String status)throws SQLException {
		if(conn == null)throw new IllegalArgumentException("conn is null (updateWaitingStatus)");
		try(PreparedStatement ps = conn.prepareStatement(UPDATE_WAITLIST_STATUS)){
			ps.setString(1, status);
			ps.setInt(2, reservationID);
			
			ResultSet rs = ps.executeQuery();
									
			if(rs.next()) {
				WaitingList waitingList = new WaitingList(rs.getInt("waitID"), reservationID, status, rs.getInt("priority"),
						rs.getTimestamp("createdAt").toLocalDateTime(), LocalTime.now());
				return waitingList;
			}
			return null;
			
		}
	}
	
	public boolean markAssigned(Connection conn, int waitId) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_TO_ASSIGNED)) {
	        ps.setInt(1, waitId);
	        return ps.executeUpdate() == 1;
	    }
	}
	
	// Method (put near the bottom of WaitingListDAO)
	public Integer[] getCountOfWaitingListBetween(Connection conn,
	                                              LocalDateTime start,
	                                              LocalDateTime end) throws SQLException {

	    Integer[] counts = new Integer[31];
	    for (int i = 0; i < counts.length; i++) counts[i] = 0;

	    if (conn == null) {
	        throw new IllegalArgumentException("conn is null (getCountOfWaitingListBetween)");
	    }
	    if (start == null || end == null) {
	        return counts; // keep zeros
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

	
	
}
