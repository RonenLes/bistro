package database;

import java.sql.*;
import entities.Table;

public class TableDAO {
	
	//INSERT
	private final String INSERT_newTable ="INSERT INTO `table` " + "(tableNumber, capacity) " +"(?, ?)";
	
	//SELECT
	private final String SELECT_tableByTableNumber = "SELECT * FROM `table` WHERE tableNumber = ?";
	
	//UPDATE
	private final String UPDATE_tableCapacityByTableNumber = "UPDATE `table` SET capacity = ? WHERE tableNumber = ?";
	
	public boolean insertNewTable(int tableNumber,int capacity) {
		
		
	}
}
