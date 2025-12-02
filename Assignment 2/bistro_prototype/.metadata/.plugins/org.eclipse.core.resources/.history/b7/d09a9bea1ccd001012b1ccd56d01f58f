package requests;

import java.io.Serializable;
import java.time.LocalDate;

public class EditReservationRequest implements Serializable {

	private int confirmationCode;
	private LocalDate dateToChange;
	private int dinersToChange;
	private LocalDate dateWhenChanging;
	
	public EditReservationRequest(int confirmationCode,LocalDate datoToChange,int dinersToChange) {
		this.confirmationCode = confirmationCode;
		this.dateToChange = datoToChange;
		this.dinersToChange = dinersToChange;
		this.dateWhenChanging = LocalDate.now();
	}
	
	public int getConfirmationCode() {
		return this.confirmationCode;
	}
	
	public LocalDate getDateToChange() {
		return this.dateToChange;
	}
	
	public int getDinersToChange() {
		return this.dinersToChange;
	}
	
	public LocalDate getDateWhenChanging() {
		return this.dateWhenChanging;
	}
}
