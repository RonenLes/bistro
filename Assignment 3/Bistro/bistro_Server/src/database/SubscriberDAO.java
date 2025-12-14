package database;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import entities.Subscriber;
import java.time.LocalDate;


/**
 * METHODS THIS CLASS HAS:
 * 1.getSubscriberByID - fetching subscriber with subscriberID from database
 */

public class SubscriberDAO {
	
	
	//SELECT statements
	private final String SELECT_subscriberById = "SELECT * FROM `subscriber` WHERE subscriberID = ?";
	
	
	
	
	
	/**
	 * retrieving subscriber entity from the database by unique id
	 * --for the future if another DAO will need to use overload method in signature with Connection conn--
	 * subscriberID used only for database logic not client related logic
	 * 
	 * @param subscriberID
	 * @return Subscriber object
	 * @throws SQLException if could not find one match
	 */
	public Subscriber getSubscriberByID(String subscriberID) throws SQLException {
		
		Subscriber subscriber = null;
		
		try(Connection conn = DBManager.getConnection();
			PreparedStatement ps = conn.prepareStatement(SELECT_subscriberById)){
			
			ps.setString(1, subscriberID);
			ResultSet rs = ps.executeQuery();
			
			if(rs.next()) {
								
				subscriber = new Subscriber(
						rs.getString("subscriberID"),
						rs.getString("userID"),
						rs.getInt("cardCode"),
						rs.getString("phoneNumber"),
						rs.getString("email"),
						rs.getDate("dateOfJoining").toLocalDate());
			}			
		}catch(SQLException e) {
			System.err.println("Database error Could not find subscriber by id");
		}
		return subscriber;
	}
	
	
	
}
