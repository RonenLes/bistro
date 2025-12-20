package controllers;

import java.sql.SQLException;
import database.UserDAO;
import entities.User;
import responses.LoginResponse;
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

        LoginResponse data = new LoginResponse(user.getUserID(), user.getRole(),user.getUsername());
        return new Response<>(true, "Hello"+user.getUsername(), data);
    }
}
