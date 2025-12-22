package entities;

import java.time.LocalTime;

public class Seating {

	
	private int SeatingID;
	private int reservationID;
	private int tableID;
	private LocalTime checkInTime;
	private LocalTime checkOutTime;
	
	public Seating(int seatingID, int reservationID, int tableID, LocalTime checkInTime, LocalTime checkOutTime){
			
		super();
		SeatingID = seatingID;
		this.reservationID = reservationID;
		this.tableID = tableID;
		this.checkInTime = checkInTime;
		this.checkOutTime = checkOutTime;
		
	}
	public int getSeatingID() {
		return SeatingID;
	}
	public int getReservationID() {
		return reservationID;
	}
	public int getTableID() {
		return tableID;
	}
	public LocalTime getCheckInTime() {
		return checkInTime;
	}
	public LocalTime getCheckOutTime() {
		return checkOutTime;
	}
	
	
	
}
