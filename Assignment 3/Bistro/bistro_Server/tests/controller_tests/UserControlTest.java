package controller_tests;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import controllers.UserControl;
import dao_stubs.UserDAOStub;
import entities.User;
import requests.LoginRequest;
import responses.*;


/**
 * test class for UserControl
 * Test in this class:
 * -correct login
 * -wrong password
 * -wrong username
 */
public class UserControlTest {
	
	private UserDAOStub nir;
	private UserDAOStub ronen;

	
	@Before
	public void setup() {
		nir = new UserDAOStub(new User("212385223","xNIRx","1234","SUBSCRIBER"));
		ronen = new UserDAOStub(new User("319002812","KingRonen","4321","MANAGER"));
	}
	
	/**
	 * test correct login attempt 
	 * @throws Exception
	 */
	@Test
	public void login_success_returnsSuccessResponse() throws Exception {
	    

	    UserControl control = new UserControl(nir);

	    LoginRequest req = new LoginRequest("xNIRx","1234");
	    Response<LoginResponse> resp = control.login(req);

	    assertTrue(resp.isSuccess());
	    assertEquals("212385223", resp.getData().getUserID());
	}
	
	
	/**
	 * check if UserControl correctly handles wrong password
	 * @throws Exception
	 */
	@Test
	public void login_failPassword_returnsFailedResponse() throws Exception {
	    

	    UserControl control = new UserControl(ronen);

	    LoginRequest req = new LoginRequest("KingRonen","1234");
	    Response<LoginResponse> resp = control.login(req);

	    assertFalse(resp.isSuccess());
	    assertEquals("Invalid username or password", resp.getMessage());
	    
	}
	
	/**
	 * test UserControl handles wrong username correctly
	 * @throws Exception
	 */
	@Test
	public void login_failUsername_returnsFailedResponse() throws Exception {
	    

	    UserControl control = new UserControl(ronen);

	    LoginRequest req = new LoginRequest("Ronen","4321");
	    Response<LoginResponse> resp = control.login(req);

	    assertFalse(resp.isSuccess());
	    assertEquals("Invalid username or password", resp.getMessage());	    
	}
}
