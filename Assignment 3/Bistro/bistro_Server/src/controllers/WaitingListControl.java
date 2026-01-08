package controllers;

import database.ReservationDAO;
import database.WaitingListDAO;

public class WaitingListControl {

	private final WaitingListDAO waitingListDAO;
	private final ReservationDAO reservationDAO;
	public WaitingListControl(WaitingListDAO waitingListDAO, ReservationDAO reservationDAO) {
		super();
		this.waitingListDAO = waitingListDAO;
		this.reservationDAO = reservationDAO;
	}
	
	public boolean callForNextCustomer() {
		
	}
	
		
}
