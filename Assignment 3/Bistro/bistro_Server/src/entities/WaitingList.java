package entities;

import java.time.LocalTime;

public class WaitingList {

	private int waitID;
	private int reservationID;
	private String status; //ENUM('WAITING', 'ASSIGNED', 'CANCELLED')
	private int priority; //1-high priority (reserved place) , 0-low priority (walk in)
	private LocalTime createdAt;
	private LocalTime assignedAt; //or cancelled at
	
	public WaitingList(int waitID, int reservationID, String status, int priority, LocalTime createdAt,
			LocalTime assignedAt) {
		this.waitID = waitID;
		this.reservationID = reservationID;
		this.status = status;
		this.priority = priority;
		this.createdAt = createdAt;
		this.assignedAt = assignedAt;
	}
	public int getWaitID() {
		return waitID;
	}
	public int getReservationID() {
		return reservationID;
	}
	public String getStatus() {
		return status;
	}
	public int getPriority() {
		return priority;
	}
	public LocalTime getCreatedAt() {
		return createdAt;
	}
	public LocalTime getAssignedAt() {
		return assignedAt;
	}
	
	
}
