package server;

import controllers.BillingControl;
import database.DBManager;
import database.SeatingDAO;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs every 30 seconds:
 * - fetch seatingIds that are due for billing
 * - call billingControl.sendBillForSeating(seatingId)
 *
 * Important:
 * - BillingControl.sendBillForSeating() should be idempotent (claim in DB)
 * - This scheduler should never sleep for 2 hours; it just polls every 30s.
 */
public class BillingScheduler {

    private final ScheduledExecutorService scheduler;
    private final SeatingDAO seatingDAO;
    private final BillingControl billingControl;

    private volatile boolean started = false;

    public BillingScheduler(SeatingDAO seatingDAO, BillingControl billingControl) {
        this.seatingDAO = seatingDAO;
        this.billingControl = billingControl;
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
            try(Connection conn=DBManager.getConnection()) {
            	
                List<Integer> dueSeatingIds = seatingDAO.getSeatingsDueForBill(conn);

                for (int seatingId : dueSeatingIds) {
                    // BillingControl opens its own Connection as you wanted.
                    billingControl.sendBillForSeating(seatingId);
                }

            } catch (Exception e) {
                System.err.println("[BillingScheduler] tick failed: " + e.getMessage());
            }
        }, 5, 30, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        scheduler.shutdownNow();
        started = false;
    }
}
