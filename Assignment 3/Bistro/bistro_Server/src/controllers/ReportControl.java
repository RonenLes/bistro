package controllers;

import database.DBManager;
import database.ReportDAO;
import database.SeatingDAO;
import database.ReservationDAO;
import database.WaitingListDAO;
import kryo.KryoUtil;
import requests.ReportRequest;
import responses.ReportResponse;
import responses.Response;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public class ReportControl {

    private final SeatingDAO seatingDAO;
    private final ReportDAO reportDAO;
    private final ReservationDAO reservationDAO;
    private final WaitingListDAO waitingListDAO;
    private static final String VISITOR_REPORT_TYPE = "VISITOR_REPORT";
    private static final String SUBSCRIBER_REPORT_TYPE = "SUBSCRIBER_REPORT";
    

    public ReportControl() {
    	this(new SeatingDAO(),new ReportDAO(),new ReservationDAO(),new WaitingListDAO());
    }
    
    public ReportControl(SeatingDAO seatingDAO, ReportDAO reportDAO,ReservationDAO reservationDAO,WaitingListDAO waitingListDAO) {
        this.seatingDAO = seatingDAO;
        this.reportDAO = reportDAO;
        this.reservationDAO=reservationDAO;
        this.waitingListDAO=waitingListDAO;
    }
    public Response<ReportResponse> handleReportRequest(ReportRequest req) {

        if (req == null || req.getType() == null) {return new Response<>(false,"Invalid report request",null);
        }
        String reportMonth = java.time.YearMonth.now().minusMonths(1).toString();
        String reportType;
        ReportResponse.ReportCommand responseType;
        switch (req.getType()) {
            case VISITOR_REPORT:
                reportType = VISITOR_REPORT_TYPE;
                responseType = ReportResponse.ReportCommand.VISITOR_REPORT_RES;
                break;

            case SUBSRIBER_REPORT:
                reportType = SUBSCRIBER_REPORT_TYPE;
                responseType = ReportResponse.ReportCommand.SUBSRIBER_REPORT_RES;
                break;

            default:
                return new Response<>(false,"Unsupported report type",null);
                                                                       
                
        }

        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) {return new Response<>(false,"DB connection failed",null);
            }
            byte[] payload = reportDAO.getReportBytes(conn, reportType, reportMonth);

            if (payload == null) return new Response<>(false,"Report not found for " + reportMonth,null);
            
            ReportResponse resp =new  ReportResponse(responseType,payload);
            return new Response<>(true,"Report loaded successfully",resp);

        } catch (Exception e) {
            return new Response<>(false,"Failed to load report: " + e.getMessage(),null);                                                         
        }
    }


    
    /**
     * Creates the report for the previous calendar month and stores it in DB.
     * Data format stored: Kryo bytes of List<LocalDateTime[]> where:
     * [0] = checkIn, [1] = checkOut
     */
    public boolean createMonthlyVisitorReportIfMissing() {

        YearMonth target = YearMonth.now().minusMonths(1);
        String monthKey = target.toString(); // "YYYY-MM"

        LocalDateTime start = target.atDay(1).atStartOfDay();
        LocalDateTime end = target.plusMonths(1).atDay(1).atStartOfDay();

        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try { 
                if (reportDAO.reportExists(conn, VISITOR_REPORT_TYPE, monthKey)) {
                    conn.rollback();  
                    return true;
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
        int daysInMonth=target.lengthOfMonth();

        LocalDateTime start = target.atDay(1).atStartOfDay();
        LocalDateTime end = target.plusMonths(1).atDay(1).atStartOfDay();
        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return false;
            conn.setAutoCommit(false);
            try { 
                if (reportDAO.reportExists(conn, SUBSCRIBER_REPORT_TYPE,target.toString())) {
                    conn.rollback();  
                    return true;
                }
                Integer [] monthlyReservationsCount=reservationDAO.getCountOfReservationsBetween(conn,start,end);
                Integer [] monthlyWaitingListCount=waitingListDAO.getCountOfWaitingListBetween(conn,start,end);
                Integer [][]payloadBeforeByte=new Integer[31][2];
                for(int i=0;i<daysInMonth;i++) {
                	
                	payloadBeforeByte[i][0]=monthlyReservationsCount[i];
                	payloadBeforeByte[i][1]=monthlyWaitingListCount[i];
                }
                byte[] payload = KryoUtil.serialize(payloadBeforeByte);
                boolean saved = reportDAO.upsertReportBytes(conn, VISITOR_REPORT_TYPE,target.toString(), payload);
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
}
