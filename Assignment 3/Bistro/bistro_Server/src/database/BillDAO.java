package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import entities.Bill;

public class BillDAO {

	private static final String INSERT_NEW_BILL ="INSERT INTO bill (seatingID, totalPrice, status, createdAt, paidAt) VALUES (?, ?, ?, ?, ?)";
	private static final String SELECT_OPEN_BILL_TOTAL_BY_SEATING_ID =
			"SELECT totalPrice FROM bill WHERE seatingID = ? AND status = 'OPEN' ORDER BY createdAt DESC LIMIT 1";
	private static final String SELECT_OPEN_BILL_BY_SEATING_ID ="SELECT billID, seatingID, totalPrice, status, createdAt, paidAt " + "FROM bill " +"WHERE seatingID = ? AND paidAt IS NULL";
	private static final String UPDATE_BILL_TO_PAID_BY_SEATING_ID =
	        "UPDATE bill " +
	        "SET status = 'PAID', paidAt = NOW() " +
	        "WHERE seatingID = ? AND paidAt IS NULL";
	
	/**
	 * insert new bill in the database
	 * @param conn
	 * @param seatingID
	 * @param totalPrice
	 * @param createdAt
	 * @return the key of the new bill entry
	 * @throws SQLException
	 */
	public int insertNewBill(Connection conn,int seatingID,double totalPrice,LocalDateTime createdAt) throws SQLException {
		if (conn == null) throw new IllegalArgumentException("conn is null");
		try (PreparedStatement pstmt =conn.prepareStatement(INSERT_NEW_BILL, Statement.RETURN_GENERATED_KEYS)) {

			pstmt.setInt(1, seatingID);
			pstmt.setDouble(2, totalPrice);
			pstmt.setString(3, "OPEN");
			pstmt.setTimestamp(4, Timestamp.valueOf(createdAt));
			pstmt.setTimestamp(5, null); 

			int isInserted = pstmt.executeUpdate();
			if (isInserted != 1) return -1;

			ResultSet rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : -1;

		} catch (SQLException e) {
			System.err.println("Database error: could not insert new bill");
			throw e;
		}
	}
	
	/**
	 * update bill status to 'PAID'
	 * @param conn
	 * @param seatingId
	 * @return
	 * @throws SQLException
	 */
	public boolean markBillAsPaidBySeatingId(Connection conn, int seatingId) throws SQLException {

	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_BILL_TO_PAID_BY_SEATING_ID)) {
	        ps.setInt(1, seatingId);
	        int updatedRows = ps.executeUpdate();
	        return updatedRows > 0;
	    }
	}
	
	/**
	 * fetch bill details by FK seatingID (bill no paid)
	 * @param conn
	 * @param seatingId
	 * @return Bill entity with its data
	 * @throws SQLException
	 */
	public Bill getOpenBillBySeatingId(Connection conn, int seatingId) throws SQLException {

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_BILL_BY_SEATING_ID)) {
	        ps.setInt(1, seatingId);

	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next())
	                return null;

	            return new Bill(
	            		rs.getInt("billID"),
	                    rs.getInt("seatingID"),
	                    rs.getDouble("totalPrice"),
	                    rs.getString("status"),
	                    rs.getTimestamp("createdAt").toLocalDateTime(),
	                    rs.getTimestamp("paidAt") != null
	                        ? rs.getTimestamp("paidAt").toLocalDateTime()
	                        : null
	            );
	        }
	    }
	}


	/**
	 * 
	 * @param conn
	 * @param seatingId
	 * @return
	 * @throws SQLException
	 */
	public Double getOpenBillTotalBySeatingId(Connection conn, int seatingId) throws SQLException {
	    if (conn == null) throw new IllegalArgumentException("conn is null");

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_OPEN_BILL_TOTAL_BY_SEATING_ID)) {
	        ps.setInt(1, seatingId);

	        try (ResultSet rs = ps.executeQuery()) {
	            if (!rs.next()) return null;
	            return rs.getDouble("totalPrice");
	        }
	    } catch (SQLException e) {
	        System.err.println("DB error fetching OPEN bill total by seatingID=" + seatingId);
	        throw e;
	    }
	}


	
	
	
	
}
