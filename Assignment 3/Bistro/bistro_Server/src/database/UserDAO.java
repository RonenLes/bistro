package database;

import entities.User;
import responses.UserHistoryResponse;

import java.sql.*;
import java.util.List;



import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalTime;


/**
 * DAO for the {@code user} table.
 *
 * <p>Main idea: handles database operations related to users (mainly subscribers), including:
 * <ul>
 *   <li>Creating a new user</li>
 *   <li>Fetching users (by login credentials, by userID, by username, or all subscribers)</li>
 *   <li>Updating user contact details (email/phone)</li>
 *   <li>Fetching a user's reservation/billing history via joins with reservation/seating/table/bill</li>
 * </ul>
 *
 * <p>Note: login/user-fetch queries intentionally do not expose the user's password in returned {@link entities.User} objects.
 */
public class UserDAO {
	
	//INSERT 
		private final String INSERT_NEW_USER = "INSERT INTO user (userID, username, password, role, phone, email) VALUES(?, ?, ?, ?, ?, ?)";

	//SELECT statements
		private final String SELECT_USER_BY_USERNAME = "SELECT * FROM `user` WHERE username = ?";
		private final String SELECT_ALL_SUBSCRIBER = "SELECT * FROM `user` WHERE role = 'SUBSCRIBER'";
		private static final String SELECT_LOGIN ="SELECT userID, username, role, phone, email FROM `user` WHERE username= ? AND password= ?";
		private static final String SELECT_USER_BY_ID ="SELECT userID, username, role, phone, email FROM `user` WHERE userID = ?";
		private static final String SELECT_HISTORY ="SELECT r.reservationDate, r.startTime, r.partySize, r.status, s.checkInTime, s.checkOutTime, t.tableNumber, b.totalPrice "+
													"FROM reservation r " +
													"LEFT JOIN seating s ON s.reservationID = r.reservationID "+
													"LEFT JOIN restaurant_table t ON t.tableID = s.tableID "+
													"LEFT JOIN bill b ON b.seatingID = s.seatingID "+
													"WHERE r.userID = ? "+
													"ORDER BY r.reservationDate DESC, r.startTime";
		
	//UPDATE
		private final String UPDATE_USER_DETAILS_BY_ID = "UPDATE `user` SET phone = ?, email = ? WHERE userID = ?";
		
