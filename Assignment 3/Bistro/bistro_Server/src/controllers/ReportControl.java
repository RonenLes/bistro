package controllers;

import database.DBManager;
import database.ReportDAO;
import database.ReservationDAO;
import database.SeatingDAO;
import database.WaitingListDAO;
import kryo.KryoUtil;
import requests.ReportRequest;
import responses.ReportResponse;
import responses.Response;

import java.sql.Connection;
import java.time.YearMonth;
import java.util.List;
import java.time.LocalDateTime;
/**
 * Controls monthly reporting logic.
 *
 * This controller handles:
 * 
 *   Fetching a monthly report (visitor report + subscriber report) from the DB
 *   Creating the monthly visitor report for the previous month if missing/invalid
 *   Creating the monthly reservation/waiting-list report for the previous month if missing/invalid
 * 
 *
 * Reports are stored in the DB as serialized byte arrays (via {@link KryoUtil}).
 *
 * Stored report formats
 * 
 *   VISITOR_REPORT payload: {@code List<LocalDateTime[]>}
 *     where each element is an array with at least 2 items (typically: entry & exit times)
 *   SUBSCRIBER_REPORT payload: {@code Integer[][]} of size {@code [31][2]}:
 *     
 *      {@code payload[dayIndex][0]} = reservations count for that day
 *       {@code payload[dayIndex][1]} = waiting list count for that day
 *    
 *     Only indices {@code 0..daysInMonth-1} are filled for the actual month.
 *   
 * 
 *
 * The {@code monthKey} used for storing/fetching is {@code YearMonth.toString()},
 * usually formatted like {@code "2026-01"}.
 */
public class ReportControl {
	
    private static final String VISITOR_REPORT_TYPE = "VISITOR_REPORT";
    private static final String SUBSCRIBER_REPORT_TYPE = "SUBSCRIBER_REPORT"; 
    
    private final ReportDAO reportDAO;
    private final SeatingDAO seatingDAO;
    private final ReservationDAO reservationDAO;
    private final WaitingListDAO waitingListDAO;

    public ReportControl() {
        this(new ReportDAO(), new SeatingDAO(), new ReservationDAO(), new WaitingListDAO());
    }

    public ReportControl(ReportDAO reportDAO, SeatingDAO seatingDAO,ReservationDAO reservationDAO,WaitingListDAO waitingListDAO) {
        this.reportDAO = reportDAO;
        this.seatingDAO = seatingDAO;
        this.reservationDAO = reservationDAO;
        this.waitingListDAO = waitingListDAO;
    }

    /**
     * Handles a report request and returns a combined {@link ReportResponse}.
     *
     * <p>If {@code request} is null or {@code request.getMonth()} is null,
     * the target month defaults to the previous month.</p>
     *
     * <p>This method loads two reports from the DB:
     * <ul>
     *   <li>Visitor report: a List of LocalDateTime arrays (each entry contains check-in/check-out times)</li>
     *   <li>Subscriber report: a 2D Integer array containing daily counts</li>
     * </ul>
     *
     * <p>DB access is performed inside a single transaction (auto-commit disabled).</p>
     *
     * @param request report request containing an optional target month
     * @return Response with a ReportResponse payload on success, or Response with an error message on failure
     */
    public Response<ReportResponse> handleReportRequest(ReportRequest request) {
    	
        YearMonth target = (request != null && request.getMonth() != null)
                ? request.getMonth()
                : YearMonth.now().minusMonths(1);

        String monthKey = target.toString();

        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) {
                return new Response<>(false, "Couldnt connect to db", null);
            }

            
            conn.setAutoCommit(false);

