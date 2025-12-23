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
	private final String SELECT_HIGHER_PRIORITY = "SELECT FROM `waiting_list` WHERE ";
	
	//UPDATE
	private final String UPDATE_WAITLIST_STATUS = "UPDATE `waiting_list` SET status = ? WHERE reservationID = ?";
	
	
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
	
	
	
	
}
