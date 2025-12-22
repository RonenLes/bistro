package requests;

public class SeatingRequest {
	
	private int confirmationCode;

	public SeatingRequest(int confirmationCode) {
		super();
		this.confirmationCode = confirmationCode;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}
	
}
