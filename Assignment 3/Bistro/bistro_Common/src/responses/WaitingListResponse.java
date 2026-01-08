package responses;

import java.time.LocalDateTime;

public class WaitingListResponse {
	
	private String priority;
	private LocalDateTime dateTime;
	public String contact;
	
	public WaitingListResponse () {}

	public WaitingListResponse(String priority, LocalDateTime dateTime, String contact) {
		this.priority = priority;
		this.dateTime = dateTime;
		this.contact = contact;
	}

	public String getPriority() {
		return priority;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public String getContact() {
		return contact;
	}
		
	
}
