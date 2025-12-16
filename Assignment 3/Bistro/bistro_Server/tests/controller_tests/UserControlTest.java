package controller_tests;

import static org.junit.Assert.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import controllers.UserControl;
import dao_stubs.UserDAOStub;
import entities.User;
import requests.LoginRequest;
import responses.*;



public class UserControlTest {
	
	private UserDAOStub nir;
	private UserDAOStub ronen;

	
	@BeforeEach
	void setup() {
		nir = new UserDAOStub(new User("212385223","xNIRx","1234","SUBSCRIBER"));
		ronen = new UserDAOStub(new User("319002812","KingRonen","4321","MANAGER"));
	}
	

	@Test
	void login_success_returnsSuccessResponse() throws Exception {
	    

	    UserControl control = new UserControl(nir);

	    LoginRequest req = new LoginRequest("xNIRx","1234");
	    Response<LoginResponse> resp = control.login(req);

	    assertTrue(resp.isSuccess());
	    assertEquals("212385223", resp.getData().getUserID());
	}
}
