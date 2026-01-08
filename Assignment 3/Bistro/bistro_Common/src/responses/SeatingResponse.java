package responses;

import java.time.LocalTime;

public class SeatingResponse {
	public enum SeatingResponseType{
		CUSTOMER_CHECKED_IN,
		CUSTOMER_IN_WAITINGLIST
	}
	private SeatingResponseType type;
	private Integer tableNumberl;
	private Integer tableCapacity;
	private LocalTime checkInTime;
	
	public SeatingResponse() {}
	
	public SeatingResponse(Integer tableNumberl, Integer tableCapacity, LocalTime checkInTime,SeatingResponseType type) {
		this.tableNumberl = tableNumberl;
		this.tableCapacity = tableCapacity;
		this.checkInTime = checkInTime;
		this.type=type;
	}
	public int getTableNumberl() {
		return tableNumberl;
	}
	public void setTableNumberl(Integer tableNumberl) {
		this.tableNumberl = tableNumberl;
	}
	public int getTableCapacity() {
		return tableCapacity;
	}
	public void setTableCapacity(Integer tableCapacity) {
		this.tableCapacity = tableCapacity;
	}
	public LocalTime getCheckInTime() {
		return checkInTime;
	}
	public void setCheckInTime(LocalTime checkInTime) {
		this.checkInTime = checkInTime;
	}
	public SeatingResponseType getType() {
		return this.type;
	}
	
}
