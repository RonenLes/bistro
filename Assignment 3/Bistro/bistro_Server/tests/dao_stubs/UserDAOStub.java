package dao_stubs;

import database.UserDAO;
import entities.User;


/**
 * a stub class replaces the dependency on user database for user control tests
 */
public class UserDAOStub extends UserDAO {
	private User userToReturn;

	public UserDAOStub(User userToReturn) {		
		this.userToReturn = userToReturn;
	}
	
	public void setUserToReturn(User userToReturn) {
		this.userToReturn=userToReturn;
	}
	
	@Override
	public User getUserByUsernameAndPassword(String username, String password) {
		if(username.equals(userToReturn.getUsername()) && password.equals(userToReturn.getPassword())) {
			return userToReturn;
		}
		return null;
	}
		
}
