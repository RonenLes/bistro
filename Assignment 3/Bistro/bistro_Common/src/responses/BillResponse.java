package responses;

/**
 * Response payload for billing operations.
 *
 * <p>Main idea:
 * Sent back to the client after a billing request (view bill / pay bill). It may also indicate
 * whether a notification was sent and whether the next waiting customer was called.</p>
 *
 * <p>Main parts:
 * <ul>
 *   <li>{@link BillResponseType} - which billing response this represents</li>
 *   <li>{@code bill} - the bill amount</li>
 *   <li>{@code notificationSent} - whether a notification was sent to the customer</li>
 *   <li>{@code calledNextCustomer} - whether the system advanced the waiting list (optional)</li>
 * </ul>
 */
public class BillResponse {	
	private BillResponseType type;
	private double bill;
	private boolean notificationSent;
	private Boolean calledNextCustomer;
	public enum BillResponseType{
		ANSWER_TO_REQUEST_TO_SEE_BILL,
		ANSWER_TO_PAY_BILL
	}
	
	public BillResponse() {}
	public BillResponse(BillResponseType type,double bill,boolean notificationSent) {
		this.type=type;
		this.bill = bill;
		this.notificationSent=notificationSent;
	}
	public BillResponse(BillResponseType type,double bill,boolean notificationSent,boolean calledNextCustomer) {
		this.type=type;
		this.bill = bill;
		this.notificationSent=notificationSent;
		this.calledNextCustomer=calledNextCustomer;
	}
	public double getBill() {
		return this.bill;
	}
	public BillResponseType getType() {
		return this.type;
	}
	public boolean getNotificationSent() {
		return this.notificationSent;
	}
	public boolean getCalledNextCustomer(){
		return this.calledNextCustomer;
	}
}