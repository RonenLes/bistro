package controllers;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import database.DBManager;
import database.ReservationDAO;
import database.SeatingDAO;
import database.TableDAO;
import database.WaitingListDAO;
import entities.Reservation;
import entities.Table;
import entities.WaitingList;
import responses.Response;
import responses.SeatingResponse;

public class SeatingControl {

    private final ReservationDAO reservationDAO;
    private final TableDAO tableDAO;
    private final SeatingDAO seatingDAO;
    private final WaitingListDAO waitingListDAO;
    
    public SeatingControl() {
        this(new ReservationDAO(), new TableDAO(), new SeatingDAO(), new WaitingListDAO());
    }

    public SeatingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO,WaitingListDAO waitingListDAO) {
        this.reservationDAO = reservationDAO;
        this.tableDAO = tableDAO;
        this.seatingDAO = seatingDAO;
        this.waitingListDAO = waitingListDAO;
    }
    
    
    /**
     * method that checks reservations, current seating, and tables to find an available table to check in the customer and 
     * assign a table for him
     * @param confirmationCode of the reservation
     * @return table entity with all the needed details about the table including tableNumber
     */
    public Response<SeatingResponse> checkInByConfirmationCode(int confirmationCode) {
        try (Connection conn = DBManager.getConnection()) {
            conn.setAutoCommit(false);

            
            Reservation r = reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
            String validationMsg= validateReservationForCheckIn(r);
            if (validationMsg!=null) {
                conn.rollback();
                return new Response<>(false, validationMsg, null);
            }
                    
           
            Table table = tableDAO.findAvailableTable(conn, r.getAllocatedCapacity());
            if (table != null) {
            	
            	int seating = seatingDAO.checkIn(conn, table.getTableID(), r.getReservationID());
            	if(seating == -1) {
            		conn.rollback();
            		return new Response<>(false,"Failed to create seating record",null);
            	}
            	
            	boolean statusUpdated = reservationDAO.updateStatus(conn,confirmationCode, "SEATED");
            	if(!statusUpdated) {
            		conn.rollback();
                    return new Response<>(false, "Failed to update reservation status", null);
            	}
            	
            	conn.commit();
            	SeatingResponse seatingResponse = new SeatingResponse(table.getTableNumber(),table.getCapacity(),LocalTime.now());
            	return new Response<SeatingResponse>(true,"Bon apetite your table number: "+table.getTableNumber(),seatingResponse);            	
            }
            
            boolean waitUpdate = waitingListDAO.insertNewWait(conn, r.getReservationID(),"WAITING", 1);
            if(!waitUpdate) {
        		conn.rollback();
                return new Response<>(false, "Failed to add to waiting list", null);
        	}
            
            boolean statusUpdated = reservationDAO.updateStatus(conn,confirmationCode, "WAITING");
            if(!statusUpdated) {
        		conn.rollback();
                return new Response<>(false, "Failed to update reservation status", null);
        	}
            conn.commit();
            return new Response<>(false, "No table right now - added to waiting list", null);
      
        } catch (Exception e) {
            return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
        }
    }
    
    
    public Response<SeatingResponse> checkOutAndAssignNew(int tableID){
    	try (Connection conn = DBManager.getConnection()){
    		conn.setAutoCommit(false);
    		
    		//1.does table even exist?
    		Table table = tableDAO.fetchTableByID(conn, tableID);
    		if(table==null) {
    			conn.rollback();
    			System.out.println("checkout fail - Table not found");
    			return new Response<>(false,"Table not found",null);
    		}
   		
    		//2.find open seating
    		Integer seatingID = seatingDAO.fetchOpenSeatingID(conn, tableID);
    		Integer seatedReservationID = seatingDAO.findOpenSeatingReservationId(conn, tableID);    		   		
    		if(seatingID == null || seatedReservationID == null) {
    			conn.rollback();
    			System.out.println("checkout fail - Table is not currently occiupied");
    			return new Response<>(false,"Table is not currently occiupied",null);
    		}
    		
    		boolean checkOut = seatingDAO.checkOutBySeatingId(conn, seatingID);
    		if(!checkOut) {
    			conn.rollback();
    			return new Response<>(false,"Failed to checkout seating",null);
    		}
    		
    		//3.update reservation status to completed
    		boolean completed = reservationDAO.updateStatusByReservationID(conn, seatedReservationID, "COMPLETED");
    		if(!completed) {
    			conn.rollback();
    			return new Response<>(false,"Failed to update reservation status to COMPLETED",null);
    		}
    		
    		//4.if there is customer in the waiting list try to assign them by priority
    		WaitingList nextInLine = waitingListDAO.getNextWaitingThatFits(conn, table.getCapacity());
    		if(nextInLine == null) {
    			conn.rollback();
    			return new Response<>(false, "Checked out. No one is waiting.", null);
    		}
    		
    		//5.occupy the table and fetch the table number for the customer
    		int newSeating = seatingDAO.checkIn(conn, table.getTableID(),nextInLine.getReservationID());
    		if(newSeating == -1) {
    			conn.rollback();
    			return new Response<>(false, "Failed to seat next waiting customer", null);
    		}
    		
    		//6.mark row in waiting list db where we popped the nextInLine customer 
    		boolean waitStatus = waitingListDAO.markAssigned(conn, nextInLine.getWaitID());
    		if(!waitStatus) {
    			conn.rollback();
    			return new Response<>(false, "Failed to mark waiting list as ASSIGNED", null);
    		}
    		
    		//7.update reservation status row in db
    		boolean reservationStatus = reservationDAO.updateStatusByReservationID(conn, nextInLine.getReservationID(), "SEATED");
    		if(!reservationStatus) {
    			conn.rollback();
    			return new Response<>(false, "Failed to update next reservation status to SEATED", null);
    		}
    		
    		conn.commit();
    		
    		SeatingResponse seatingResponse = new SeatingResponse(table.getTableNumber(),table.getCapacity(),LocalTime.now());
    		return new Response<>(true,"Checked out. Next customer assigned to table + "+table.getTableNumber(),seatingResponse);
    		
    		
    	}catch(Exception e) {
    		System.out.println(e.getMessage());
    		return new Response<>(false, "Checkout failed: " + e.getMessage(), null);
    	}
    }
    
    
    
    private String validateReservationForCheckIn(Reservation r) {   	
    	if(r==null) return "Reservation is missing";
    	if(!LocalDate.now().equals(r.getReservationDate())) return "Not the date of the resevation";
    	if("CANCELLED".equalsIgnoreCase(r.getStatus())) return "The reservation is cancelled";
    	if(!"CONFIRMED".equalsIgnoreCase(r.getStatus())) return "Reservation is not confirmed";
    	if(r.getStartTime() == null) return "Reservation start time is missing";
    	LocalDateTime start = LocalDateTime.of(r.getReservationDate(), r.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(start.plusMinutes(15))) return "Arrived too late (more than 15 minutes after start time)";
        if(now.isAfter(start.minusMinutes(15))) return "Arrived too early";
        return null;
    	
    }

   
}
