package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import database.DBManager;
import database.ReservationDAO;
import database.UserDAO;
import entities.User;
import responses.LoginResponse;
import responses.LoginResponse.UserReponseCommand;
import responses.ReservationResponse;
import responses.Response;
import responses.UserHistoryResponse;
import requests.LoginRequest;

public class UserControl {
	private final UserDAO userDAO;
	private final ReservationDAO reservationDAO;
	
	public UserControl(UserDAO userDAO,ReservationDAO reservationDAO) {this.userDAO=userDAO; this.reservationDAO = reservationDAO;} //constructor for tests
			
	public UserControl() {this(new UserDAO(),new ReservationDAO());} //constructors for server use
	
	public Response<LoginResponse> handleUserRequest(LoginRequest req){
		if(req==null) return new Response<>(false, "LoginRequest is missing", null);
		if (req.getUserCommand() == null) return new Response<>(false, "user command is missing", null);
		
		return switch(req.getUserCommand()) {
		
		case LOGIN_REQUEST-> login(req);
		case SHOW_DETAILS_REQUEST->viewUserDetails(req);
		case EDIT_DETAIL_REQUEST -> editDetail(req);
		case HISTORY_REQUEST-> getHistory(req);
		case UPCOMING_RESERVATIONS_REQUEST -> getUpcomingReservations(req);
		};
	}


    public Response<LoginResponse> login(LoginRequest req)  {
    	
    	try (Connection conn=DBManager.getConnection()){
    		System.out.println("DB connection OK: " + conn.getMetaData().getURL());    		
    		User user = userDAO.getUserByUsernameAndPassword(conn,req.getUsername(), req.getPassword());

    		if (user == null) {
    			return new Response<>(false, "Invalid username or password", null);
    		}

    		LoginResponse data = new LoginResponse(user.getUserID(), user.getRole(),user.getUsername(),UserReponseCommand.LOGIN_RESPONSE);
    		return new Response<>(true, "Hello"+user.getUsername(), data);
    		
    	}catch(SQLException e) {
    		System.err.println("LOGIN DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "login db error", null);
    	}catch(Exception e) {
    		System.err.println("LOGIN ERROR: " + e.getMessage());
            e.printStackTrace();
            return new Response<>(false, "login server error", null);
    	}
        
    }
    
    public Response<LoginResponse> editDetail(LoginRequest req) {
    	try (Connection conn = DBManager.getConnection()){
    		User user = userDAO.fetchUserByUsername(conn, req.getUsername());
    		boolean editUser = userDAO.updateUserDetailsByUserID(conn,user.getUserID(), req.getEmail(), req.getPhone());
    		if(!editUser) return new Response<>(false, "Failed to edit details", null);
    		LoginResponse loginResponse = new LoginResponse(null,null,null,UserReponseCommand.EDIT_RESPONSE);
    		
    		return new Response<>(true,"details edited successfully",loginResponse);
    	}catch(SQLException e) {
    		System.err.println("LOGIN DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "login db error", null);
    	}catch(Exception e) {
    		System.err.println("LOGIN ERROR: " + e.getMessage());
            e.printStackTrace();
            return new Response<>(false, "login server error", null);
    	}
    }
    
    public Response<LoginResponse> getHistory(LoginRequest req){
    	try(Connection conn = DBManager.getConnection()){
    		User user = userDAO.fetchUserByUsername(conn, req.getUsername());
    		List<UserHistoryResponse> history = userDAO.getHistoryByUserID(conn, user.getUserID());
    		LoginResponse userHistory = new LoginResponse(UserReponseCommand.HISTORY_RESPONSE,history);
    		return new Response<>(true,"History",userHistory);
    	}catch(SQLException e) {
    		System.err.println("history DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "history db error", null);
    	}catch(Exception e) {
    		System.err.println("history DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "history server error", null);
    	}
    }
    
    public String generateUserID() {
        try (Connection conn = DBManager.getConnection()) {
            String userID;
            do {
                int n = 1 + (int) (Math.random() * 99999); // 1..99999
                userID = String.format("U-%05d", n);       // U-00001 .. U-99999
            } while (userDAO.getUserByUserID(conn, userID) != null);
            return userID;

        } catch (SQLException e) {
            System.out.println("generating userID db fail");
            return null;
        }
    }
    
    /**
     * method for users to changed his email and phone
     * @param req with details to change to
     * @return the same details youve changed
     */
    public Response<LoginResponse> viewUserDetails(LoginRequest req){
    	try(Connection conn = DBManager.getConnection()){
    		User user = userDAO.fetchUserByUsername(conn, req.getUsername());
    		LoginResponse resp = new LoginResponse(UserReponseCommand.SHOW_DETAIL_RESPONSE,user.getEmail(),user.getPhone());
    		return new Response<>(true,"Your current details",resp);
    	}catch(Exception e) {
    		System.err.println("user DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "user details server error", null);
    	}
    }
    
    
    /**
     * method to 
     * @param req
     * @return
     */
    public Response<LoginResponse> getUpcomingReservations(LoginRequest req){
    	try(Connection conn = DBManager.getConnection()){
    		User user = userDAO.fetchUserByUsername(conn, req.getUsername());
    		if (user == null) {
    			return new Response<>(false, "User not found", null);
    		}
    		List<UserHistoryResponse> upcoming = reservationDAO.fetchUpcomingReservationsByUser(conn, user.getUserID());
    		LoginResponse resp = new LoginResponse(UserReponseCommand.UPCOMING_RESERVATIONS_RESPONSE,upcoming);
    		return new Response<>(true, "Upcoming reservations", resp);
    	}catch(SQLException e) {
    		System.err.println("upcoming reservations DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "upcoming reservations db error", null);
    	}catch(Exception e) {
    		System.err.println("upcoming reservations ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "upcoming reservations server error", null);
    	}
    }
    
    
}
