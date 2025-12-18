package database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import entities.Table;

public class TableDAO {
	
	//INSERT
	private final String INSERT_newTable ="INSERT INTO `restaurant_table` " + "(tableNumber, capacity) " +"(?, ?)";
	
	//SELECT
	private final String SELECT_sumOfTotalSeats ="SELECT SUM(capacity) AS totalCap FROM `restaurant_table`";
	private final String SELECT_tableByTableNumber = "SELECT * FROM `table` WHERE tableNumber = ?";
	private final String SELECT_tablesByCapacity = "SELECT capacity, COUNT(*) AS total FROM restaurant_table GROUP BY capacity";								
	private final String SELECT_availableTable = "SELECT t.tableID, t.tableNumber " //to find the now available table TO_DO in table receive run
			+ "FROM `table` "
			+ "WHERE t.capacity >= ?"
			+ " AND t.tableID NOT IN ("
			+ " SELECT s.tableID"
			+ " FROM seating s"
			+ " WHERE s.checkOutTime IS NULL"
			+ " )"
			+ "ORDER BY t.capacity ASC, t.tableNumber ASC"
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
	
	/**
	 * method to count how many table for each capacity
	 * @return a map that connects capacity to the amount of tables for it
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getTotalTablesByCapacity() throws SQLException {	    
	    Map<Integer, Integer> totals = new HashMap<>();

	    try (Connection conn = DBManager.getConnection();
	         PreparedStatement ps = conn.prepareStatement(SELECT_tablesByCapacity);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            totals.put(rs.getInt("capacity"), rs.getInt("total"));
	        }
	    }
	    return totals;
	}

	
}
