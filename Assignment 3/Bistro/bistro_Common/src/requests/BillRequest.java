package requests;

public class BillRequest {
	
	private BillRequestType type;
	private int confirmationCode;
	public enum BillRequestType{
		REQUEST_TO_SEE_BILL,
		PAY_BILL
	}
	public BillRequest(int confirmationCode,BillRequestType type) {
		this.confirmationCode = confirmationCode;
		this.type=type;
	}
	public int getConfirmationCode() {
		return this.confirmationCode;
	}
	public BillRequestType getType() {
		return this.type;
	}

}
