package entities;

import java.time.LocalTime;

public class Seating {

	
	private int SeatingID;
	private int reservationID;
	private int tableID;
	private LocalTime checkInTime;
	private LocalTime checkOutTime;
	private int billSent; //0-> bill hasn't been sent, 1->bill has been sent->,2->thread is working on current seating
	
	public Seating(int seatingID, int reservationID, int tableID, LocalTime checkInTime, LocalTime checkOutTime){
		SeatingID = seatingID;
		this.reservationID = reservationID;
		this.tableID = tableID;
		this.checkInTime = checkInTime;
		this.checkOutTime = checkOutTime;
		this.billSent=0;
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
	public int getBillSent() {
		return billSent;
	}
	
	
	
	
}
