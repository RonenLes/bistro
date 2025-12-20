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
	private final String SELECT_minimalTableSize = "SELECT MIN(capacity) as roundedUp FROM `restaurant_table` WHERE capacity >= ?";
	private final String SELECT_tablesByCapacity = "SELECT capacity, COUNT(*) AS total FROM restaurant_table GROUP BY capacity";								
	private static final String SELECT_availableTable =
		    "SELECT t.tableID, t.tableNumber " +
		    "FROM `table` t " +
		    "WHERE t.capacity >= ? " +
		    "  AND t.tableID NOT IN ( " +
		    "      SELECT s.tableID " +
		    "      FROM seating s " +
		    "      WHERE s.checkOutTime IS NULL " +
		    "  ) " +
		    "ORDER BY t.capacity ASC, t.tableNumber ASC " +
		    "LIMIT 1";

	
	
	
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
	
	/**
	 * 
	 * @param partySize of the current desired reservation
	 * @return closest capacity of a table to the give partySize 
	 * @throws SQLException
	 */
	public int getMinimalTableSize(int partySize) throws SQLException{
		
		try (Connection conn = DBManager.getConnection();
		     PreparedStatement ps = conn.prepareStatement(SELECT_minimalTableSize);
		     ResultSet rs = ps.executeQuery()){
			
			if(rs.next()) return rs.getInt("roundedUp");
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert new table");
			
		}
		return -1;
	}
	
	

	
}
