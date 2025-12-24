package requests;

public class WaitingListRequest {

	private boolean wantToWait;
	private waitListCommand waitCommand;
	
	public WaitingListRequest() {}
	
	public enum waitListCommand{
		WANT_TO_WAIT,
		CANCEL_WAIT
	}
	
	public WaitingListRequest(boolean wantToWait,waitListCommand waitCommand) {
		this.wantToWait = wantToWait;
		this.waitCommand = waitCommand;
	}

	public boolean isWantToWait() {
		return wantToWait;
	}

	public waitListCommand getWaitCommand() {
		return waitCommand;
	}
	
	
}
