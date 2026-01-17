package responses;

import java.time.LocalDateTime;

/**
 * Response payload for waiting list operations.
 *
 * <p>Main idea:
 * Used for waiting list display and cancellation flows. Can represent either:
 * <ul>
 *   <li>Cancellation result ({@code hasBeenCancelled})</li>
 *   <li>Waiting list entry details (priority, createdAt, contact)</li>
 * </ul>
 */
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
