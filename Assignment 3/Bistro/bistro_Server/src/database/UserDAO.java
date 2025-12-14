package database;

import entities.User;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;


/**
 * METHODS THIS CLASS HAS:
 * 1.getUserByUsername - fetching user with username from database
 */
public class UserDAO {

	//SELECT statements
	private final String SELECT_userByUsername = "SELECT * FROM `user` WHERE username = ?";
	
	
	/**
	 * method to retrieve user from database so the control can check role and existence 
	 * @param username
	 * @return User entity
	 * @throws SQLException
	 */
	public User getUserByUsername(String username) throws SQLException{
		User user  = null;
		
		try(Connection conn = DBManager.getConnection();
			PreparedStatement ps = conn.prepareStatement(SELECT_userByUsername)){
			
			ps.setString(1, username);
			
			ResultSet rs = ps.executeQuery();
			
			if(rs.next()) {
				user = new User(
						rs.getString("userID"),
						rs.getString("username"),
						rs.getString("password"),
						rs.getString("role"));
			}
		}catch(Exception e) {
			System.err.println("Database error Could not find username");			
		}
		return user;
	}
}
