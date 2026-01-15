package server;

import controllers.BillingControl;
import controllers.NotificationControl;
import controllers.ReportControl;
import controllers.WaitingListControl;
import database.DBManager;
import database.OpeningHoursDAO;
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

/**
 * 
 */
/**
 * 
 */
public class BillingScheduler {

    private final ScheduledExecutorService reminderScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t1 = new Thread(r, "ReminderScheduler");
        t1.setDaemon(true);
        return t1;
    });

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t2 = new Thread(r, "BillingScheduler");
        t2.setDaemon(true);
        return t2;
    });

    
    private final ScheduledExecutorService monthlyReportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t3 = new Thread(r, "MonthlyReportScheduler");
        t3.setDaemon(true);
        return t3;
    });
    private final ScheduledExecutorService waitingListScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "waitingListScheduler");
        t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService openingHoursScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t4 = new Thread(r, "openingHoursScheduler");
        t4.setDaemon(true);
        return t4;
    });
    private final SeatingDAO seatingDAO;
    private final BillingControl billingControl;
    private final ReservationDAO reservationDAO;
    private final UserDAO userDAO;
    private final NotificationControl notificationControl;
    private final WaitingListControl waitingListControl;
    private final OpeningHoursDAO openingHoursDAO;
    
    private final ReportControl reportControl;

    private volatile boolean started = false;

    public BillingScheduler(SeatingDAO seatingDAO,BillingControl billingControl,ReservationDAO reservationDAO,UserDAO userDAO,
    						NotificationControl notificationControl,
                            ReportControl reportControl,WaitingListControl waitingListControl,OpeningHoursDAO openingHoursDAO) {

        this.seatingDAO = seatingDAO;
        this.billingControl = billingControl;
        this.reservationDAO = reservationDAO;
        this.userDAO = userDAO;
        this.notificationControl = notificationControl;
        this.waitingListControl=waitingListControl;
        this.reportControl = reportControl;
        this.openingHoursDAO=openingHoursDAO;
    }

    /**
     * function that runs on server start, activates all schedulers.
     */
    public synchronized void start() {
        if (started) return;
        started = true;

        
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBManager.getConnection()) {
                if (conn == null) {
                    System.err.println("tick failed: conn is null(1)");
                    return;
                }
                conn.setAutoCommit(false);

                try {
                    mark2HoursSeating(conn);
                    markNoShows(conn);
                    conn.commit();
                } catch (Exception ex) {
                    try { conn.rollback(); } catch (Exception ignore) {}
                    System.err.println("tick failed (rolled back(2)): " + ex.getMessage());
                }

            } catch (Exception e) {
                System.err.println("tick failed: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
        
        reminderScheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBManager.getConnection()) {
                if (conn == null) {
                    System.err.println("reminder tick failed: conn is null(3)");
                    return;
                }

                conn.setAutoCommit(false);

                try {
                    findPrior2HourReservation(conn);
                    conn.commit();
                } catch (Exception ex) {
                    try { conn.rollback(); } catch (Exception ignore) {}
                    System.err.println("findPrior2HourReservation failed(4): " + ex.getMessage());
                }

            } catch (Exception e) {
                System.err.println("reminder tick failed(5): " + e.getMessage());
            }
        }, 0, 30, TimeUnit.MINUTES);
        
        monthlyReportScheduler.scheduleAtFixedRate(() -> {
            try {
                if (reportControl == null) {
                    System.err.println("Monthly report skipped: reportControl is null(6)");
                    return;
                }

                

                boolean ok = reportControl.createMonthlyVisitorReportIfMissing();
                if (!ok) {
                    System.err.println("Monthly visitor report creation failed");
                } else {
                    System.out.println("Monthly visitor report created/exists (for previous month)");
                }
                ok=reportControl.createMonthlyReservationWaitingListReportIfMissing();
                if (!ok) {
                    System.err.println("Monthly reservation report creation failed");
                } else {
                    System.out.println("Monthly visitor report created/exists (for previous month)");
                }

            } catch (Exception e) {
                System.err.println("Monthly report tick failed: " + e.getMessage());
            }
        }, 0, 24, TimeUnit.HOURS);
        
        waitingListScheduler.scheduleAtFixedRate(() -> {
            try {
                waitingListControl.cancelLateArrivalsFromWaitingListToTable();
            } catch (Exception e) {
                System.out.println("waitngListScheduler failed " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
        
        openingHoursScheduler.scheduleAtFixedRate(() -> {
            Connection conn = null;
            try {
                conn = DBManager.getConnection();
                if (conn == null) {
                    System.err.println("openingHoursScheduler tick failed: conn is null");
                    return;
                }

                conn.setAutoCommit(false);

                ensureOpeningHoursNext30Days(conn);

                conn.commit();
            } catch (Exception e) {
                System.out.println("openingHoursScheduler failed: " + e.getMessage());
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ignore) {}
                }
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException ignore) {}
                }
            }
        }, 0, 24, TimeUnit.HOURS);
    }
    
    /**
     * checks for the next 30 days, that we have the opening hours of the restaurant for each day.
     * @param conn
     * @throws SQLException
     */
    public void ensureOpeningHoursNext30Days(Connection conn) throws SQLException {
        LocalDate startDate = LocalDate.now();
        LocalDate endExclusive = startDate.plusDays(30); 
        LocalTime defaultOpen = LocalTime.of(10, 0);
        LocalTime defaultClose = LocalTime.of(23, 0);
        for (LocalDate d = startDate; d.isBefore(endExclusive); d = d.plusDays(1)) {
            
            if (openingHoursDAO.getOpeningHour(conn, d) != null) {
                continue;
            }
            boolean inserted = openingHoursDAO.insertNewOpeningHour(
                    conn,
                    d,
                    dayNameEnglish(d),  
                    defaultOpen,
                    defaultClose
            );

            if (!inserted) {
                throw new SQLException("Failed to insert opening_hours for missing date: " + d);
            }
        }
    }
    
    /**
     * helper method for ensureOpeningHoursNext30Days.
     * @param d- the current date
     * @return the day of the week for the current date as a String
     */
    private String dayNameEnglish(LocalDate d) {
        switch (d.getDayOfWeek()) {
            case SUNDAY: return "Sunday";
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            case SATURDAY: return "Saturday";
            default: throw new IllegalStateException("Unexpected day: " + d.getDayOfWeek());
        }
    }
    
    /**
     * reminder to all the customers whose reservation is in two hours.
     * @param conn
     * @throws SQLException
     */
    private void findPrior2HourReservation(Connection conn) throws SQLException {
        List<Reservation> reservations = reservationDAO.getReservationsDueForReminder(conn);
        for (Reservation r : reservations) {
            String guestContact = r.getGuestContact();
            if (guestContact == null || guestContact.isBlank()) {
                String userID = r.getUserID();
                if (userID == null || userID.isBlank()) {
                    System.out.println("Reminder skipped: missing userID (reservationID=" + r.getReservationID() + ")");
                    continue;
                }
                User user = userDAO.getUserByUserID(conn, userID);
                if (user == null) {
                    System.out.println("Reminder skipped: user not found for userID=" + userID);
                    continue;
                }
                String email = user.getEmail();
                String phoneNumber = user.getPhone();

                if (email != null && !email.isBlank()) {
                    if (!notificationControl.sendAutomaticEmailTwoHourPrior(email)) {
                        System.out.println("failed to send email to " + userID);
                    }
                }
                if (phoneNumber != null && !phoneNumber.isBlank()) {
                    if (!notificationControl.sendAutomaticSMSTwoHourPrior(phoneNumber)) {
                        System.out.println("Failed to send SMS to " + userID + " (" + phoneNumber + ")");
                    }
                }
            } else {
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
     * sending the bill to all the customers who are seating at a table for 2 hours without requesting a bill.
     * @param conn
     * @throws SQLException
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
     * chaning the reservation status to NO_SHOW and informing customer about the cancelation of his reservation due to him being late
     * @param conn
     * @throws SQLException
     */
    private void markNoShows(Connection conn) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalTime cutoff = LocalTime.now().minusMinutes(15);

        List<Integer> reservationIds = reservationDAO.getReservationsDueForNoShow(conn, today, cutoff);

        for (int reservationId : reservationIds) {
        	Reservation reservation=reservationDAO.getReservationByReservationID(conn, reservationId);
        	int confirmationCode=reservation.getConfirmationCode();
            reservationDAO.updateStatusByReservationID(conn, reservationId, "NO_SHOW");
            if(reservation.getGuestContact()==null||reservation.getGuestContact().isBlank()) {
            	notificationControl.sendCancelledReservation(reservation.getUserID(), "Your reservation with confirmation code "+confirmationCode+" has been canceled");
            }
            else {
            	notificationControl.sendCancelledReservation(reservation.getGuestContact(), "Your reservation with confirmation code "+confirmationCode+" has been canceled");
            }
        }
    }
    
    /**
     * stopping all the schedulers
     */
    public synchronized void stop() {
        scheduler.shutdownNow();
        reminderScheduler.shutdownNow();
        monthlyReportScheduler.shutdownNow(); 
        waitingListScheduler.shutdownNow();
        openingHoursScheduler.shutdownNow();
        started = false;
    }
}
