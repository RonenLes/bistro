package database;

import java.sql.*;
import entities.Table;

public class TableDAO {
	
	//INSERT
	private final String INSERT_newTable ="INSERT INTO `restaurant_table` " + "(tableNumber, capacity) " +"(?, ?)";
	
	//SELECT
	private final String SELECT_sumOfTotalSeats ="SELECT SUM(capacity) AS totalCap FROM `restaurant_table`";
	private final String SELECT_tableByTableNumber = "SELECT * FROM `table` WHERE tableNumber = ?";
	private final String SELECT_availableTable = "SELECT t.tableID, t.tableNumber\r\n" //to find the now available table TO_DO in table receive run
			+ "FROM `table` t\r\n"
			+ "WHERE t.capacity >= ?\r\n"
			+ "  AND t.tableID NOT IN (\r\n"
			+ "      SELECT s.tableID\r\n"
			+ "      FROM seating s\r\n"
			+ "      WHERE s.checkOutTime IS NULL\r\n"
			+ "  )\r\n"
			+ "ORDER BY t.capacity ASC, t.tableNumber ASC\r\n"
			+ "LIMIT 1;";
	
	//UPDATE
	private final String UPDATE_tableCapacityByTableNumber = "UPDATE `table` SET capacity = ? WHERE tableNumber = ?"; //TO_DO in manager run
	
	
	/**
	 * method to calculate the total amount of seats the resturant has
	 * @return sum of all the capacity of the resturant_table
	 * @throws SQLException
	 */
	public int getTotalSeatCapacity() throws SQLException {
		try(Connection conn = DBManager.getConnection();
			PreparedStatement ps = conn.prepareStatement(SELECT_sumOfTotalSeats)){
			ResultSet rs = ps.executeQuery();
			
			if(rs.next()) return rs.getInt("totaclCap");
			
		}catch(SQLException e) {
			System.err.println("Database error: could not fetch sum of seating capacity");
			
		}
		return -1;
	}
	
	/**
	 * adding new table to the resturant_table in database
	 * @param tableNumber should be checked for uniqueness 
	 * @param capacity 
	 * @return boolean if succedded 
	 * @throws SQLException
	 */
	public boolean insertNewTable(int tableNumber,int capacity) throws SQLException {
		
		try(Connection conn = DBManager.getConnection();
			PreparedStatement ps = conn.prepareStatement(INSERT_newTable)) {
			ps.setInt(1,tableNumber);
			ps.setInt(2, capacity);
			int isInserted = ps.executeUpdate();
			return isInserted ==1;
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert new table");
			return false;
		}
	}
	
	
}
