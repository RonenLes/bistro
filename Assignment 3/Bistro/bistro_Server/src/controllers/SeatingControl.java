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

/**
 * SeatingControl is responsible for check-in / check-out flows.
 *
 * Main responsibilities:
 * - Route seating requests coming from the client (by confirmation code or by reservation details).
 * - Perform transactional DB operations for seating (insert seating, update reservation status, waiting list updates).
 * - Coordinate notifications (confirmation code, waiting list, table ready invites).
 *
 * Notes:
 * - This class uses explicit transactions (conn.setAutoCommit(false)) and commits/rollbacks manually.
 * - Some helpers commit internally (e.g., seatNow/moveToWaiting) based on your current design.
 */
public class SeatingControl {

    private final ReservationDAO reservationDAO;
    private final TableDAO tableDAO;
    private final SeatingDAO seatingDAO;
    private final WaitingListDAO waitingListDAO;
    private final NotificationControl notificationControl;
    private final UserDAO userDAO;

    /**
     * default constructor for kryo
     */
    public SeatingControl() {
        this(new ReservationDAO(), new TableDAO(), new SeatingDAO(), new WaitingListDAO(),new NotificationControl(),new UserDAO());
    }

    /**
     * constructor
     * @param reservationDAO
     * @param tableDAO
     * @param seatingDAO
     * @param waitingListDAO
     * @param notificationControl
     * @param userDAO
     */
    public SeatingControl(ReservationDAO reservationDAO, TableDAO tableDAO, SeatingDAO seatingDAO,WaitingListDAO waitingListDAO,NotificationControl notificationControl,UserDAO userDAO) {
        this.reservationDAO = reservationDAO;
        this.tableDAO = tableDAO;
        this.seatingDAO = seatingDAO;
        this.waitingListDAO = waitingListDAO;
        this.notificationControl=notificationControl;
        this.userDAO=userDAO;
    }

    /**
     * Entry point for seating requests coming from the client.
     * Routes the request based on {@link SeatingRequest#getType()}.
     *
     * Supported flows:
     * - BY_CONFIRMATIONCODE: check-in flow by confirmation code.
     * - BY_RESERVATION: check-in flow for walk-in / non-reserved customers (creates reservation and seats if possible).
     *
     * @param req client's seating request payload
     * @return Response with SeatingResponse describing the outcome
     */
    public Response<SeatingResponse> handleSeatingRequest(SeatingRequest req) {
        if (req == null) return new Response<>(false, "Request is missing", null);
        if(req.getType()==null)return new Response<>(false, "type is missing", null);
       return switch(req.getType()) {
            case BY_CONFIRMATIONCODE -> checkInRouterByConfirmationCode(req.getConfirmationCode());
            case BY_RESERVATION -> checkInForNonReserved(req.getReservation());
        };
    }

