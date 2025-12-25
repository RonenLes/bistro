package controllers;

import java.sql.Connection;
import java.sql.SQLException;

import database.DBManager;
import database.UserDAO;
import entities.User;
import responses.LoginResponse;
import responses.LoginResponse.UserReponseCommand;
import responses.Response;
import requests.LoginRequest;
import java.sql.Connection;
public class UserControl {
	private final UserDAO userDAO;
	
	public UserControl(UserDAO userDAO) {this.userDAO=userDAO;} //constructor for tests
			
	public UserControl() {this(new UserDAO());} //constructors for server use
	
	public Response<LoginResponse> handleUserRequest(LoginRequest req,String userID){
		if(req==null) new Response<>(false, "LoginRequest is missing", null);
		if (req.getUserCommand() == null) return new Response<>(false, "user command is missing", null);
		
		return switch(req.getUserCommand()) {
		
		case LOGIN_REQUEST-> login(req);
		case EDIT_DETAIL_REQUEST -> editDetail(userID, req);
		};
	}


    public Response<LoginResponse> login(LoginRequest req)  {
    	
    	try (Connection conn=DBManager.getConnection()){
    		User user = userDAO.getUserByUsernameAndPassword(conn,req.getUsername(), req.getPassword());

    		if (user == null) {
    			return new Response<>(false, "Invalid username or password", null);
    		}

    		LoginResponse data = new LoginResponse(user.getUserID(), user.getRole(),user.getUsername(),UserReponseCommand.LOGIN_RESPONSE);
    		return new Response<>(true, "Hello"+user.getUsername(), data);
    		
    	}catch(Exception e) {
    		return new Response<>(false, "login db error", null);
    	}
        
    }
    
    public Response<LoginResponse> editDetail(String userID,LoginRequest req) {
    	try (Connection conn = DBManager.getConnection()){
    		
    		boolean editUser = userDAO.updateUserDetailsByUserID(conn,userID, req.getEmail(), req.getPhone());
    		if(!editUser) return new Response<>(false, "Failed to edit details", null);
    		LoginResponse loginResponse = new LoginResponse(null,null,null,UserReponseCommand.EDIT_RESPONSE);
    		
    		return new Response<>(true,"details edited successfully",loginResponse);
    	}catch(Exception e) {
    		return new Response<>(false, "Failed to edit details", null);
    		
    	}
    }
}
