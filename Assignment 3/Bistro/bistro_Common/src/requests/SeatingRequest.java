package requests;

public class SeatingRequest {
	
	private int confirmationCode;
	
	public SeatingRequest() {};
	
	public SeatingRequest(int confirmationCode) {

		this.confirmationCode = confirmationCode;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}
	
}
