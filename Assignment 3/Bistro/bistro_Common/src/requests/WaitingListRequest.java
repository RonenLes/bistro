package requests;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * Request payload used for waiting list actions that are identified by a reservation confirmation code.
 *
 * <p>Main idea:
 * The client sends a confirmation code and the server uses it to locate the relevant reservation
 * and its waiting-list entry.</p>
 *
 * <p>Main fields:
 * <ul>
 *   <li>{@code confirmationCode} - reservation confirmation code</li>
 * </ul>
 */
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
