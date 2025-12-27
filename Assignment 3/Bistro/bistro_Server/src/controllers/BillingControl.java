package controllers;

import java.sql.Connection;
import java.sql.SQLException;

import database.DBManager;
import database.ReservationDAO;
import database.TableDAO;
import database.UserDAO;
import entities.Reservation;
import entities.User;
import database.SeatingDAO;

public class BillingControl {
	private final ReservationDAO reservationDAO;
	private final TableDAO tableDAO;
	private final SeatingDAO seatingDAO;
	private final NotificationControl notificationControl;
	private final UserDAO userDAO;
	
	public BillingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO,NotificationControl notificationControl,UserDAO userDAO) {
		this.reservationDAO = reservationDAO;
		this.tableDAO = tableDAO;
		this.seatingDAO = seatingDAO;
		this.notificationControl=notificationControl;
		this.userDAO=userDAO;
	}
	/**
     * Called by the Scheduler.
     * Creates its own Connection 
     *
     * @return TRUE only if bill was sent and marked as sent.
     *         FALSE otherwise (including if claim failed / not due / missing contact / notification failed).
     */
    public boolean sendBillAutomatically(Connection conn,int seatingId) {
        if (seatingId <= 0) return false;
        if (conn == null) return false; 
            try {
                // 1) claim (0->2). If false: already claimed/sent/not due/checked out
                boolean claimed = seatingDAO.claimAutoBillSend(conn, seatingId);
                if (!claimed) {
                    conn.rollback();
                    return false;
                }

                // 2) get reservationId from seatingId
                Integer reservationId = seatingDAO.getReservationIdBySeatingId(conn, seatingId);
                if (reservationId == null) {
                    seatingDAO.updateBillSent(conn, seatingId,0); 
                    conn.commit();
                    return false;
                }

                // 3) load reservation to decide where to send
                Reservation res = reservationDAO.getReservationByReservationID(conn, reservationId);
                if (res == null) {
                    seatingDAO.updateBillSent(conn, seatingId,0);
                    conn.commit();
                    return false;
                }

                String guestContact = res.getGuestContact();
                String userId = res.getUserID();

                // 4) build bill message (placeholder)
                String billMessage = buildBillMessage(seatingId, reservationId);
                
                // 5) send
                boolean sentOk = sendBillToCorrectContact(conn, guestContact, userId, billMessage);
                if (!sentOk) {
                    seatingDAO.updateBillSent(conn, seatingId,0); // 2->0 so it can retry
                    conn.commit();
                    return false;
                }

                // 6) mark as sent (2->1)
                boolean marked = seatingDAO.updateBillSent(conn, seatingId,1);
                if (!marked) {
                    // extremely rare, but be safe: release claim so it can retry
                    seatingDAO.updateBillSent(conn, seatingId,0);
                    conn.commit();
                    return false;
                }

                conn.commit();
                return true;

            } 

         catch (SQLException e) {
            return false;
        }
    }
    private String buildBillMessage(int seatingId, int reservationId) {
        return "Your bill is ready. Reservation #" + reservationId +
               ", Seating #" + seatingId + ". Thank you!";
    }
    private boolean sendBillToCorrectContact(Connection conn,String guestContact,String userId,String billMessage) throws SQLException {

    	if (guestContact != null && !guestContact.isBlank()) {
    		notificationControl.sendBillToGuest(guestContact, billMessage);
    		return true;
    	}
    	if (userId == null || userId.isBlank()) {
    		return false;
    	}
    	User user = userDAO.getUserByUserID(conn, userId);
    	if (user == null) {
    		return false;
    	}
    	notificationControl.sendBillToUser(user, billMessage);
    	return true;
    }
}
