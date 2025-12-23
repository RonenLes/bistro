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
    public Response<Table> checkInByConfirmationCode(int confirmationCode) {
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
            	return new Response<Table>(true,"Bon apetite your table number: "+table.getTableNumber(),table);            	
            }
            
            waitingListDAO.insertNewWait(conn, r.getReservationID(),"WAITING", 1);
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
    
    
    
    private String validateReservationForCheckIn(Reservation r) {   	
    	if(r==null) return "Reservation is missing";
    	if(!LocalDate.now().equals(r.getReservationDate())) return "Not the date of the resevation";
    	if("CANCELLED".equalsIgnoreCase(r.getStatus())) return "The reservation is cancelled";
    	if(!"CONFIRMED".equalsIgnoreCase(r.getStatus())) return "Reservation is not confirmed";
    	if(r.getStartTime() == null) return "Reservation start time is missing";
    	LocalDateTime start = LocalDateTime.of(r.getReservationDate(), r.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(start.plusMinutes(15))) return "Arrived too late (more than 15 minutes after start time)";
        if(now.isAfter(start.plusMinutes(-15))) return "Arrived too early";
        return null;
    	
    }

   
}
