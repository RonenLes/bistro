package server;

import controllers.BillingControl;
import controllers.NotificationControl;
import database.DBManager;
import database.ReservationDAO;
import database.SeatingDAO;
import database.UserDAO;
import entities.Reservation;
import entities.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BillingScheduler {

	private final ScheduledExecutorService reminderScheduler =Executors.newSingleThreadScheduledExecutor(r -> {
	            Thread t1 = new Thread(r, "ReminderScheduler");
	            t1.setDaemon(true);
	            return t1;
	        });
    private final ScheduledExecutorService scheduler= Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t2 = new Thread(r, "BillingScheduler");
        t2.setDaemon(true);
        return t2;
    });
    private final SeatingDAO seatingDAO;
    private final BillingControl billingControl;
    private final ReservationDAO reservationDAO;
    private final UserDAO userDAO;
    private final NotificationControl notificationControl;
    private volatile boolean started = false;
    
    public BillingScheduler(SeatingDAO seatingDAO,BillingControl billingControl,ReservationDAO reservationDAO,UserDAO userDAO,NotificationControl notificationControl) {

        this.seatingDAO = seatingDAO;
        this.billingControl = billingControl;
        this.reservationDAO = reservationDAO;
        this.userDAO=userDAO;
        this.notificationControl=notificationControl;
        
    }
    
    public synchronized void start() {
        if (started) return;
        started = true;
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBManager.getConnection()) {
                if (conn == null) {
                    System.err.println("tick failed: conn is null");
                    return;
                }
                conn.setAutoCommit(false);
                try {
                    mark2HoursSeating(conn);
                } catch (Exception ex) {
                    System.err.println("mark2HoursSeating failed: " + ex.getMessage());
                }

                try {
                    markNoShows(conn);
                } catch (Exception ex) {
                    System.err.println("markNoShows failed: " + ex.getMessage());
                }

                conn.commit();

            } catch (Exception e) {
                System.err.println("tick failed: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
        reminderScheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBManager.getConnection()) {
                if (conn == null) {
                    System.err.println("reminder tick failed: conn is null");
                    return;
                }

                conn.setAutoCommit(false);

                try {
                    findPrior2HourReservation(conn);
                    conn.commit();
                } catch (Exception ex) {
                    try { conn.rollback(); } catch (Exception ignore) {}
                    System.err.println("findPrior2HourReservation failed: " + ex.getMessage());
                }

            } catch (Exception e) {
                System.err.println("reminder tick failed: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.MINUTES);
    }
    private void findPrior2HourReservation(Connection conn) throws SQLException {
    	List<Reservation> reservations=reservationDAO.getReservationsDueForReminder(conn);
    	for(Reservation r : reservations) {
    		String guestContact=r.getGuestContact();
    		if(guestContact==null ||guestContact.isBlank()) {
    			String userID=r.getUserID();
    			if (userID == null || userID.isBlank()) {
                    System.out.println("Reminder skipped: missing userID (reservationID=" + r.getReservationID() + ")");
                    continue;
                }
    			User user=userDAO.getUserByUserID(conn, userID);
    			if (user == null) {
                    System.out.println("Reminder skipped: user not found for userID=" + userID);
                    continue;
                }
    			String email=user.getEmail();
    			String phoneNumber=user.getPhone();
    			if (email != null && !email.isBlank()) {
    				if(!notificationControl.sendAutomaticEmailTwoHourPrior(email)) {
        				System.out.println("failed to send email to " +userID);
        			}
    			}
    			if (phoneNumber != null && !phoneNumber.isBlank()) {
                    if (!notificationControl.sendAutomaticSMSTwoHourPrior(phoneNumber)) {
                        System.out.println("Failed to send SMS to " + userID + " (" + phoneNumber + ")");
                    }
                }
    		}
    		else {

                String c = guestContact.trim();
                boolean isEmail = c.contains("@") && c.contains(".");
                if (isEmail) {
                    if (!notificationControl.sendAutomaticEmailTwoHourPrior(c)) {
                        System.out.println("Failed to send EMAIL to guestContact=" + c);
                    }
                } else {
                    if (!notificationControl.sendAutomaticSMSTwoHourPrior(c)) {
                        System.out.println("Failed to send SMS to guestContact=" + c);
                    }
                }
            }
    	}
    }

    /**
     * Tables seated for 2+ hours → send bill
     */
    private void mark2HoursSeating(Connection conn) throws SQLException {
        List<Integer> dueSeatingIds = seatingDAO.getSeatingsDueForBill(conn);
        for (int seatingId : dueSeatingIds) {
            try {
                boolean ok = billingControl.sendBillAutomatically(conn, seatingId);
                if (!ok) {
                    System.err.println("bill failed for seatingId=" + seatingId);
                }
            } catch (Exception ex) {
                System.err.println("exception while billing seatingId=" + seatingId + ": " + ex.getMessage());
                try {
                    seatingDAO.updateBillSent(conn, seatingId, 0);
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Reservations late by 15 minutes and not SEATED → NO SHOW
     */
    private void markNoShows(Connection conn) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalTime cutoff = LocalTime.now().minusMinutes(15);

        List<Integer> reservationIds =reservationDAO.getReservationsDueForNoShow(conn, today, cutoff);

        for (int reservationId : reservationIds) {
            reservationDAO.updateStatusByReservationID(conn, reservationId, "NO SHOW");
        }
    }
    
    public synchronized void stop() {
        scheduler.shutdownNow();
        reminderScheduler.shutdownNow();
        started = false;
    }
}
