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
	private static final String SELECT_howManyUniqueCodes = "SELECT COUNT(*) FROM `reservations` WHERE confirmationCode = ?";
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
	private static final String UPDATE_RESERVATION_BY_CONFIRMATION_CODE =
	        "UPDATE `reservation` " +
	        "SET reservationDate = ?, status = ?, partySize = ?, guestContact = ?, userID = ?, startTime = ? " +
	        "WHERE confirmationCode = ?";

	
	
	public boolean updateReservation(LocalDate reservationDate,String status,int partySize,int confirmationCode,
	        String guestContact,
	        String userID,
	        LocalTime startTime
	) throws SQLException {

	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement ps = conn.prepareStatement(UPDATE_RESERVATION_BY_CONFIRMATION_CODE)) {

	        ps.setDate(1, java.sql.Date.valueOf(reservationDate));
	        ps.setString(2, status);
	        ps.setInt(3, partySize);
	        ps.setString(4, guestContact); // can be null
	        ps.setString(5, userID);       // can be null
	        ps.setTime(6, startTime != null ? java.sql.Time.valueOf(startTime) : null);
	        ps.setInt(7, confirmationCode);

	        int affected = ps.executeUpdate();
	        return affected == 1;

	    } catch (SQLException e) {
	        System.err.println("DB error updating reservation by confirmationCode=" + confirmationCode);
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
	public boolean insertNewReservation(LocalDate reservationDate,int numberOfGuests,int allocatedCapacity,
			int confirmationCode,String userID,LocalTime startTime,String status,String guest) throws SQLException {

	    java.sql.Date sqlReservationDate = java.sql.Date.valueOf(reservationDate);

	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement pstmt = conn.prepareStatement(INSERT_newReservation)) {

	        pstmt.setDate(1, sqlReservationDate);
	        pstmt.setString(2, status);
	        pstmt.setInt(3, numberOfGuests);
	        pstmt.setInt(4, allocatedCapacity);
	        pstmt.setInt(5, confirmationCode);
	        pstmt.setString(6, guest);
	        pstmt.setString(7, userID);
	        pstmt.setTime(8, java.sql.Time.valueOf(startTime));

	        int isInserted = pstmt.executeUpdate();
	        return isInserted == 1;

	    } catch (SQLException e) {
	        System.err.println("Database error: could not insert new reservation");
	        throw e;
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
	
	public Reservation getReservationByConfirmationCode(int confirmationCode) throws SQLException {

	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement ps = conn.prepareStatement(SELECT_reservationByConfirmationCode)) {

	        ps.setInt(1, confirmationCode);

	        try (ResultSet rs = ps.executeQuery()) {

	            if (!rs.next()) {
	                return null; // No reservation with this confirmation code
	            }

	            return new Reservation(
	                rs.getInt("reservationID"),
	                rs.getDate("reservationDate").toLocalDate(),
	                rs.getString("status"),
	                rs.getInt("partySize"),
	                rs.getInt("allocatedCapacity"),
	                rs.getInt("confirmationCode"),
	                rs.getString("guestContact"),  // may be null
	                rs.getString("userID"),        // may be null
	                rs.getTime("startTime") != null
	                        ? rs.getTime("startTime").toLocalTime()
	                        : null
	            );
	        }

	    } catch (SQLException e) {
	        System.err.println(
	            "DB error fetching reservation by confirmationCode=" + confirmationCode
	        );
	        throw e;
	    }
	}

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
	    }
	    return booked;
	}

}