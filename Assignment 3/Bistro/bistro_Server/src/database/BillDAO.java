package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class BillDAO {

	private static final String INSERT_NEW_BILL ="INSERT INTO bill (seatingID, totalPrice, status, createdAt, paidAt) VALUES (?, ?, ?, ?, ?)";
	private static final String SELECT_OPEN_BILL_TOTAL_BY_SEATING_ID =
			"SELECT totalPrice FROM bill WHERE seatingID = ? AND status = 'OPEN' ORDER BY createdAt DESC LIMIT 1";


	public int insertNewBill(Connection conn,int seatingID,double totalPrice,LocalDateTime createdAt,LocalDateTime paidAt) throws SQLException {
		if (conn == null) throw new IllegalArgumentException("conn is null");
		try (PreparedStatement pstmt =conn.prepareStatement(INSERT_NEW_BILL, Statement.RETURN_GENERATED_KEYS)) {

			pstmt.setInt(1, seatingID);
			pstmt.setDouble(2, totalPrice);
			pstmt.setString(3, "OPEN");
			pstmt.setTimestamp(4, Timestamp.valueOf(createdAt));
			pstmt.setTimestamp(5, Timestamp.valueOf(paidAt)); 

			int isInserted = pstmt.executeUpdate();
			if (isInserted != 1) return -1;

			ResultSet rs = pstmt.getGeneratedKeys();
			return rs.next() ? rs.getInt(1) : -1;

		} catch (SQLException e) {
			System.err.println("Database error: could not insert new bill");
			throw e;
		}
	}
	
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
