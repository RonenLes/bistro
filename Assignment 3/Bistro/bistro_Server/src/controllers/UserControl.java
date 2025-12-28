package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import database.DBManager;
import database.UserDAO;
import entities.User;
import responses.LoginResponse;
import responses.LoginResponse.UserReponseCommand;
import responses.Response;
import responses.UserHistoryResponse;
import requests.LoginRequest;

public class UserControl {
	private final UserDAO userDAO;
	
	public UserControl(UserDAO userDAO) {this.userDAO=userDAO;} //constructor for tests
			
	public UserControl() {this(new UserDAO());} //constructors for server use
	
	public Response<?> handleUserRequest(LoginRequest req,String userID){
		if(req==null) return new Response<>(false, "LoginRequest is missing", null);
		if (req.getUserCommand() == null) return new Response<>(false, "user command is missing", null);
		
		return switch(req.getUserCommand()) {
		
		case LOGIN_REQUEST-> login(req);
		case EDIT_DETAIL_REQUEST -> editDetail(userID, req);
		case HISTORY_REQUEST-> getHistory(userID);
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
    
    public Response<LoginResponse> editDetail(String userID,LoginRequest req) {
    	try (Connection conn = DBManager.getConnection()){
    		
    		boolean editUser = userDAO.updateUserDetailsByUserID(conn,userID, req.getEmail(), req.getPhone());
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
    
    public Response<List<UserHistoryResponse>> getHistory(String userID){
    	try(Connection conn = DBManager.getConnection()){
    		List<UserHistoryResponse> history = userDAO.getHistoryByUserID(conn, userID);
    		return new Response<>(true,"History",history);
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
    	String userID = null;
    	try (Connection conn = DBManager.getConnection()){
    		do {
    			int n = +100000 + (int) (Math.random() * 900000);
    			userID = "U-"+n;
    		}while(userDAO.getUserByUserID(conn, userID)!=null);
    		return userID;
    	}catch(SQLException e){
    		System.out.println("generating userID db fail");
    		return null;
    	}
    	
    }
    
    
}
