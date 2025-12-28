package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entities.Table;
import requests.TableInfo;

public class TableDAO {
	
	//INSERT
	private final String INSERT_newTable ="INSERT INTO `restaurant_table` " + "(tableNumber, capacity) " +"(?, ?)";
	
	//SELECT
	private final String SELECT_ALL_TABLES ="SELECT * FROM `restaurant_table`";
	private final String SELECT_TABLE_BY_ID = "SELECT * FROM `restaurant_table` WHERE tableID = ?";
	private final String SELECT_minimalTableSize = "SELECT MIN(capacity) as roundedUp FROM `restaurant_table` WHERE capacity >= ?";
	private final String SELECT_tablesByCapacity = "SELECT capacity, COUNT(*) AS total FROM restaurant_table GROUP BY capacity";								
	private static final String SELECT_availableTable ="SELECT t.tableID, t.tableNumber, t.capacity "
			+ "FROM `restaurant_table` t "
			+ "WHERE t.capacity >= ? "
			+ "AND t.tableID NOT EXISTS "
			+ "(SELECT s.tableID "
			+ "FROM seating s "
			+ "WHERE s.checkOutTime IS NULL) "
			+ "ORDER BY t.capacity ASC, t.tableNumber ASC "
			+ "LIMIT 1";
	
	//UPDATE
	private final String UPDATE_TABLE_BY_TABLE_NUMBER = "UPDATE `restaurant_table` SET capacity = ? WHERE tableNumber = ?";
			    	
	
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
	 * 
	 * @param partySize of the current desired reservation
	 * @return closest capacity of a table to the give partySize 
	 * @throws SQLException
	 */
	public int getMinimalTableSize(Connection conn,int partySize) throws SQLException{
		
		try (PreparedStatement ps = conn.prepareStatement(SELECT_minimalTableSize);
		     ResultSet rs = ps.executeQuery()){
			
			if(rs.next()) return rs.getInt("roundedUp");
			
		}catch(SQLException e) {
			System.err.println("Database error: could not insert new table");
			throw e;
		}
		return -1;
	}
	
	
	public Table findAvailableTable(int allocatedCapacity) throws SQLException {
	    try (Connection conn = DBManager.getConnection()) {
	        return findAvailableTable(conn, allocatedCapacity); // delegate
	    }
	}
	
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
	
	
	public boolean updateTableByTableNumber(Connection conn,int tableNumber,int newCap)throws SQLException{
		try(PreparedStatement ps = conn.prepareStatement(UPDATE_TABLE_BY_TABLE_NUMBER)){
			ps.setInt(1, newCap);
			ps.setInt(2, tableNumber);
			int insert = ps.executeUpdate();
			return insert == 1;
		}
	}
	
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
	
	
}
