package controllers;

import java.sql.Connection;
import java.sql.SQLException;

import database.DBManager;
import database.ReservationDAO;
import database.TableDAO;
import database.SeatingDAO;

public class BillingControl {
	private final ReservationDAO reservationDAO;
	private final TableDAO tableDAO;
	private final SeatingDAO seatingDAO;
	private final NotificationControl notificationControl;
	
	public BillingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO,NotificationControl notificationControl) {
		this.reservationDAO = reservationDAO;
		this.tableDAO = tableDAO;
		this.seatingDAO = seatingDAO;
		this.notificationControl=notificationControl;
	}
	public void sendBillForSeating(int seatingId) throws SQLException {
	    // 1) load seating -> reservationID -> reservation
		Connection conn = DBManager.getConnection();
		boolean isCheckedOut= seatingDAO.checkOutBySeatingId(conn, seatingId);
		
		
	    // 2) compute bill (billingDAO.createBillForSeating or calculate amount)
	    // 3) send via NotificationControl (user or guest)
	    // 4) mark billSent=1, billSentTime=NOW()
	    // NOTE: make it idempotent: if already billSent, do nothing
	  }
}
