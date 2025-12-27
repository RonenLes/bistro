package server;

import controllers.BillingControl;
import database.DBManager;
import database.ReservationDAO;
import database.SeatingDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BillingScheduler {

    private final ScheduledExecutorService scheduler;
    private final SeatingDAO seatingDAO;
    private final BillingControl billingControl;
    private final ReservationDAO reservationDAO;

    private volatile boolean started = false;

    public BillingScheduler(SeatingDAO seatingDAO,
                            BillingControl billingControl,
                            ReservationDAO reservationDAO) {

        this.seatingDAO = seatingDAO;
        this.billingControl = billingControl;
        this.reservationDAO = reservationDAO;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BillingScheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() {
        if (started) return;
        started = true;

        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = DBManager.getConnection()) {
                conn.setAutoCommit(false);

                // 1) Bills
                List<Integer> dueSeatingIds = seatingDAO.getSeatingsDueForBill(conn);

                for (int seatingId : dueSeatingIds) {
                    try {
                        boolean ok = billingControl.sendBillAutomatically(conn, seatingId);

                        if (!ok) {
                            // Optional: log only (do not throw)
                            System.err.println("bill failed for seatingId=" + seatingId);
                        }

                    } catch (Exception ex) {
                        // Never stop the scheduler because of one seating
                        System.err.println("exception while billing seatingId=" + seatingId + ": " + ex.getMessage());

                        // Best effort: release claim so it can retry next tick
                        try {
                            seatingDAO.updateBillSent(conn, seatingId, 0); // release (2->0)
                        } catch (Exception ignore) {}
                    }
                }

                // 2) No-Shows
                try {
                    markNoShows(conn);
                } catch (Exception ex) {
                    // No-shows failure should also NOT stop the scheduler
                    System.err.println("[BillingScheduler] markNoShows failed: " + ex.getMessage());
                }

                conn.commit();

            } catch (Exception e) {
                // Connection-level failure (DB down, etc.) — still don't stop scheduler
                System.err.println("[BillingScheduler] tick failed: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * Tables seated for 2+ hours → send bill
     */
    private void mark2HoursSeating(Connection conn) throws SQLException {
        List<Integer> dueSeatingIds = seatingDAO.getSeatingsDueForBill(conn);
        for (int seatingId : dueSeatingIds) {
            billingControl.sendBillAutomatically(conn,seatingId);
        }
    }

    /**
     * Reservations late by 15 minutes and not SEATED → NO SHOW
     */
    private void markNoShows(Connection conn) throws SQLException {
        LocalDate today = LocalDate.now();
        LocalTime cutoff = LocalTime.now().minusMinutes(15);

        List<Integer> reservationIds =
                reservationDAO.getReservationsDueForNoShow(conn, today, cutoff);

        for (int reservationId : reservationIds) {
            reservationDAO.updateStatusByReservationID(conn, reservationId, "NO SHOW");
        }
    }

    public synchronized void stop() {
        scheduler.shutdownNow();
        started = false;
    }
}
