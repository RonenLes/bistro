package database;

import entities.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;


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
															"(reservationDate, status, partySize, confirmationCode, guestContact, userID, startTime) "+
															"VALUES (?, ?, ?, ?, ?, ?, ?)";
	
	//SELECT statements
	private static final String SELECT_howManyUniqueCodes = "SELECT COUNT(*) FROM `reservations` WHERE confirmationCode = ?";
	private static final String SELECT_reservationByConfirmationCode = "SELECT * FROM `reservation` WHERE confirmationCode = ?";
	private static final String SELECT_amountOfUsedSeats ="SELECT COALESCE(SUM(partySize),0) AS usedSeats\r\n"
			+ "        FROM reservation\r\n"
			+ "        WHERE reservationDate = ?\r\n"
			+ "          AND status IN ('NEW','CONFIRMED')\r\n"
			+ "          AND (? < ADDTIME(startTime, '02:00:00') AND ? > startTime)";
	
	
	// UPDATE statement
	private static final String UPDATE_reservationByConfirmationCode= "UPDATE `reservation` SET partySize = ?, reservationDate = ?,startTime = ? WHERE confirmationCode = ?";
	
	/**
	 * method searching reservation by a unique code and updating the reservation 
	 * @param newGuests
	 * @param newDate
	 * @param confirmationCode
	 * @return if found the reservation and succeded  
	 * @throws SQLException
	 */
	public boolean updateReservation(int newGuests,LocalDate newDate,int confirmationCode,LocalTime newTime) throws SQLException{
		java.sql.Date sqlReservationDate = java.sql.Date.valueOf(newDate);
		
		try (Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(UPDATE_reservationByConfirmationCode))			
			{
			pstmt.setInt(1, newGuests);
			pstmt.setDate(2,sqlReservationDate);
			pstmt.setTime(3, java.sql.Time.valueOf(newTime));
			pstmt.setInt(4, confirmationCode);
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
			pstmt.setString(5, guest);
			pstmt.setString(6, userID);
			pstmt.setTime(7, java.sql.Time.valueOf(startTime));
			
			int isInserted = pstmt.executeUpdate();
			return isInserted==1; //return 'true' if indeed new reservation added and only 1 row was affected
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert new reservation");
			throw e;
		}	
	}
	
	
	/**
	 * Calculates the total number of seats already reserved for a given date
	 * and time window.
	 * 
	 * This method is typically used to determine availability before creating
	 * or updating a reservation.
	 * @param date the reservation date to check
	 * @param startTime the start time of the requested reservation
	 * @param end the end time of the requested reservation
	 * @return the total number of seats already reserved during the given time window
	 * @throws SQLException
	 */
	public int getUsedSeats(LocalDate date, LocalTime startTime, LocalTime end) throws SQLException{
		 try (Connection conn = DBManager.getConnection();
		         PreparedStatement ps = conn.prepareStatement(SELECT_amountOfUsedSeats)) {

		        ps.setDate(1, java.sql.Date.valueOf(date));
		        ps.setTime(2, java.sql.Time.valueOf(startTime));
		        ps.setTime(3, java.sql.Time.valueOf(end));

		        try (ResultSet rs = ps.executeQuery()) {
		            rs.next();
		            return rs.getInt("usedSeats");
		        }
		    }
	}
	
	
	/**
	 * 
	 * @param confirmationCode do check on
	 * @return true if the given confirmation code is unique
	 * @throws SQLException
	 */
	public boolean isConfirmationCodeUsed(int confirmationCode) throws SQLException {
		
		try (Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(SELECT_howManyUniqueCodes))			
			{
			pstmt.setInt(1,confirmationCode);
			ResultSet rs=pstmt.executeQuery();
			
			if(rs.next()) {
				int count = rs.getInt(1);
				return count >0;
			}
		}catch(SQLException e) {
			System.err.println("Database error counting unique codes");
			throw e;
		}
		return false;
	}
}
