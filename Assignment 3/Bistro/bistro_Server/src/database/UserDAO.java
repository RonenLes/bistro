package database;

import entities.User;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;


/**
 * METHODS THIS CLASS HAS:
 * 1.getUserByUsernameAndPassword - fetching user with username and password from database
 */
public class UserDAO {

	//SELECT statements
		private static final String SELECT_LOGIN ="SELECT userID, username, role, phoneNumber, email FROM `user` WHERE username=? AND password=?";
		private static final String SELECT_USER_BY_ID ="SELECT userID, username, role, phoneNumber, email FROM `user` WHERE userID = ?";
		
	//UPDATE
		private final String UPDATE_USER_DETAILS_BY_ID = "UPDATE `user` SET phone = ?, email = ? WHERE userID = ?";
		
		/**
		 * @param username- username field
		 * @param password- password field
		 * @return the user with matching username and password,null if there are no matches.
		 * @throws SQLException if there is a database error
		 */
		public User getUserByUsernameAndPassword(String username, String password) throws SQLException {

		    try (Connection conn = DBManager.getConnection();
		         PreparedStatement ps = conn.prepareStatement(SELECT_LOGIN)) {

		        ps.setString(1, username);
		        ps.setString(2, password);

		        ResultSet rs = ps.executeQuery();

		        if (rs.next()) {
		        	return new User(
		        		    rs.getString("userID"),
		        		    rs.getString("username"),
		        		    null,                    // password is not exposed
		        		    rs.getString("role"),
		        		    rs.getString("phoneNumber"),
		        		    rs.getString("email")
		        		);
		        }

		    } catch (SQLException e) {
		        System.err.println("DB error during login");
		        throw e;   // חשוב: אל תבלע את החריגה
		    }

		    return null;
		}

		
		
		/**
		 * Fetches a user by userID.
		 *
		 * @param userID the unique user identifier
		 * @return User object if found, null otherwise
		 * @throws SQLException if a database error occurs
		 */
		    public User getUserByUserID(String userID) throws SQLException {

		        try (Connection conn = DBManager.getConnection();
		             PreparedStatement ps = conn.prepareStatement(SELECT_USER_BY_ID)) {

		            ps.setString(1, userID);
		            ResultSet rs = ps.executeQuery();

		            if (rs.next()) {
		            	return new User(
		            		    rs.getString("userID"),
		            		    rs.getString("username"),
		            		    null,                    // password is not exposed
		            		    rs.getString("role"),
		            		    rs.getString("phoneNumber"),
		            		    rs.getString("email")
		            		);
		            }
		        } catch (SQLException e) {
		            System.err.println("DB error fetching user by userID");
		            throw e;
		        }

		        return null;
		    }
		    
		    
		    public boolean updateUserDetailsByUserID(String userID,String email,String phone)throws SQLException{
		    	try(Connection conn = DBManager.getConnection();
		             PreparedStatement ps = conn.prepareStatement(UPDATE_USER_DETAILS_BY_ID)){
		    		ps.setString(1, phone);
		    		ps.setString(2, email);
		    		ps.setString(3,userID);
		    		return ps.executeUpdate() == 1;		    		
		    	}		    			    	
		    }
}
