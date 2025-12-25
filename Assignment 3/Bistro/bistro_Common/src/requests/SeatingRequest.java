package requests;

public class SeatingRequest {
	
	private int confirmationCode;
	private ReservationRequest reservation;
	public SeatingRequest() {};
	
	public SeatingRequest(int confirmationCode,ReservationRequest reservation) {

		this.confirmationCode = confirmationCode;
		this.reservation=reservation;
		
	}
	
	public ReservationRequest getReservation() {
		return reservation;
	}

	public void setReservation(ReservationRequest reservation) {
		this.reservation = reservation;
	}

	public int getConfirmationCode() {
		
		return confirmationCode;
	}
	
	
}
