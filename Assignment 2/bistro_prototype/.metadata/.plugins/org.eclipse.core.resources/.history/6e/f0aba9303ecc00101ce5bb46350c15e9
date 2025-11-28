package common;

import java.io.Serializable;
import java.time.LocalDate;

public class ReservationRequest implements Serializable{
	
	private String customerInfo;
	private LocalDate dateOfRequest;
	private int dinersCount;
	
	public ReservationRequest(String customerInfro, LocalDate dateOfRequest, int dinersCount) {
		this.customerInfo = customerInfro;
		this.dateOfRequest = dateOfRequest;
		this.dinersCount = dinersCount;
	}
	
	public String getCustomerInfo() {
		return this.customerInfo;
	}
	
	public LocalDate getDateofRequest() {
		return this.dateOfRequest;
	}
	
	public int getDinersCount() {
		return this.dinersCount;
	}
}
