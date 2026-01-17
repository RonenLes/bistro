package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Table;
import requests.TableInfo;

/**
 * DAO for the {@code restaurant_table} table.
 *
 * <p>Main idea: manages the restaurant's physical tables and their capacities, including:
 * <ul>
 *   <li>Creating and updating tables (table number + capacity)</li>
 *   <li>Fetching table details (by ID) and listing all active tables</li>
 *   <li>Finding an available (not currently seated) table for a requested capacity</li>
 *   <li>Capacity helpers (minimal/rounded capacity, totals by capacity, active counts)</li>
 *   <li>Operational checks (is a table currently occupied)</li>
 *   <li>Soft-deleting a table by deactivating it ({@code isActive = 0})</li>
 * </ul>
 *
 * <p>All methods assume "active tables" are rows where {@code isActive = 1}.
 */
public class TableDAO {
	
	//INSERT
	private final String INSERT_newTable ="INSERT INTO `restaurant_table` " + "(tableNumber, capacity, isActive) " +"VALUES(?, ?, ?)";
			
	
	//SELECT
	private static final String SELECT_CAPACITY_BY_TABLE_NUMBER ="SELECT capacity FROM restaurant_table WHERE tableNumber = ? AND isActive = 1";
	private static final String SELECT_IS_TABLE_OCCUPIED_NOW_BY_NUMBER ="SELECT 1 " +"FROM seating s JOIN restaurant_table t ON t.tableID = s.tableID " +
																		"WHERE t.tableNumber = ? AND s.checkOutTime IS NULL LIMIT 1";  
	private static final String SELECT_ACTIVE_COUNT_BY_CAPACITY ="SELECT COUNT(*) FROM restaurant_table WHERE isActive = 1 AND capacity = ?";	   	                         																	                     
	private final String SELECT_ALL_TABLES ="SELECT * FROM `restaurant_table` WHERE isActive = 1";
	private final String SELECT_TABLE_BY_ID = "SELECT * FROM `restaurant_table` WHERE tableID = ? AND isActive = 1";
	private final String SELECT_minimalTableSize = "SELECT MIN(capacity) as roundedUp FROM `restaurant_table` WHERE isActive = 1 AND capacity >= ?";
	private final String SELECT_tablesByCapacity = "SELECT capacity, COUNT(*) AS total FROM restaurant_table WHERE isActive = 1 GROUP BY capacity";								
	private static final String SELECT_availableTable ="SELECT t.tableID, t.tableNumber, t.capacity "
			+ "FROM `restaurant_table` t "
			+ "WHERE t.isActive = 1 AND t.capacity >= ? "
			+ "AND t.tableID NOT IN "
			+ "(SELECT s.tableID "
			+ "FROM seating s "
			+ "WHERE s.checkOutTime IS NULL) "
			+ "ORDER BY t.capacity ASC, t.tableNumber ASC "
			+ "LIMIT 1";
	
	//UPDATE
	private final String UPDATE_TABLE_BY_TABLE_NUMBER = "UPDATE `restaurant_table` SET capacity = ? WHERE tableNumber = ?";
	private static final String UPDATE_DEACTIVATE_TABLE_BY_NUMBER ="UPDATE restaurant_table SET isActive = 0 WHERE tableNumber = ? AND isActive = 1";
			    	
	
	public Table fetchTableByID(Connection conn,int tableID)throws SQLException {
		try(PreparedStatement ps = conn.prepareStatement(SELECT_TABLE_BY_ID)){
			ps.setInt(1, tableID);
			ResultSet rs = ps.executeQuery();
			if(!rs.next()) return null;
			return new Table(rs.getInt("tableID"),rs.getInt("tableNumber"),rs.getInt("capacity"));			
		}
	}
	
	/**
	 * adding new table to the resturant_table in database
	 * @param tableNumber should be checked for uniqueness 
	 * @param capacity 
	 * @return boolean if succedded 
	 * @throws SQLException
	 */
	public boolean insertNewTable(Connection conn,int tableNumber,int capacity) throws SQLException {
		
		try(PreparedStatement ps = conn.prepareStatement(INSERT_newTable)) {
			ps.setInt(1,tableNumber);
			ps.setInt(2, capacity);
			ps.setBoolean(3, true);
			int isInserted = ps.executeUpdate();
			return isInserted ==1;
			
		}catch(SQLException e) {
			if (e.getErrorCode() == 1062) return false;
			throw e;
		}
		
	}
	
