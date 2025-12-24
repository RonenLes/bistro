package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;

import entities.Reservation;
import entities.Table;
import entities.WaitingList;
import entities.Seating;

public class WaitingListDAO {
	
	
	//INSERT
	private final String INSERT_NEW_WAIT = "INSERT INTO `waiting_list` (reservationID,status,priority,createdAt) "+
											"(?,?,?,NOW())";
	
	//SELECT
	private final String SELECT_NEXT_IN_LINE = "SELECT w.waitID, w.reservationID, w.priority, w.status "+
											   "FROM waiting_list w "+
											   "JOIN reservation r ON r.reservationID = w.reservationID "+
											   "WHERE w.statis = 'WAITING' AND r.allocatedCapacity <= ? "+
											   "ORDER BY w.priority DESC, w.createdAt ASC "+
											   "LIMIT 1 FOR UPDATE";
	
	//UPDATE
	private final String UPDATE_WAITLIST_STATUS = "UPDATE `waiting_list` SET status = ? WHERE reservationID = ?";
	private final String UPDATE_STATUS_TO_ASSIGNED = "UPDATE `waiting_list` SET status='ASSIGNED', assignedAt=NOW() "+
													 "WHERE waitID = ? AND status = 'WAITING'";
	
	
	public WaitingList getNextWaitingThatFits(Connection conn, int tableCapacity) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_NEXT_IN_LINE)) {
	        ps.setInt(1, tableCapacity);
	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next()) return null;
	            WaitingList w = new WaitingList(rs.getInt("waitID"),rs.getInt("reservationID"),rs.getString("status"),
	            		rs.getInt("priority"),rs.getTime("createdAt").toLocalTime(),null);	            
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
	public boolean insertNewWait(Connection conn,int reservationID,String status,int priority) throws SQLException{
		if(conn == null) throw new IllegalArgumentException("conn is null (insertNewWait)");
		try(PreparedStatement ps = conn.prepareStatement(INSERT_NEW_WAIT)){
			ps.setInt(1, reservationID);
			ps.setString(2, status);
			ps.setInt(3, priority);
			
			int isInserted = ps.executeUpdate();
			return isInserted==1;
		}
	}
	
	
	//wrapper method 
	public WaitingList updateWaitingStatus(int reservationID,String status)throws SQLException{
		try(Connection conn = DBManager.getConnection()){
			return updateWaitingStatus(conn, reservationID, status);
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
						rs.getTime("createdAt").toLocalTime(), LocalTime.now());
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
	
	
}