    /**
     * Check-in router for a customer arriving with a confirmation code.
     *
     * 
     * - Otherwise: run the normal check-in flow (validate, seat if table available, else waiting list).
     *
     * @param confirmationCode confirmation code provided by the customer
     * @return Seating response indicating seated / waiting / failure
     */
    public Response<SeatingResponse> checkInRouterByConfirmationCode(int confirmationCode) {
        try (Connection conn = DBManager.getConnection()) {
            if (conn == null) return new Response<>(false, "Couldnt connect to db", null);
            conn.setAutoCommit(false);

            try {
                // 1) Load reservation
                Reservation r = reservationDAO.getReservationByConfirmationCode(conn, confirmationCode);
                if (r == null) {
                    conn.rollback();
                    return new Response<>(false, "Reservation not found", null);
                }

                // 2) Detect "CALLED" flow: reservation already called from waiting list and has held seating
                Integer seatingId = seatingDAO.getSeatingIdByReservationId(conn, r.getReservationID());
                boolean isCalled = "CALLED".equalsIgnoreCase(r.getStatus());

                if (isCalled && seatingId != null && seatingDAO.isCheckInNull(conn, seatingId)) {

                    // 2a) Complete the called-customer check-in (updates seating + waiting list + reservation status)
                    Response<SeatingResponse> resp = checkInForCalledCustomer(conn, r, seatingId);
                    if (!resp.isSuccess()) { conn.rollback(); return resp; }
                    conn.commit();
                    return resp;
                }

                // 3) Normal flow: validate, seat if possible, otherwise add to waiting list when relevant
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

    /**
     * Handles the special case where a reservation was previously "CALLED" from the waiting list.
     * In this flow you already have a held seating row created earlier.
     *
     *
     * @param conn active transaction connection
     * @param r reservation entity (must be CALLED)
     * @param seatingId held seating row id that belongs to this reservation
     * @return success -> CUSTOMER_CHECKED_IN, failure -> message + null payload
     * @throws SQLException if DAO operations fail
     */
    private Response<SeatingResponse> checkInForCalledCustomer(Connection conn, Reservation r, int seatingId) throws SQLException {
        if (r == null) return new Response<>(false, "Reservation is null", null);
        if (r.getStatus() == null || !"CALLED".equalsIgnoreCase(r.getStatus())) {
            return new Response<>(false, "Customer is not in CALLED status", null);
        }

        // 1) Set check-in time on the existing held seating row
        boolean ok = seatingDAO.markCheckInNow(conn, seatingId);
        if (!ok) return new Response<>(false, "Failed to check in seating", null);

        // 2) Update waiting list bookkeeping for called customer
        boolean updatedWaitingList=waitingListDAO.markAssignedIfCalled(conn, r.getReservationID());
        if(!updatedWaitingList) return new Response<>(false, "Failed to update waiting list status", null);

        // 3) Reservation status moves to SEATED
        boolean updated = reservationDAO.updateStatusByReservationID(conn, r.getReservationID(), "SEATED");
        if (!updated) return new Response<>(false, "Failed to update reservation status", null);

        // 4) Return table information
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

    /**
     * Walk-in / non-reserved check-in flow.
     *
     *
     * @param rr ReservationRequest that contains partySize and either userID or guestContact
     * @return Response with table details when seated, or waiting list response when no table available
     */
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
                // 1) Validate party size
                int partySize = rr.getPartySize();
                if (partySize <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Party size is invalid", null);
                }

                // 2) Identify contact method
                String userID = rr.getUserID();
                String guestContact = rr.getGuestContact();

                // 3) Use current date/time for a walk-in reservation
                LocalDate today = LocalDate.now();
                LocalTime nowTime = LocalTime.now();

                // 4) Decide which table sizes can fit this party
                int allocatedCapacity = tableDAO.getMinimalTableSize(conn, partySize);
                if (allocatedCapacity <= 0) {
                    rollback(conn);
                    return new Response<>(false, "No suitable table size exists", null);
                }

                // 5) Try to find free table
                Table table = tableDAO.findAvailableTable(conn, allocatedCapacity);

                // 6) Create reservation
                int confirmationCode = reservationDAO.generateConfirmationCode(conn);

                int reservationId = reservationDAO.insertNewReservation(conn,today,partySize,allocatedCapacity,confirmationCode, userID,nowTime,"CONFIRMED",guestContact);

                if (reservationId <= 0) {
                    rollback(conn);
                    return new Response<>(false, "Failed to create reservation for walk-in", null);
                }

                // 7) If no table now -> waiting list flow (priority 0 in your current code)
                if (table == null) {
                    return moveToWaiting(conn,reservationId,confirmationCode,0,"No available table right now - added to waiting list");
                }

                // 8) Send confirmation code to user/guest
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

                // 9) Create seating and mark reservation as SEATED
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
     * Check-in flow for an existing reservation (loaded already).
     *
     *
     * @param conn active transaction connection
     * @param r reservation entity
     * @return SeatingResponse (seated/waiting/failure)
     */
    public Response<SeatingResponse> checkInByConfirmationCode(Connection conn,Reservation r) {
        try  {
            if(r==null) return new Response<>(false, "reservation is null error" ,null);
            Integer confirmationCode=r.getConfirmationCode();

            // 1) Validate reservation check-in rules
            String validationMsg= validateReservationForCheckIn(r);

            if (validationMsg!=null) {
                return handleValidationFailure(conn, r, confirmationCode, validationMsg);
            }

            // 2) Try to seat immediately
            Table table = tableDAO.findAvailableTable(conn, r.getAllocatedCapacity());
            if (table != null) {
                System.out.println("Found table,seating the customer now.");
                return seatNow(conn, r, confirmationCode, table);
            }

            // 3) Otherwise -> waiting flow
            return handleNoTableAvailable(conn, r, confirmationCode);

        } catch (Exception e) {
            return new Response<>(false, "Check-in failed: " + e.getMessage(), null);
        }
    }

    /**
     * Checks out the currently seated reservation on a given table:
     *
     * @param conn active transaction connection
     * @param tableID table id to checkout
     * @return true if checkout + reservation update succeeded, false otherwise
     * @throws SQLException
     */
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

    /**
     * Attempts to assign the next waiting customer to a table that just became available.
     *
     *
     * Important:
     * - This method returns boolean only; caller is expected to commit/rollback.
     * - If any step fails, returning false allows caller to rollback so the held seating is removed.
     *
     * @param conn active transaction connection
     * @param tableID table id that became available
     * @return true if no one waiting OR assignment succeeded, false on failure (caller should rollback)
     * @throws SQLException
     */
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
     * Validates if a reservation is eligible for check-in.
     *
     *
     * @param r reservation to validate
     * @return null if ok; otherwise a string token/message describing the failure
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
     *
     * @param conn active JDBC connection (transaction)
     * @param r reservation entity
     * @param confirmationCode reservation confirmation code
     * @return waiting-list response or failure
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
     *
     * Note:
     * - This method commits the transaction on success (as written in your code).
     *
     * @param conn active JDBC connection (transaction)
     * @param r reservation entity
     * @param confirmationCode reservation confirmation code
     * @param table chosen available table
     * @return seated response or failure
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
     * Moves a reservation into the waiting list within the current transaction
     *
     * @param conn active JDBC connection (transaction)
     * @param reservationId reservation id to insert into waiting list
     * @param confirmationCode confirmation code to include in the message
     * @param priority waiting list priority used by your DAO
     * @param msg success message returned to the client
     * @return CUSTOMER_IN_WAITINGLIST response on success; failure otherwise
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

        // Reload reservation so we can decide how to notify (user vs guest)
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
