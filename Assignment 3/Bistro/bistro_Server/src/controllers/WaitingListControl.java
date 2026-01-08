package controllers;

import database.ReservationDAO;
import database.WaitingListDAO;

public class WaitingListControl {

	private final WaitingListDAO waitingListDAO;
	private final ReservationDAO reservationDAO;
	
	
	public WaitingListControl() {
		this(new WaitingListDAO(),new ReservationDAO());
	}
	
	public WaitingListControl(WaitingListDAO waitingListDAO, ReservationDAO reservationDAO) {
		this.waitingListDAO = waitingListDAO;
		this.reservationDAO = reservationDAO;
	}
	
	
	
		
}
