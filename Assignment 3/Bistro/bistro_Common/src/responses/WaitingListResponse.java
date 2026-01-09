package responses;

public class WaitingListResponse {
	boolean hasBeenCancelled;
	public WaitingListResponse () {
	}
	public WaitingListResponse(boolean hasBeenCancelled) {
		this.hasBeenCancelled=hasBeenCancelled;
	}
	public boolean getHasBeenCancelled() {
		return this.hasBeenCancelled;
	}
}