		/**
		 * method for fetching all user from database 
		 * @param conn from the pool
		 * @return list of user entities containing userID,username,password,role,phone,email
		 * @throws SQLException
		 */
		public List<User> fetchAllUsers(Connection conn)throws SQLException{
			List<User> users = new ArrayList<>();
			try(PreparedStatement ps = conn.prepareStatement(SELECT_ALL_SUBSCRIBER)){
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					User user = new User(
							rs.getString("userID"),
							rs.getString("username"),
							rs.getString("password"),
							rs.getString("role"),
							rs.getString("phone"),
							rs.getString("email"));
					users.add(user);
				}
				return users;
			}
		}
		
		/**
		 * method to insert new user to the database
		 * @param userID
		 * @param username
		 * @param password
		 * @param role
		 * @param phone
		 * @param email
		 * @return true if database updated successfully
		 * @throws SQLException
		 */
		public boolean insertNewUser(String userID,String username,String password,String role,String phone, String email)throws SQLException{
			try(Connection conn  = DBManager.getConnection();
					PreparedStatement ps = conn.prepareStatement(INSERT_NEW_USER)){
				ps.setString(1, userID);
				ps.setString(2, username);
				ps.setString(3, password);
				ps.setString(4, role);
				ps.setString(5, phone);
				ps.setString(6, email);
				
				int insert = ps.executeUpdate();
				return insert == 1;				
			}
		}
		
		
		/**
		 * method to get user details by username and password
		 * @param username- username field
		 * @param password- password field
		 * @return the user entity with matching username and password,null if there are no matches.
		 * @throws SQLException if there is a database error
		 */
		public User getUserByUsernameAndPassword(Connection conn,String username, String password) throws SQLException {

		    try (PreparedStatement ps = conn.prepareStatement(SELECT_LOGIN)) {

		        ps.setString(1, username);
		        ps.setString(2, password);

		        ResultSet rs = ps.executeQuery();

		        if (rs.next()) {
		        	User user = new User(
		        		    rs.getString("userID"),
		        		    rs.getString("username"),
		        		    null,                    // password is not exposed
		        		    rs.getString("role"),
		        		    rs.getString("phone"),
		        		    rs.getString("email")
		        		);		        	
		        	return user;
		        }

		    } catch (SQLException e) {
		        System.err.println("DB error during login");
		           
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
		    public User getUserByUserID(Connection conn,String userID) throws SQLException {

		        try (PreparedStatement ps = conn.prepareStatement(SELECT_USER_BY_ID)) {
		            ps.setString(1, userID);
		            ResultSet rs = ps.executeQuery();

		            if (rs.next()) {
		            	return new User(
		            		    rs.getString("userID"),
		            		    rs.getString("username"),
		            		    null,                    // password is not exposed
		            		    rs.getString("role"),
		            		    rs.getString("phone"),
		            		    rs.getString("email")
		            		);
		            }
		        } catch (SQLException e) {
		            System.err.println("DB error fetching user by userID");
		            throw e;
		        }

		        return null;
		    }
		    
		    
		    /**
		     * update user email and phone in database 
		     * @param conn
		     * @param userID
		     * @param email
		     * @param phone
		     * @return true if success
		     * @throws SQLException
		     */
		    public boolean updateUserDetailsByUserID(Connection conn,String userID,String email,String phone)throws SQLException{
		    	try(PreparedStatement ps = conn.prepareStatement(UPDATE_USER_DETAILS_BY_ID)){
		    		ps.setString(1, phone);
		    		ps.setString(2, email);
		    		ps.setString(3,userID);
		    		return ps.executeUpdate() == 1;		    		
		    	}		    			    	
		    }
		    
		    
		    /**
		     * method to fetch user history by userID after checkout 
		     * @param conn
		     * @param userID
		     * @return List<UserHistoryResponse> containing reservationDate,startTime,check in/out time, table number, total price,partySize, status
		     * @throws SQLException
		     */
		    public List<UserHistoryResponse> getHistoryByUserID(Connection conn,String userID) throws SQLException{
		    	List<UserHistoryResponse> historyList = new ArrayList<>();
		    	
		    	try(PreparedStatement ps = conn.prepareStatement(SELECT_HISTORY)){
		    		ps.setString(1, userID);
		    		ResultSet rs = ps.executeQuery();
		    		
		    		while(rs.next()) {
		    			LocalDate resDate = rs.getDate("reservationDate").toLocalDate();
		                LocalTime reservedFor = rs.getTime("startTime").toLocalTime();
		                
		                Time inTime = rs.getTime("checkInTime");
		                LocalTime checkIn = (inTime == null) ? null : inTime.toLocalTime();
		                
		                Time outTime = rs.getTime("checkOutTime");
		                LocalTime checkOut = (outTime == null) ? null : outTime.toLocalTime();
		                
		                int tableNumber = rs.getInt("tableNumber");
		                
		                Double totalPrice = rs.getObject("totalPrice") == null ? null : rs.getDouble("totalPrice");
		                
		                int partySize = rs.getInt("partySize");
		                String status = rs.getString("status");
		                
		                historyList.add(new UserHistoryResponse(resDate,reservedFor,checkIn,checkOut,tableNumber,totalPrice,partySize,status));
		                
		    		}
		    	}
		    	return historyList;
		    }
		    
		    /**
		     * method to fetch user detail by username
		     * @param conn
		     * @param username
		     * @return User entity containing userID,username,role,phone,email
		     * @throws SQLException
		     */
		    public User fetchUserByUsername(Connection conn,String username)throws SQLException{
		    	try(PreparedStatement ps = conn.prepareStatement(SELECT_USER_BY_USERNAME)){
		    		ps.setString(1,username);
		    		ResultSet rs = ps.executeQuery();
		    		if(rs.next()) {
		    			return new User(
		            		    rs.getString("userID"),
		            		    rs.getString("username"),
		            		    null,                    
		            		    rs.getString("role"),
		            		    rs.getString("phone"),
		            		    rs.getString("email")
		            		);
		    		}
		    		return null;
		    	}
		    }
		    
}
