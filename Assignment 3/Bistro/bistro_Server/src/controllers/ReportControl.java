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
