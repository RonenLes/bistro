package requests;

public class BillRequest {
	
	private BillRequestType type;
	private int confirmationCode;
	private boolean isCashPayment;
	
	public enum BillRequestType{
		REQUEST_TO_SEE_BILL,
		PAY_BILL
	}
	
	public BillRequest() {}
	
	public BillRequest(int confirmationCode,BillRequestType type) {
		this.confirmationCode = confirmationCode;
		this.type=type;
	}
	
	public BillRequest(BillRequestType type, int confirmationCode, boolean isCashPayment) {
		super();
		this.type = type;
		this.confirmationCode = confirmationCode;
		this.isCashPayment = isCashPayment;
	}
	
	public int getConfirmationCode() {
		return this.confirmationCode;
	}
	public BillRequestType getType() {
		return this.type;
	}
	public boolean getIsCashPayment() {
		return this.isCashPayment;
	}

}
