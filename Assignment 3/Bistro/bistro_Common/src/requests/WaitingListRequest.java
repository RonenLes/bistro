package requests;

public class WaitingListRequest {

	private boolean wantToWait;

	public WaitingListRequest() {}
	
	public WaitingListRequest(boolean wantToWait) {
		this.wantToWait = wantToWait;
	}

	public boolean isWantToWait() {
		return wantToWait;
	}
	
	
}
