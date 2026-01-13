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
import database.UserDAO;
import database.WaitingListDAO;
import entities.Reservation;
import entities.Table;
import entities.User;
import entities.WaitingList;
import requests.ReservationRequest;
import requests.SeatingRequest;
import responses.Response;
import responses.SeatingResponse;
import responses.SeatingResponse.SeatingResponseType;

public class SeatingControl {

    private final ReservationDAO reservationDAO;
    private final TableDAO tableDAO;
    private final SeatingDAO seatingDAO;
    private final WaitingListDAO waitingListDAO;
    private final NotificationControl notificationControl;
    private final UserDAO userDAO;
    public SeatingControl() {
        this(new ReservationDAO(), new TableDAO(), new SeatingDAO(), new WaitingListDAO(),new NotificationControl(),new UserDAO());
    }

    public SeatingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO,WaitingListDAO waitingListDAO,NotificationControl notificationControl,UserDAO userDAO) {
        this.reservationDAO = reservationDAO;
        this.tableDAO = tableDAO;
        this.seatingDAO = seatingDAO;
        this.waitingListDAO = waitingListDAO;
        this.notificationControl=notificationControl;
        this.userDAO=userDAO;
    }
    
    public Response<SeatingResponse> handleSeatingRequest(SeatingRequest req) {
        if (req == null) return new Response<>(false, "Request is missing", null);
        if(req.getType()==null)return new Response<>(false, "type is missing", null);
       return switch(req.getType()) {
        	case BY_CONFIRMATIONCODE -> checkInRouterByConfirmationCode(req.getConfirmationCode());
        	case BY_RESERVATION -> checkInForNonReserved(req.getReservation());
        };
    }
    public Response<SeatingResponse> checkInRouterByConfirmationCode(int confirmationCode) {
        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return new Response<>(false, "Couldnt connect to db", null);
            conn.setAutoCommit(false);

            try {
                Reservation r = reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
                if (r == null) {
                    conn.rollback();
                    return new Response<>(false, "Reservation not found", null);
                }

                // We treat as "CALLED customer" ONLY if:
                // (1) reservation.status == CALLED
                // (2) there is already a seating row for this reservation
                // (3) that seating row has checkInTime NULL (held seating)
                Integer seatingId = seatingDAO.getSeatingIdByReservationId(conn, r.getReservationID());
                boolean isCalled = "CALLED".equalsIgnoreCase(r.getStatus());

                if (isCalled && seatingId != null && seatingDAO.isCheckInNull(conn, seatingId)) {
                    // called customer check-in
                    Response<SeatingResponse> resp = checkInForCalledCustomer(conn, r, seatingId);
                    if (!resp.isSuccess()) { conn.rollback(); return resp; }
                    conn.commit();
                    return resp;
                }

                // normal reservation check-in
                Response<SeatingResponse> resp = checkInByConfirmationCode(conn, r);
                if (!resp.isSuccess()) { conn.rollback(); return resp; }
                conn.commit();
                return resp;

            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignore) {}
                return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
            }
        } catch (Exception e) {
            return new Response<>(false, "DB error: " + e.getMessage(), null);
        }
    }

    private Response<SeatingResponse> checkInForCalledCustomer(Connection conn, Reservation r, int seatingId) throws SQLException {
    	if (r == null) return new Response<>(false, "Reservation is null", null);
        if (r.getStatus() == null || !"CALLED".equalsIgnoreCase(r.getStatus())) {
            return new Response<>(false, "Customer is not in CALLED status", null);
        }
        
        boolean ok = seatingDAO.markCheckInNow(conn, seatingId);
        if (!ok) return new Response<>(false, "Failed to check in seating", null);

        boolean updatedWaitingList=waitingListDAO.markAssignedIfCalled(conn, r.getReservationID()); 
        if(!updatedWaitingList) return new Response<>(false, "Failed to update waiting list status", null);
        
        boolean updated = reservationDAO.updateStatusByReservationID(conn, r.getReservationID(), "SEATED");
        if (!updated) return new Response<>(false, "Failed to update reservation status", null);
        
        
        Integer tableId=seatingDAO.getTableIDBySeatingID(conn, seatingId);
        if(tableId==null) {
        	return new Response<>(false, "failed to return a response", null);
        }
        Table table=tableDAO.fetchTableByID(conn, tableId);
        if(table==null) {
        	return new Response<>(false, "failed to return a response", null);
        }
        
        
        SeatingResponse sr = new SeatingResponse(table.getTableNumber(),table.getCapacity(),LocalTime.now(),SeatingResponse.SeatingResponseType.CUSTOMER_CHECKED_IN);
        return new Response<>(true, "Checked in (called customer)", sr);
    }



	public Response<SeatingResponse> checkInForNonReserved(ReservationRequest rr) {

        if (rr == null) {
            return new Response<>(false, "Reservation details are missing", null);
        }

        try (Connection conn = DBManager.getConnection()) {

            if (conn == null) {
                return new Response<>(false, "DB connection failed", null);
            }

            conn.setAutoCommit(false);

            try {
                int partySize = rr.getPartySize();
                if (partySize <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Party size is invalid", null);
                }

                String userID = rr.getUserID();
                String guestContact = rr.getGuestContact();

                LocalDate today = LocalDate.now();
                LocalTime nowTime = LocalTime.now();

                int allocatedCapacity = tableDAO.getMinimalTableSize(conn, partySize);
                if (allocatedCapacity <= 0) {
                    rollback(conn);
                    return new Response<>(false, "No suitable table size exists", null);
                }
                
                Table table = tableDAO.findAvailableTable(conn, allocatedCapacity);
                
                int confirmationCode = reservationDAO.generateConfirmationCode(conn);
                
                int reservationId = reservationDAO.insertNewReservation(conn,today,partySize,allocatedCapacity,confirmationCode, userID,nowTime,"CONFIRMED",guestContact);

                if (reservationId <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Failed to create reservation for walk-in", null);
                }
                if (table == null) {
                    return moveToWaiting(conn,reservationId,confirmationCode,0,"No available table right now - added to waiting list");
                }
                Reservation r=reservationDAO.getReservationByReservationID(conn, reservationId);
                boolean isSent;
                if(r.getGuestContact()==null || r.getGuestContact().isBlank()) {
                	User user=userDAO.getUserByUserID(conn, userID);
                	if(user==null) {
                		rollback(conn);
                        return new Response<>(false, "couldnt find user to send confirmation code to", null);
                	}
                	isSent=notificationControl.sendConfirmationToUser(user, confirmationCode);
                	if(!isSent) {
                		rollback(conn);
                		return new Response<>(false, "couldnt send confirmation code to user", null);
                	}
                }
                else {
                	isSent=notificationControl.sendConfirmationToGuest(guestContact, confirmationCode);
                	if(!isSent) {
                		rollback(conn);
                		return new Response<>(false, "couldnt send confirmation code to guest", null);
                	}
                }
                int seatingId = seatingDAO.checkIn(conn, table.getTableID(), reservationId);
                if (seatingId == -1) {
                    rollback(conn);
                    return new Response<>(false, "Failed to create seating record", null);
                }
                
                boolean statusUpdated = reservationDAO.updateStatusByReservationID(conn, reservationId, "SEATED");
                if (!statusUpdated) {
                    rollback(conn);
                    return new Response<>(false, "Failed to update reservation status", null);
                }
                
                
                conn.commit();

                SeatingResponse seatingResponse =new SeatingResponse(table.getTableNumber(), table.getCapacity(), nowTime,
                		SeatingResponse.SeatingResponseType.CUSTOMER_CHECKED_IN);

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
    public Response<SeatingResponse> checkInByConfirmationCode(Connection conn,Reservation r) {
        try  {
        	if(r==null) return new Response<>(false, "reservation is null error" ,null);
        	Integer confirmationCode=r.getConfirmationCode();
            String validationMsg= validateReservationForCheckIn(r);
            
            if (validationMsg!=null) {
            	
            	return handleValidationFailure(conn, r, confirmationCode, validationMsg);
            }                      
            Table table = tableDAO.findAvailableTable(conn, r.getAllocatedCapacity());
            if (table != null) {
            	System.out.println("Found table,seating the customer now.");
            	return seatNow(conn, r, confirmationCode, table);           	
            }
            
            return handleNoTableAvailable(conn, r, confirmationCode);
      
        } catch (Exception e) {
            return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
        }
    }
    
    public boolean checkOutCurrentSeating(Connection conn, int tableID) throws SQLException {
        if (conn == null) return false;

        Table table = tableDAO.fetchTableByID(conn, tableID);
        if (table == null) return false;

        Integer seatingID = seatingDAO.fetchOpenSeatingID(conn, tableID);
        Integer seatedReservationID = seatingDAO.findOpenSeatingReservationId(conn, tableID);
        if (seatingID == null || seatedReservationID == null) return false;

        if (!seatingDAO.checkOutBySeatingId(conn, seatingID)) return false;
        
        if (!reservationDAO.updateStatusByReservationID(conn, seatedReservationID, "COMPLETED")) return false;

        return true;
    }
    public boolean tryAssignNextFromWaitingList(Connection conn, int tableID) throws SQLException {
        if (conn == null) return false;

        Table table = tableDAO.fetchTableByID(conn, tableID);
        if (table == null) return false;

        WaitingList nextInLine = waitingListDAO.getNextWaitingThatFits(conn, table.getCapacity());
        if (nextInLine == null) return true;

        Reservation r = waitingListDAO.getReservationByWaitingID(conn, nextInLine.getWaitID());
        if (r == null) return false;

        // 1) create held seating first (so you don't "call" someone without holding a slot)
        int heldSeatingId = seatingDAO.insertHeldSeating(conn, tableID, r.getReservationID());
        if (heldSeatingId == -1) return false;

        // 2) send notification (if this fails, caller should rollback, and held seating won't exist)
        boolean hasSent;
        if (r.getGuestContact() == null || r.getGuestContact().isBlank()) {
            User user = userDAO.getUserByUserID(conn, r.getUserID());
            if (user == null) return false;

            hasSent = notificationControl.sendInviteToTable(
                user.getEmail(),
                user.getPhone(),
                "Your table is ready, confirmation code: " + r.getConfirmationCode() + ". Arrive in 15 minutes!"
            );
        } else {
            hasSent = notificationControl.sendInviteToTable(
                r.getGuestContact(),
                "Your table is ready, confirmation code: " + r.getConfirmationCode() + ". Arrive in 15 minutes!"
            );
        }
        if (!hasSent) return false;

        // 3) mark waiting list called
        if (!waitingListDAO.markCalled(conn, nextInLine.getWaitID())) return false;

        // 4) update reservation status
        if (!reservationDAO.updateStatusByReservationID(conn, r.getReservationID(), "CALLED")) return false;

        return true;
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
    	if(r==null) return "Reservation is missing";
    	String status = r.getStatus();	
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
            return moveToWaiting(conn, r.getReservationID(), confirmationCode, 1, "Arrived early - added to waiting list");
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
        return moveToWaiting(conn, r.getReservationID(), confirmationCode,1, "No table right now - added to waiting list");
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
        SeatingResponse seatingResponse =new SeatingResponse(table.getTableNumber(), table.getCapacity(), LocalTime.now(),SeatingResponseType.CUSTOMER_CHECKED_IN);

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
            Connection conn, int reservationId, int confirmationCode, int priority, String msg) throws SQLException {
    	
        boolean waitInserted = waitingListDAO.insertNewWait(conn, reservationId, "WAITING", priority);
        if (!waitInserted) {
            rollback(conn);
            return new Response<>(false, "Failed to add to waiting list", null);
        }

        boolean statusUpdated = reservationDAO.updateStatusByReservationID(conn, reservationId, "WAITING");
        if (!statusUpdated) {
            rollback(conn);
            return new Response<>(false, "Failed to update reservation status", null);
        }
        Reservation r=reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
        if (r==null) {
        	rollback(conn);
            return new Response<>(false, "Failed to update reservation status", null);
        }
        if(r.getGuestContact()==null ||r.getGuestContact().isBlank()) {
        	User user=userDAO.getUserByUserID(conn, r.getUserID());
        	if(user==null) {
        		rollback(conn);
                return new Response<>(false, "failed to get user details", null);
        	}
        	boolean sendToUser=notificationControl.sendNotificationEnteringWaitingList(user.getEmail(),user.getPhone(),"Hello,you have entered the waiting list! your confirmation code is: "+confirmationCode);
        	if(!sendToUser) {
        		rollback(conn);
                return new Response<>(false, "failed to send user details", null);
        	}
        }
        else {
        	boolean sendToGuest=notificationControl.sendNotificationEnteringWaitingList(r.getGuestContact(), "Hello,you have entered the waiting list! your confirmation code is: "+confirmationCode);
        	if(!sendToGuest) {
        		rollback(conn);
                return new Response<>(false, "failed to send user details", null);
        	}
        }
        conn.commit();
        SeatingResponse seatingResponse =
                new SeatingResponse(null, null, null, SeatingResponse.SeatingResponseType.CUSTOMER_IN_WAITINGLIST);
        return new Response<>(true, msg, seatingResponse);
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
