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
	        if (expired.isEmpty()) {
	            conn.rollback();
	            return;
	        }
	        for (WaitingList w : expired) {
	            int reservationId = w.getReservationID();
	            Integer seatingId = seatingDAO.getSeatingIdByReservationId(conn, reservationId);
	            Integer tableId = (seatingId != null) ? seatingDAO.getTableIDBySeatingID(conn, seatingId) : null;
	            Reservation reservation = reservationDAO.getReservationByReservationID(conn, reservationId);
	            if (reservation == null || reservation.getStatus() == null) {
	                
	                continue;
	            }   
	            if (!"CALLED".equalsIgnoreCase(reservation.getStatus())) {
	                continue;
	            }

	            
	            boolean waitingCancelled = waitingListDAO.updateWaitingStatus(conn, reservationId, "CANCELLED");
	            if (!waitingCancelled) {
	            	conn.rollback();          
	                conn.setAutoCommit(false);
	                continue;
	            }
	            boolean reservationCancelled =reservationDAO.updateStatusByReservationID(conn, reservationId, "CANCELLED");
	            if (!reservationCancelled) {
	               
	                conn.rollback();
	                conn.setAutoCommit(false);
	                continue;
	            }

	            
	            conn.commit();
	            conn.setAutoCommit(false);

	            
	            if (tableId != null) {
	                seatingControl.checkOutAndAssignNew(tableId);
	            } else {
	                System.out.println("processCalledTimeouts: No active seating/table found for reservationID=" + reservationId);
	            }
	        }

	        conn.commit();

	    } catch (Exception e) {
	        System.out.println("processCalledTimeouts failed: " + e.getMessage());
	    }
	}


	
		
}
