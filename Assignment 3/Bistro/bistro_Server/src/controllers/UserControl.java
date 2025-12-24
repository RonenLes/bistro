package controllers;

import java.sql.SQLException;
import database.UserDAO;
import entities.User;
import responses.LoginResponse;
import responses.LoginResponse.UserReponseCommand;
import responses.Response;
import requests.LoginRequest;

public class UserControl {
	private final UserDAO userDAO;
	
	public UserControl(UserDAO userDAO) {this.userDAO=userDAO;} //constructor for tests
			
	public UserControl() {this(new UserDAO());} //constructors for server use


    public Response<LoginResponse> login(LoginRequest req) throws SQLException {
        User user = userDAO.getUserByUsernameAndPassword(req.getUsername(), req.getPassword());

        if (user == null) {
            return new Response<>(false, "Invalid username or password", null);
        }

        LoginResponse data = new LoginResponse(user.getUserID(), user.getRole(),user.getUsername(),UserReponseCommand.LOGIN_RESPONSE);
        return new Response<>(true, "Hello"+user.getUsername(), data);
    }
    
    public Response<LoginResponse> editDetail(String userID,LoginRequest req) {
    	try {
    		boolean editUser = userDAO.updateUserDetailsByUserID(userID, req.getEmail(), req.getPhone());
    		if(!editUser) return new Response<>(false, "Failed to edit details", null);
    		LoginResponse loginResponse = new LoginResponse(null,null,null,UserReponseCommand.EDIT_RESPONSE);
    		
    		return new Response<>(true,"details edited successfully",loginResponse);
    	}catch(Exception e) {
    		return new Response<>(false, "Failed to edit details", null);
    		
    	}
    }
}
