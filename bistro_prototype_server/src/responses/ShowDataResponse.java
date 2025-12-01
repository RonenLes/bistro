package responses;

import java.util.List;

import entities.Reservation;

import java.io.Serializable;
import java.util.ArrayList;

public class ShowDataResponse implements Serializable{
	
	private boolean isSuccess;
	private List<Reservation> reservationList;
	private String msg;
	
	public ShowDataResponse(boolean isSuccess, List<Reservation> reservationList,String msg) {
		this.isSuccess = isSuccess;
		this.reservationList = reservationList;
		this.msg=msg;		
	}
	
	
	
	public boolean getIsSuccess() {
		return this.isSuccess;
	}
	
	public List<Reservation> getReservationList(){
		return this.reservationList;
	}
	
	public String toString() {
		return this.msg;
	}
	
	
}
