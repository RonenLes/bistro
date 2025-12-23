package responses;

import java.time.LocalTime;

public class SeatingResponse {
	
	private int tableNumberl;
	private int tableCapacity;
	private LocalTime checkInTime;
	
	public SeatingResponse() {}
	
	public SeatingResponse(int tableNumberl, int tableCapacity, LocalTime checkInTime) {
		this.tableNumberl = tableNumberl;
		this.tableCapacity = tableCapacity;
		this.checkInTime = checkInTime;
	}
	public int getTableNumberl() {
		return tableNumberl;
	}
	public void setTableNumberl(int tableNumberl) {
		this.tableNumberl = tableNumberl;
	}
	public int getTableCapacity() {
		return tableCapacity;
	}
	public void setTableCapacity(int tableCapacity) {
		this.tableCapacity = tableCapacity;
	}
	public LocalTime getCheckInTime() {
		return checkInTime;
	}
	public void setCheckInTime(LocalTime checkInTime) {
		this.checkInTime = checkInTime;
	}
	
}
