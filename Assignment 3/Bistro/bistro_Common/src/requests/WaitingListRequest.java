package requests;

import java.time.LocalDate;
import java.time.LocalTime;

public class WaitingListRequest {
	private int confirmationCode;

	public WaitingListRequest(int confirmationCode) {
		super();
		this.confirmationCode = confirmationCode;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}
	
}
