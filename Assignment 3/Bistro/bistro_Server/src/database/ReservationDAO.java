package database;

import entities.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;


/**
 * method implemented:
 * 1.updateReservation - update an existing reservation in the database
 * 2.insertNewReservation - add a new reservation to database
 * 3.getUsedSeats - calculating number of used seats to deterimne free space 
 * 4.isConfirmationCodeUsed - checking if a given confirmation code already exist for generating unique later 
 */
public class ReservationDAO {
	
	
	//INSERT statement
	private static final String INSERT_newReservation = "INSERT INTO `reservation` " + 
															"(reservationDate, status, partySize, allocatedCapacity, confirmationCode, guestContact, userID, startTime) "+
															"VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	
	//SELECT statements
	private static final String SELECT_reservationByConfirmationCode = "SELECT * FROM `reservation` WHERE confirmationCode = ?";
	private static final String SELECT_amountOfUsedSeats ="""
	        SELECT allocatedCapacity, COUNT(*) AS booked
	        FROM reservation
	        WHERE reservationDate = ?
	          AND status IN ('NEW','CONFIRMED')
	          AND (? < ADDTIME(startTime, '02:00:00') AND ? > startTime)
	        GROUP BY allocatedCapacity
	        """;
	
	// UPDATE statement
	private static final String UPDATE_reservationByConfirmationCode= "UPDATE `reservation` SET partySize = ?, allocatedCapacity = ?, reservationDate = ?,startTime = ? WHERE confirmationCode = ?";
	
	/**
	 * method searching reservation by a unique code and updating the reservation 
	 * @param newGuests
	 * @param newDate
	 * @param confirmationCode
	 * @return if found the reservation and succeded  
	 * @throws SQLException
	 */
	public boolean updateReservation(int newGuests,LocalDate newDate,int confirmationCode,LocalTime newTime,int newAllocation) throws SQLException{
		java.sql.Date sqlReservationDate = java.sql.Date.valueOf(newDate);
		
		try (Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(UPDATE_reservationByConfirmationCode))			
			{
			pstmt.setInt(1, newGuests);
			pstmt.setInt(2, newAllocation);
			pstmt.setDate(3,sqlReservationDate);
			pstmt.setTime(4, java.sql.Time.valueOf(newTime));
			pstmt.setInt(5, confirmationCode);
			int isAffected = pstmt.executeUpdate();
			return isAffected ==1;
		}catch(SQLException e) {
			System.err.println("Database error: could not edit new reservation");
			throw e;
		}
	}
	
	/**
	 * method to add a new reservation
	 * @param reservationID
	 * @param reservationDate
	 * @param numberOfGuests
	 * @param confirmationCode
	 * @param subscriberId
	 * @param dateOfPlacingOrder
	 * @return true if succedded 
	 * @throws SQLException
	 */
	public boolean insertNewReservation(LocalDate reservationDate,int numberOfGuests,
			int confirmationCode,String userID,LocalTime startTime,String status,String guest) throws SQLException {
		
		java.sql.Date sqlReservationDate = java.sql.Date.valueOf(reservationDate);
		
		
		try (
			Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(INSERT_newReservation))
		{
			//insert the details we got from control into statement
			
			pstmt.setDate(1,sqlReservationDate);
			pstmt.setString(2, status);
			pstmt.setInt(3,numberOfGuests);
			pstmt.setInt(4,confirmationCode);
			pstmt.setInt(5,confirmationCode);
			pstmt.setString(6, guest);
			pstmt.setString(7, userID);
			pstmt.setTime(8, java.sql.Time.valueOf(startTime));
			
			int isInserted = pstmt.executeUpdate();
			return isInserted==1; //return 'true' if indeed new reservation added and only 1 row was affected
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert new reservation");
			throw e;
		}	
	}
	
	
	
	/**
	 * 
	 * @param confirmationCode to retrieve a reservation from database based on the code
	 * @return Reservation 
	 * @throws SQLException
	 */
	public Reservation isConfirmationCodeUsed(int confirmationCode) throws SQLException {
		
		Reservation reservation =null;
		
		try (Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(SELECT_reservationByConfirmationCode)){			
			
			pstmt.setInt(1,confirmationCode);
			ResultSet rs=pstmt.executeQuery();
			
			if(rs.next()) {
				reservation = new Reservation(
									rs.getInt("reservationID"),
									rs.getDate("reservationDate").toLocalDate(),
									rs.getString("status"),
									rs.getInt("partySize"),
									rs.getInt("allocatedCapacity"),
									rs.getInt("confirmationCode"),
									rs.getString("guestContact"),
									rs.getString("userID"),
									rs.getTime("startTime").toLocalTime());				
			}
			return reservation;
			}catch(SQLException e) {
				return reservation;
			}
		
		
	}
	
	/**
	 * 
	 * @param date of the desired reservation
	 * @param start time of the reservation based on opening hours of the date
	 * @param end expected time of reservation
	 * @return a map with each table size and how many booked for this specific table size
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getBookedTablesByCapacity(LocalDate date, LocalTime start, LocalTime end)throws SQLException {
	        	    
	    Map<Integer, Integer> booked = new HashMap<>();

	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement ps = conn.prepareStatement(SELECT_amountOfUsedSeats)) {

	        ps.setDate(1, java.sql.Date.valueOf(date));
	        ps.setTime(2, java.sql.Time.valueOf(start));
	        ps.setTime(3, java.sql.Time.valueOf(end));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                booked.put(rs.getInt("allocatedCapacity"), rs.getInt("booked"));
	            }
	        }
	    }catch(SQLException e) {
	    	System.err.println("database error: cant fetch booked tables by capacity");
	    }
	    return booked;
	}
	
	

}
