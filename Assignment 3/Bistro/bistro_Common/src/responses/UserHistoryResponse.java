package responses;

import java.time.LocalDate;
import java.time.LocalTime;

public class UserHistoryResponse {

	private LocalDate reservationDate;
	private LocalTime reservedForTime;
	private LocalTime checkInTime;
	private LocalTime CheckOutTime;
	private int tableNumber;
	private double totalPrice;
	public UserHistoryResponse(LocalDate reservationDate, LocalTime reservedForTime, LocalTime checkInTime,
			LocalTime checkOutTime, int tableNumber, double totalPrice) {
		this.reservationDate = reservationDate;
		this.reservedForTime = reservedForTime;
		this.checkInTime = checkInTime;
		CheckOutTime = checkOutTime;
		this.tableNumber = tableNumber;
		this.totalPrice = totalPrice;
	}
	public LocalDate getReservationDate() {
		return reservationDate;
	}
	public LocalTime getReservedForTime() {
		return reservedForTime;
	}
	public LocalTime getCheckInTime() {
		return checkInTime;
	}
	public LocalTime getCheckOutTime() {
		return CheckOutTime;
	}
	public int getTableNumber() {
		return tableNumber;
	}
	public double getTotalPrice() {
		return totalPrice;
	}
					
}
