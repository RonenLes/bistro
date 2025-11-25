package db;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class OrderDB {

	private DBManager dbManager;
	
	//SELECT statements
	private static final String showAllOrders = "SELECT * FROM 'reservations'";
	private static final String showOrdersByDate = "SELECT * FROM 'reservations' WHERE reservationDate = ?";
	
	public OrderDB() {
		this.dbManager = DBManager.getInstance();
	}
	
	public List<Order> readAllOrders() throws SQLException{
		
		List<Order> orders = new ArrayList<>();
		
		try {
			Connection conn = dbManager.getConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(showAllOrders);
			
			while(rs.next()) {
				Order order = new Order()
			}
		}
	}
	
}
