package requests;


/**
 * Request payload for billing actions.
 *
 * <p>Main idea:
 * Identifies the target reservation using {@code confirmationCode} and specifies the billing action
 * using {@link BillRequestType} (view bill vs pay bill).</p>
 *
 * <p>Main fields:
 * <ul>
 *   <li>{@code confirmationCode} - reservation confirmation code</li>
 *   <li>{@link BillRequestType} - billing action type</li>
 * </ul>
 */
public class BillRequest {
	
	private BillRequestType type;
	private int confirmationCode;
	
	
	public enum BillRequestType{
		REQUEST_TO_SEE_BILL,
		PAY_BILL
	}
	
	public BillRequest() {}
	
	public BillRequest(int confirmationCode,BillRequestType type) {
		this.confirmationCode = confirmationCode;
		this.type=type;
	}
	
	public BillRequest(BillRequestType type, int confirmationCode) {

		this.type = type;
		this.confirmationCode = confirmationCode;	
	}
	
	public int getConfirmationCode() {
		return this.confirmationCode;
	}
	public BillRequestType getType() {
		return this.type;
	}
	

}
