package responses;

import java.time.LocalDateTime;

public class WaitingListResponse {
	boolean hasBeenCancelled;
	private String priority;
	private LocalDateTime createdAt;
	private String contact;
	
			
	public WaitingListResponse(String priority, LocalDateTime createdAt, String contact) {
		this.priority = priority;
		this.createdAt = createdAt;
		this.contact = contact;
	}
	
	
	public String getPriority() {
		return priority;
	}


	public LocalDateTime getCreatedAt() {
		return createdAt;
	}


	public String getContact() {
		return contact;
	}


	public WaitingListResponse () {
	}
	public WaitingListResponse(boolean hasBeenCancelled) {
		this.hasBeenCancelled=hasBeenCancelled;
	}
	public boolean getHasBeenCancelled() {
		return this.hasBeenCancelled;
	}
}
