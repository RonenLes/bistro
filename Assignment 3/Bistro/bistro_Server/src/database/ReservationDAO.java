package database;

import entities.Reservation;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private static final String SELECT_RESERVATIONS_DUE_FOR_NO_SHOW_PARAM ="SELECT reservationID FROM reservation WHERE reservationDate = ? AND status IN ('NEW','CONFIRMED') AND startTime <= ?";
	private static final String SELECT_reservationByConfirmationCode = "SELECT * FROM `reservation` WHERE confirmationCode = ?";
	private static final String SELECT_reservationByReservationId = "SELECT * FROM `reservation` WHERE reservationID = ?";
	private static final String SELECT_amountOfUsedSeats ="""
	        SELECT allocatedCapacity, COUNT(*) AS booked
	        FROM reservation
	        WHERE reservationDate = ?
	          AND status IN ('NEW','CONFIRMED','SEATED')
	          AND (? < ADDTIME(startTime, '02:00:00') AND ? > startTime)
	        GROUP BY allocatedCapacity
			""";
	private static final String SELECT_RESERVATIONS_DUE_FOR_REMINDER =
	        "SELECT reservationID, reservationDate, status, partySize, allocatedCapacity, " +
	        "       confirmationCode, guestContact, userID, startTime " +
	        "FROM reservations " +
	        "WHERE TIMESTAMP(reservationDate, startTime) >= (NOW() + INTERVAL 2 HOUR) " +
	        "  AND TIMESTAMP(reservationDate, startTime) <  (NOW() + INTERVAL 150 MINUTE) " +
	        "  AND status = 'APPROVED'";
	private static final String SELECT_CONFIRMATION_CODE_EXISTS = "SELECT 1 FROM reservation WHERE confirmationCode = ? LIMIT 1";
	
	// UPDATE statement
	private static final String UPDATE_STATUS_RESERVATION_SQL_BY_RESERVATION_ID ="UPDATE `reservation` SET status = ? WHERE reservationID = ?";
	private static final String UPDATE_STATUS_RESERVATION_SQL ="UPDATE `reservation` " +"SET status = ? " +"WHERE confirmation_code = ?";
	private static final String UPDATE_RESERVATION_BY_CONFIRMATION_CODE =
	        "UPDATE `reservation` " +
	        "SET reservationDate = ?, status = ?, partySize = ?, guestContact = ?, userID = ?, startTime = ? " +
	        "WHERE confirmationCode = ?";

	
	
	public boolean updateReservation(Connection conn,LocalDate reservationDate,String status,int partySize,int confirmationCode,
	        String guestContact,
	        String userID,
	        LocalTime startTime
	) throws SQLException {

	    try (
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
	public List<Reservation> getReservationsDueForReminder(Connection conn) throws SQLException {

	    List<Reservation> reservations = new ArrayList<>();

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATIONS_DUE_FOR_REMINDER);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            Reservation r = new Reservation(
	                    rs.getInt("reservationID"),
	                    rs.getDate("reservationDate").toLocalDate(),
	                    rs.getString("status"),
	                    rs.getInt("partySize"),
	                    rs.getInt("allocatedCapacity"),
	                    rs.getInt("confirmationCode"),
	                    rs.getString("guestContact"),
	                    rs.getString("userID"),
	                    rs.getTime("startTime").toLocalTime()
	            );
	            reservations.add(r);
	        }
	    }

	    return reservations;
	}


	public List<Integer> getReservationsDueForNoShow(Connection conn, LocalDate date, LocalTime lateCutoffTime)
	        throws SQLException {

	    if (conn == null) throw new IllegalArgumentException("conn is null");

	    List<Integer> ids = new ArrayList<>();

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_RESERVATIONS_DUE_FOR_NO_SHOW_PARAM)) {
	        ps.setDate(1, java.sql.Date.valueOf(date));
	        ps.setTime(2, java.sql.Time.valueOf(lateCutoffTime));

	        try (ResultSet rs = ps.executeQuery()) {
	            while (rs.next()) {
	                ids.add(rs.getInt("reservationID"));
	            }
	        }
	    }
	    return ids;
	}

	
	public boolean updateStatusByReservationID(Connection conn, int reservationID,String status) throws SQLException {
	    if (conn == null) throw new IllegalArgumentException("conn is null(updateStatus)");
	    

	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_RESERVATION_SQL_BY_RESERVATION_ID)) {
	        ps.setString(1, status);
	        ps.setInt(2, reservationID);
	        return ps.executeUpdate() == 1;
	    }
	}
	
	/**
	 * Cancels a reservation by setting its status to 'CANCELLED' (transaction-friendly).
	 *
	 * @param conn active JDBC connection (can be part of a larger transaction)
	 * @param confirmationCode reservation confirmation code
	 * @return true if exactly one row was updated, false otherwise
	 * @throws SQLException if a DB error occurs
	 */
	public boolean updateStatus(Connection conn, int confirmationCode,String status) throws SQLException {
	    if (conn == null) throw new IllegalArgumentException("conn is null(updateStatus)");
	    if (confirmationCode <= 0) return false;

	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_RESERVATION_SQL)) {
	        ps.setString(1, status);
	        ps.setInt(2, confirmationCode);
	        return ps.executeUpdate() == 1;
	    }
	}

	
	/**
	 * Cancels a reservation by setting its status to 'CANCELLED'.
	 * Convenience wrapper that opens/closes its own connection.
	 */
	public boolean updateStatus(int confirmationCode,String status) throws SQLException {
	    try (Connection conn = DBManager.getConnection()) {
	        return updateStatus(conn, confirmationCode,status);
	    }
	}

	
	// 1) Convenience wrapper (NO conn)
	public int insertNewReservation(LocalDate reservationDate,int numberOfGuests,int allocatedCapacity,int confirmationCode,	                                	                                	                                
	                                String userID, LocalTime startTime,String status,String guest) throws SQLException {	                               	                                	                                
	    try (Connection conn = DBManager.getConnection()) {
	        return insertNewReservation(conn, reservationDate, numberOfGuests, allocatedCapacity,
	                                    confirmationCode, userID, startTime, status, guest);
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
	 * @return reservationID  
	 * @throws SQLException
	 */
	public int insertNewReservation(Connection conn,LocalDate reservationDate,int numberOfGuests,int allocatedCapacity,
			int confirmationCode,String userID,LocalTime startTime,String status,String guest) throws SQLException {

	    java.sql.Date sqlReservationDate = java.sql.Date.valueOf(reservationDate);
	    if (conn == null) throw new IllegalArgumentException("conn is null");
	    try (PreparedStatement pstmt = conn.prepareStatement(INSERT_newReservation,Statement.RETURN_GENERATED_KEYS)) {
	         

	        pstmt.setDate(1, sqlReservationDate);
	        pstmt.setString(2, status);
	        pstmt.setInt(3, numberOfGuests);
	        pstmt.setInt(4, allocatedCapacity);
	        pstmt.setInt(5, confirmationCode);
	        pstmt.setString(6, guest);
	        pstmt.setString(7, userID);
	        pstmt.setTime(8, java.sql.Time.valueOf(startTime));

	        int isInserted = pstmt.executeUpdate();
	        if( isInserted != 1) return -1;
	        
	        ResultSet rs = pstmt.getGeneratedKeys();
	        return rs.next() ? rs.getInt(1) : -1;

	    } catch (SQLException e) {
	        System.err.println("Database error: could not insert new reservation");
	        throw e;
	    }
	}
	
	
	public Reservation getReservationByConfirmationCode(int code) throws SQLException {
        try (Connection conn = DBManager.getConnection()) {
            return getReservationByConfirmationCode(conn, code); // delegate
        }
    }
	
	public Reservation getReservationByConfirmationCode(Connection conn,int confirmationCode) throws SQLException {

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_reservationByConfirmationCode)) {
	         
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
	public Reservation getReservationByReservationID(Connection conn,int reservationID) throws SQLException {

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_reservationByReservationId)) {
	         
	        ps.setInt(1, reservationID);

	        try (ResultSet rs = ps.executeQuery()) {

	            if (!rs.next()) {
	                return null; 
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
	            "DB error fetching reservation by reservationID=" + reservationID
	        );
	        throw e;
	    }
	}
	public Map<Integer, Integer> getBookedTablesByCapacity(Connection conn,LocalDate date, LocalTime start, LocalTime end)throws SQLException {
		
	    Map<Integer, Integer> booked = new HashMap<>();
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_amountOfUsedSeats)) {

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
	public boolean isConfirmationCodeUsed(Connection conn, int code) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_CONFIRMATION_CODE_EXISTS)) {
	        ps.setInt(1, code);
	        try (ResultSet rs = ps.executeQuery()) {
	            return rs.next();
	        }
	    }
	}

	public int generateConfirmationCode(Connection conn) throws SQLException {
	    if (conn == null) {
	        throw new IllegalArgumentException("Connection is null");
	    }

	    int code;
	    do {
	        // 6-digit code
	        code = 100000 + (int) (Math.random() * 900000);
	    } while (isConfirmationCodeUsed(conn, code));

	    return code;
	}
	
	

}