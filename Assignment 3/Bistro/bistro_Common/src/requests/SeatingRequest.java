package requests;


/**
 * Request payload for seating-related operations.
 *
 * <p>Main idea:
 * Supports seating actions either by a reservation confirmation code or by providing a full
 * {@link ReservationRequest} object (depending on {@link SeatingRequestType}).</p>
 *
 * <p>Main parts:
 * <ul>
 *   <li>{@link SeatingRequestType} - indicates how the seating operation should be resolved</li>
 *   <li>{@code confirmationCode} - used when seating is requested by confirmation code</li>
 *   <li>{@code reservation} - used when seating is requested by reservation details</li>
 * </ul>
 */
public class SeatingRequest {
	public enum SeatingRequestType{
		BY_CONFIRMATIONCODE,
		BY_RESERVATION,
	}
	private SeatingRequestType type;
	private int confirmationCode;
	private ReservationRequest reservation;
	
	public SeatingRequest() {};
	
	public SeatingRequest(SeatingRequestType type,int confirmationCode,ReservationRequest reservation) {

		this.confirmationCode = confirmationCode;
		this.reservation=reservation;
		this.type=type;
		
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
	public SeatingRequestType getType() {
		return this.type;
	}
	
	
}