            try {
                byte[] visitorBytes = reportDAO.getReportBytes(conn, VISITOR_REPORT_TYPE, monthKey);
                if (visitorBytes == null) {
                    conn.rollback();
                    return new Response<>(false, "Visitor report not found for " + monthKey, null);
                }
                
                Object visitorObj = KryoUtil.deserialize(visitorBytes);
                if (!(visitorObj instanceof List)) {
                    conn.rollback();
                    return new Response<>(false, "Bad visitor report payload for " + monthKey, null);
                }

                @SuppressWarnings("unchecked")
                List<LocalDateTime[]> visits = (List<LocalDateTime[]>) visitorObj;

                
                byte[] resWaitBytes = reportDAO.getReportBytes(conn, SUBSCRIBER_REPORT_TYPE, monthKey);
                if (resWaitBytes == null) {
                    conn.rollback();
                    return new Response<>(false, "Reservation/WaitingList report not found for " + monthKey, null);
                }

                Object resWaitObj = KryoUtil.deserialize(resWaitBytes);
                if (!(resWaitObj instanceof Integer[][])) {
                    conn.rollback();
                    return new Response<>(false, "Bad reservation/waitinglist payload for " + monthKey, null);
                }
                
                Integer[][] dailyCounts = (Integer[][]) resWaitObj;
                
                conn.commit();
                ReportResponse payload = new ReportResponse(monthKey,visits,dailyCounts);
                return new Response<>(true, "", payload);

            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignore) {}
                return new Response<>(false, "Report failed: " + e.getMessage(), null);
            }

        } catch (Exception e) {
            return new Response<>(false, "Report failed: " + e.getMessage(), null);
        }
    }

    
    /**
     * Creates the monthly visitor report for the <b>previous month</b> if it is missing or invalid.
     *
     * <p>Steps:
     * <ol>
     *   <li>Check if report exists</li>
     *   <li>If it exists, validate it by deserializing and checking its structure</li>
     *   <li>If missing/invalid, query visits from {@link SeatingDAO} and upsert into DB</li>
     * </ol>
     *
     * @return {@code true} if a valid report already existed or was successfully created;
     *         {@code false} if DB access failed or saving failed
     */
    public boolean createMonthlyVisitorReportIfMissing() {
        YearMonth target = YearMonth.now().minusMonths(1);
        String monthKey = target.toString();

        LocalDateTime start = target.atDay(1).atStartOfDay();
        LocalDateTime end = target.plusMonths(1).atDay(1).atStartOfDay();

        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (reportDAO.reportExists(conn, VISITOR_REPORT_TYPE, monthKey)) {
                    byte[] existing = reportDAO.getReportBytes(conn, VISITOR_REPORT_TYPE, monthKey);
                    boolean valid = false;
                    if (existing != null) {
                        try {
                            Object obj = KryoUtil.deserialize(existing);
                            valid = isValidVisitsPayload(obj);
                        } catch (Exception ignore) {
                            valid = false;
                        }
                    }
                    if (valid) {
                        conn.rollback();
                        return true;
                    }
                }
                List<LocalDateTime[]> visits = seatingDAO.getVisitTimesBetween(conn, start, end);
                byte[] payload = KryoUtil.serialize(visits);

                boolean saved = reportDAO.upsertReportBytes(conn, VISITOR_REPORT_TYPE, monthKey, payload);
                if (!saved) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignore) {}
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates the monthly reservation/waiting-list report for the <b>previous month</b>
     * if it is missing or invalid.
     *
     * <p>The stored payload is {@code Integer[31][2]}:
     * <ul>
     *   <li>{@code [i][0]} = reservations count for day {@code i+1}</li>
     *   <li>{@code [i][1]} = waiting-list count for day {@code i+1}</li>
     * </ul>
     *
     * <p>Only the first {@code daysInMonth} rows are filled.
     *
     * @return {@code true} if a valid report already existed or was successfully created;
     *         {@code false} if DB access failed or saving failed
     */
    public boolean createMonthlyReservationWaitingListReportIfMissing() {
        YearMonth target = YearMonth.now().minusMonths(1);
        String monthKey = target.toString();
        int daysInMonth = target.lengthOfMonth();

        LocalDateTime start = target.atDay(1).atStartOfDay();
        LocalDateTime end = target.plusMonths(1).atDay(1).atStartOfDay();

        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try {
                if (reportDAO.reportExists(conn, SUBSCRIBER_REPORT_TYPE, monthKey)) {
                    byte[] existing = reportDAO.getReportBytes(conn, SUBSCRIBER_REPORT_TYPE, monthKey);
                    boolean valid = false;
                    if (existing != null) {
                        try {
                            Object obj = KryoUtil.deserialize(existing);
                            valid = (obj instanceof Integer[][]);
                        } catch (Exception ignore) {
                            valid = false;
                        }
                    }
                    if (valid) {
                        conn.rollback();
                        return true;
                    }
                }

                Integer[] monthlyReservationsCount = reservationDAO.getCountOfReservationsBetween(conn, start, end);
                Integer[] monthlyWaitingListCount = waitingListDAO.getCountOfWaitingListBetween(conn, start, end);

                Integer[][] payloadBeforeByte = new Integer[31][2];
                for (int i = 0; i < daysInMonth; i++) {
                    payloadBeforeByte[i][0] = monthlyReservationsCount[i];
                    payloadBeforeByte[i][1] = monthlyWaitingListCount[i];
                }

                byte[] payload = KryoUtil.serialize(payloadBeforeByte);
                boolean saved = reportDAO.upsertReportBytes(conn, SUBSCRIBER_REPORT_TYPE, monthKey, payload);
                if (!saved) {
                    conn.rollback();
                    return false;
                }

                conn.commit();
                return true;

            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignore) {}
                return false;
            }

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates that the deserialized visitor report payload has the expected structure:
     * a list where each element is a {@code LocalDateTime[]} with length at least 2.
     *
     * @param obj deserialized object from stored bytes
     * @return {@code true} if payload structure is valid; otherwise {@code false}
     */
    private boolean isValidVisitsPayload(Object obj) {
        if (!(obj instanceof java.util.List<?> list)) return false;
        for (Object el : list) {
            if (el == null) continue;
            if (!(el instanceof LocalDateTime[] arr)) return false;
            if (arr.length < 2) return false;
        }
        return true;
    }

}
