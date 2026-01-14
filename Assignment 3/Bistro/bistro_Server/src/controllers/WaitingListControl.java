package controllers;

import java.sql.Connection;
import java.util.List;

import database.DBManager;
import database.ReservationDAO;
import database.SeatingDAO;
import database.WaitingListDAO;
import entities.Reservation;
import entities.WaitingList;
import requests.WaitingListRequest;
import responses.Response;
import responses.WaitingListResponse;

public class WaitingListControl {

	private final WaitingListDAO waitingListDAO;
	private final ReservationDAO reservationDAO;
	private final SeatingDAO seatingDAO;
	private final SeatingControl seatingControl;
	public WaitingListControl() {
		this(new WaitingListDAO(),new ReservationDAO(),new SeatingDAO(),new SeatingControl());
	}
	public WaitingListControl(WaitingListDAO waitingListDAO, ReservationDAO reservationDAO,SeatingDAO seatingDAO,SeatingControl seatingControl) {
		
		this.waitingListDAO = waitingListDAO;
		this.reservationDAO = reservationDAO;
		this.seatingDAO=seatingDAO;
		this.seatingControl=seatingControl;
		
	}
	
	public Response<WaitingListResponse> cancelWaitingList(WaitingListRequest req) {
		try (Connection conn = DBManager.getConnection()) {
	        if (conn == null) {
	        	
	        		return new Response<>(false,"Failed to connect to db",null);
	        }
	        conn.setAutoCommit(false);
	        int confirmationCode=req.getConfirmationCode();
	        Reservation r=  reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
	        if(r==null) {
	    	    conn.rollback();
       		    return new Response<>(false,"Failed to locate reservation",null);
	        }
	        int reservationID=r.getReservationID();
	        
	        boolean hasUpdated=waitingListDAO.updateWaitingStatus(conn, reservationID, "CANCELLED");
	        if(!hasUpdated) {
	        	conn.rollback();
       		    return new Response<>(false,"failed to update waitingList status",null);
	        }
	        
	        hasUpdated = reservationDAO.updateStatusByReservationID(conn, reservationID, "CANCELLED");
	        if(!hasUpdated) {
	        	conn.rollback();
       		    return new Response<>(false,"failed to update waitingList status",null);
	        }
	        
	        conn.commit();
	       WaitingListResponse response= new WaitingListResponse(true);
	       return new Response<>(true,"We hope to see you another time !",response);
		
	}
		catch (Exception e) {
	        System.out.println("processCalledTimeouts failed: " + e.getMessage());
	        return new Response<>(false,"SQL failure",null);
	    }
	}
	
	public void cancelLateArrivalsFromWaitingListToTable() {
	    try (Connection conn = DBManager.getConnection()) {
	        if (conn == null) {
	            System.out.println("processCalledTimeouts: DB connection failed");
	            return;
	        }

	        conn.setAutoCommit(false);

	        List<WaitingList> expired = waitingListDAO.fetchExpiredCalled(conn);
	        if (expired == null || expired.isEmpty()) {
	            conn.rollback();
	            return;
	        }

	        for (WaitingList w : expired) {
	            try {
	                int reservationId = w.getReservationID();

	                Integer seatingId = seatingDAO.getSeatingIdByReservationId(conn, reservationId);
	                Integer tableId = (seatingId != null) ? seatingDAO.getTableIDBySeatingID(conn, seatingId) : null;

	                Reservation reservation = reservationDAO.getReservationByReservationID(conn, reservationId);
	                if (reservation == null || reservation.getStatus() == null) {
	                    
	                    conn.rollback();
	                    conn.setAutoCommit(false);
	                    continue;
	                }

	                if (!"CALLED".equalsIgnoreCase(reservation.getStatus())) {
	                    
	                    conn.rollback();
	                    conn.setAutoCommit(false);
	                    continue;
	                }

	                
	                boolean waitingCancelled = waitingListDAO.updateWaitingStatus(conn, reservationId, "CANCELLED");
	                if (!waitingCancelled) {
	                    conn.rollback();
	                    conn.setAutoCommit(false);
	                    continue;
	                }
	                
	                // 2) cancel reservation
	                boolean reservationCancelled = reservationDAO.updateStatusByReservationID(conn, reservationId, "CANCELLED");
	                if (!reservationCancelled) {
	                    conn.rollback();
	                    conn.setAutoCommit(false);
	                    continue;
	                }

	                
	                conn.commit();
	                conn.setAutoCommit(false);

	               
	                if (tableId != null) {
	                    boolean freed = seatingControl.checkOutCurrentSeating(conn, tableId);
	                    if (!freed) {
	                        conn.rollback();
	                        conn.setAutoCommit(false);
	                        continue;
	                    }
	                    try {
	                        seatingControl.tryAssignNextFromWaitingList(conn, tableId);
	                        conn.commit();
	                        conn.setAutoCommit(false);
	                    } catch (Exception e) {
	                        try { conn.rollback(); } catch (Exception ignore) {}
	                        conn.setAutoCommit(false);
	                        System.out.println("tryAssignNextFromWaitingList failed for tableId=" + tableId + ": " + e.getMessage());
	                        
	                    }
	                } else {
	                    conn.rollback();
	                    conn.setAutoCommit(false);
	                }

	            } catch (Exception perRow) {
	                try { conn.rollback(); } catch (Exception ignore) {}
	                conn.setAutoCommit(false);
	                System.out.println("processCalledTimeouts per-row failed: " + perRow.getMessage());
	            }
	        }
	        
	        try { conn.commit(); } catch (Exception ignore) {}

	    } catch (Exception e) {
	        System.out.println("processCalledTimeouts failed: " + e.getMessage());
	    }
	}



	
		
}
