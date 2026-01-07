package controllers;
import java.sql.Connection;

import java.sql.SQLException;
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
import requests.ReservationRequest;
import requests.SeatingRequest;
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
    
    public Response<SeatingResponse> handleSeatingRequest(SeatingRequest req) {
        if (req == null) return new Response<>(false, "Request is missing", null);
        if (req.getConfirmationCode() > 0) {
            return checkInByConfirmationCode(req.getConfirmationCode());
        } else {
            return checkInForNonReserved(req.getReservation());
        }
    }
    
    public Response<SeatingResponse> checkInForNonReserved(ReservationRequest rr) {

        if (rr == null)
            return new Response<>(false, "Reservation details are missing", null);

        try (Connection conn = DBManager.getConnection()) {

            if (conn == null)
                return new Response<>(false, "DB connection failed", null);

            conn.setAutoCommit(false);

            try {
                int partySize = rr.getPartySize();
                if (partySize <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Party size is invalid", null);
                }

                int allocatedCapacity = tableDAO.getMinimalTableSize(conn, partySize);
                if (allocatedCapacity <= 0) {
                    rollback(conn);
                    return new Response<>(false, "No suitable table size exists", null);
                }
                Table table = tableDAO.findAvailableTable(conn, allocatedCapacity);
                int confirmationCode = reservationDAO.generateConfirmationCode(conn);
                int reservationId = reservationDAO.insertNewReservation(conn,LocalDate.now(),partySize,allocatedCapacity,confirmationCode,null,LocalTime.now(),
                        "NEW",
                        rr.getGuestContact()
                );

                if (reservationId <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Failed to create reservation for walk-in", null);
                }
                if (table == null) {
                    boolean waitInserted =waitingListDAO.insertNewWait(conn, reservationId, "WAITING", 0);
                    if (!waitInserted) {
                        rollback(conn);
                        return new Response<>(false, "Failed to add to waiting list", null);
                    }
                    boolean statusUpdated =reservationDAO.updateStatusByReservationID(conn, reservationId, "WAITING");
                    if (!statusUpdated) {
                        rollback(conn);
                        return new Response<>(false, "Failed to update reservation status", null);
                    }
                    conn.commit();
                    return new Response<>(false,"No available table right now - added to waiting list",null);
                }
                int seatingId = seatingDAO.checkIn(conn, table.getTableID(), reservationId);
                if (seatingId == -1) {
                    rollback(conn);
                    return new Response<>(false, "Failed to create seating record", null);
                }
                boolean statusUpdated =reservationDAO.updateStatusByReservationID(conn, reservationId, "SEATED");
                if (!statusUpdated) {
                    rollback(conn);
                    return new Response<>(false, "Failed to update reservation status", null);
                }
                conn.commit();
                SeatingResponse seatingResponse =new SeatingResponse(table.getTableNumber(), table.getCapacity(), LocalTime.now());
                return new Response<>(true,"Bon appetite! Your table number: " + table.getTableNumber(),seatingResponse);

            } catch (Exception e) {
                rollback(conn);
                return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
            }
        } catch (Exception e) {
            return new Response<>(false, "DB connection failed: " + e.getMessage(), null);
        }
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
            	
            	return handleValidationFailure(conn, r, confirmationCode, validationMsg);
            }
                                         
            Table table = tableDAO.findAvailableTable(conn, r.getAllocatedCapacity());
            if (table != null) {
            	
            	return seatNow(conn, r, confirmationCode, table);           	
            }
            
            return handleNoTableAvailable(conn, r, confirmationCode);
      
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
    
    
    /**
     * validation of the reservation of:
     * 1.reservation exist
     * 2.reservation date in the current date
     * 3.reservation status exist
     * 4.the reservation is not cancelled
     * 5.reservation in not already seated
     * 6.waiting reservation 
     * 7.only allow confirmed or in wait reservations
     * 8.reservation dont have time
     * 9.the guest arrived 15 minutes earlier and less
     * 10.guest arrived in time window of 15 mintues before the startTime 
     * 11.guest was late reservation cancelled
     * @param r
     * @return
     */
    private String validateReservationForCheckIn(Reservation r) {
    	
    	String status = r.getStatus();
    	
    	if(r==null) return "Reservation is missing";
    	
    	if(!LocalDate.now().equals(r.getReservationDate())) return "Not the date of the resevation";
    	
    	if(status == null) return "Reservation status is missing";
    	if("CANCELLED".equalsIgnoreCase(status)) return "The reservation is cancelled";
    	if("SEATED".equalsIgnoreCase(status)) return "Already seated";
    	if ("WAITING".equalsIgnoreCase(status)) return null;
    	
    	boolean okStatus ="CONFIRMED".equalsIgnoreCase(status) || "WAITING".equalsIgnoreCase(status);
    	if (!okStatus) return "Reservation cannot be checked-in (status=" + status + ")";        
                
    	
    	if(r.getStartTime() == null) return "Reservation start time is missing";
    	    	   	
    	LocalDateTime start = LocalDateTime.of(r.getReservationDate(), r.getStartTime());
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(start.minusMinutes(15))) return "TOO_EARLY";
        if (now.isBefore(start))                  return "EARLY";
        if (now.isAfter(start.plusMinutes(15)))   return "Arrived too late";
        
        return null;
    	
    }

   //-----------------------------------------after refactor helper methodss for checkIn-------------------------------------------------------
    /**
     * if validation result is {@code "EARLY"} and the reservation is not already {@code WAITING},
     * the customer is inserted into the waiting list and the reservation status is updated to {@code WAITING}.
 	 * Otherwise, the transaction is rolled back and the validation message is returned as failure.
 	 *
     * @param conn active JDBC connection (transaction)
     * @param r the fetched reservation (can be null)
     * @param confirmationCode reservation confirmation code
     * @param msg validation failure message (may include special token {@code "EARLY"})
     * @return failure response with message, or waiting-list response when applicable
     * @throws SQLException
     */
    public Response<SeatingResponse> handleValidationFailure(
            Connection conn, Reservation r, int confirmationCode, String msg) throws SQLException{
    	if ("EARLY".equals(msg) && r != null && !"WAITING".equalsIgnoreCase(r.getStatus())) {
            return moveToWaiting(conn, r, confirmationCode, 1, "Arrived early - added to waiting list");
        }
    	
    	rollback(conn);
    	return new Response<>(false,msg,null);
    }
    
    /**
     * Handles the case where no table is available right now.
     * @param conn active JDBC connection (transaction)
     * @param r 
     * @param confirmationCode
     * @return
     * @throws SQLException
     */
    private Response<SeatingResponse> handleNoTableAvailable(
            Connection conn, Reservation r, int confirmationCode) throws SQLException {

        // If already waiting, don't insert again
        if (r != null && "WAITING".equalsIgnoreCase(r.getStatus())) {
            rollback(conn);
            return new Response<>(false, "Still no available table", null);
        }

        return moveToWaiting(conn, r, confirmationCode,1, "No table right now - added to waiting list");
    }
    
    /**
     * Seats the customer immediately: work flow
     * 1.Insert a seating row (occupy the chosen table)
     * 2.Update reservation status to @code SEATED
     * 
     * @param conn
     * @param r
     * @param confirmationCode
     * @param table
     * @return
     * @throws SQLException
     */
    private Response<SeatingResponse> seatNow(
            Connection conn, Reservation r, int confirmationCode, Table table) throws SQLException {

        int seatingId = seatingDAO.checkIn(conn, table.getTableID(), r.getReservationID());
        if (seatingId == -1) {
            rollback(conn);
            return new Response<>(false, "Failed to create seating record", null);
        }

        if (!reservationDAO.updateStatus(conn, confirmationCode, "SEATED")) {
            rollback(conn);
            return new Response<>(false, "Failed to update reservation status", null);
        }

        conn.commit();
        SeatingResponse seatingResponse =
                new SeatingResponse(table.getTableNumber(), table.getCapacity(), LocalTime.now());

        return new Response<>(true,
                "Bon apetite your table number: " + table.getTableNumber(),
                seatingResponse);
    }

    
    /**
     * Moves a reservation into the waiting list within the current transaction: work flow
     * 1.Insert waiting-list row with @code WAITING status and given priority
     * 2.Commit the transaction
     * @param conn
     * @param r
     * @param confirmationCode
     * @param priority
     * @param msg
     * @return
     * @throws SQLException
     */
    private Response<SeatingResponse> moveToWaiting(
            Connection conn, Reservation r, int confirmationCode, int priority, String msg) throws SQLException {

        if (r == null) {
            rollback(conn);
            return new Response<>(false, "Reservation not found", null);
        }

        boolean waitInserted = waitingListDAO.insertNewWait(conn, r.getReservationID(), "WAITING", priority);
        if (!waitInserted) {
            rollback(conn);
            return new Response<>(false, "Failed to add to waiting list", null);
        }

        boolean statusUpdated = reservationDAO.updateStatus(conn, confirmationCode, "WAITING");
        if (!statusUpdated) {
            rollback(conn);
            return new Response<>(false, "Failed to update reservation status", null);
        }

        conn.commit();
        return new Response<>(false, msg, null);
    }
    
    
    /**
     * Attempts to roll back the current transaction without throwing exceptions to the caller.
     * Used for cleanup when a failure occurs mid-transaction
     * @param conn
     */
    private void rollback(Connection conn) {
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException ignore) {}
    }
}
