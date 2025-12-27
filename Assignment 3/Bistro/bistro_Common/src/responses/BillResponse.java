package responses;

public class BillResponse {	
	private BillResponseType type;
	private double bill;
	private boolean notificationSent;
	public enum BillResponseType{
		ANSWER_TO_REQUEST_TO_SEE_BILL,
		ANSWER_TO_PAY_BILL
	}
	public BillResponse(BillResponseType type,double bill,boolean notificationSent) {
		this.type=type;
		this.bill = bill;
		this.notificationSent=notificationSent;
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
}