	/**
	 * method to count how many table for each capacity
	 * @return a map that connects capacity to the amount of tables for it
	 * @throws SQLException
	 */
	public Map<Integer, Integer> getTotalTablesByCapacity(Connection conn) throws SQLException {	    
	    Map<Integer, Integer> totals = new HashMap<>();

	    try (PreparedStatement ps = conn.prepareStatement(SELECT_tablesByCapacity);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            totals.put(rs.getInt("capacity"), rs.getInt("total"));
	        }
	    }
	    return totals;
	}
	
	/**
	 * getting the closest table capacity to a given party size 
	 * @param partySize of the current desired reservation
	 * @return closest capacity of a table to the give partySize 
	 * @throws SQLException
	 */
	public int getMinimalTableSize(Connection conn,int partySize) throws SQLException{
		
		try (PreparedStatement ps = conn.prepareStatement(SELECT_minimalTableSize)){
		     ps.setInt(1,partySize);			
		     ResultSet rs = ps.executeQuery();
			if(rs.next()) return rs.getInt("roundedUp");
			
		}catch(SQLException e) {
			System.err.println("Database error: cant fetch minimal size");
			throw e;
		}
		return -1;
	}
	
		
	/**
	 * fetch an available table that is not being used
	 * @param conn
	 * @param allocatedCapacity
	 * @return Table entity with all its details
	 * @throws SQLException
	 */
	public Table findAvailableTable(Connection conn,int allocatedCapacity) throws SQLException{
		
		Table table = null;
		
		try (PreparedStatement ps = conn.prepareStatement(SELECT_availableTable);){					
			ps.setInt(1, allocatedCapacity);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				table = new Table(rs.getInt("tableID"),rs.getInt("tableNumber"),rs.getInt("capacity"));
			}
			return table;
			
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert find a table");
			throw e;
		}
				
	}
	
	/**
	 * update an existing table by table number (unique)
	 * @param conn
	 * @param tableNumber
	 * @param newCap
	 * @return true if success
	 * @throws SQLException
	 */
	public boolean updateTableByTableNumber(Connection conn,int tableNumber,int newCap)throws SQLException{
		try(PreparedStatement ps = conn.prepareStatement(UPDATE_TABLE_BY_TABLE_NUMBER)){
			ps.setInt(1, newCap);
			ps.setInt(2, tableNumber);
			int insert = ps.executeUpdate();
			return insert == 1;
		}
	}
	
	/**
	 * fetching all tables that isAvailable =1 
	 * @param conn
	 * @return List<TableInfo> number table and cap
	 * @throws SQLException
	 */
	public List<TableInfo> fetchAllTables(Connection conn)throws SQLException{
		List<TableInfo> tables = new ArrayList<>();
		try(PreparedStatement ps = conn.prepareStatement(SELECT_ALL_TABLES)){
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				tables.add(new TableInfo(rs.getInt("tableNumber"),rs.getInt("capacity")));
			}
			return tables;
		}
	}
	
	/**
	 * fetching the capacity of the table by its number
	 * @param conn
	 * @param tableNumber
	 * @return
	 * @throws SQLException
	 */
	public Integer getCapacityByTableNumber(Connection conn, int tableNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_CAPACITY_BY_TABLE_NUMBER)) {
            ps.setInt(1, tableNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
	
	/**
	 * checking if a table is currenctly occupied in seating
	 * @param conn
	 * @param tableNumber
	 * @return
	 * @throws SQLException
	 */
	public boolean isTableOccupiedNow(Connection conn, int tableNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_IS_TABLE_OCCUPIED_NOW_BY_NUMBER)) {
            ps.setInt(1, tableNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
	
	/**
	 * count how many tables are active
	 * @param conn
	 * @param capacity
	 * @return
	 * @throws SQLException
	 */
	public int countActiveTablesByCapacity(Connection conn, int capacity) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_COUNT_BY_CAPACITY)) {
	        ps.setInt(1, capacity);
	        try (ResultSet rs = ps.executeQuery()) {
	            return rs.next() ? rs.getInt(1) : 0;
	        }
	    }
	}
	
	/**
	 * set isAvailable = 0 (like delete)
	 * @param conn
	 * @param tableNumber
	 * @return
	 * @throws SQLException
	 */
	public boolean deactivateTableByNumber(Connection conn, int tableNumber) throws SQLException {
	    try (PreparedStatement ps = conn.prepareStatement(UPDATE_DEACTIVATE_TABLE_BY_NUMBER)) {
	        ps.setInt(1, tableNumber);
	        return ps.executeUpdate() == 1;
	    }
	}

	
}
