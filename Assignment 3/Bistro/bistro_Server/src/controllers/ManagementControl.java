package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import database.*;
import entities.OpeningHours;
import entities.Reservation;
import entities.User;
import entities.WaitingList;
import requests.TableInfo;
import requests.ManagerRequest;
import responses.CurrentOpeningHoursResponse;
import responses.CurrentSeatingResponse;
import responses.LoginResponse;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.ReservationResponse;
import responses.Response;
import responses.WaitingListResponse;
/**
 * class controller to handle all the requests related to representitve and manager(without reports)
 */
public class ManagementControl {

	private TableDAO tableDAO;
	private SeatingDAO seatingDAO;
	private ReservationDAO reservationDAO;
	private BillDAO billDAO;
	private UserDAO userDAO;
	private UserControl userControl;
	private NotificationControl notificationControl;
	private WaitingListDAO waitingListDAO;
	private BillingControl billingControl;
	private OpeningHoursDAO openingHoursDAO;
	
	
	
	public ManagementControl() {
		this(new TableDAO(),new SeatingDAO(),new ReservationDAO(),new BillDAO(),
				new UserDAO(),new UserControl(),new NotificationControl(),new WaitingListDAO(),new BillingControl(),new OpeningHoursDAO());
	}
	
	public ManagementControl(TableDAO tableDAO, SeatingDAO seatingDAO, ReservationDAO reservationDAO, BillDAO billDAO,
			UserDAO userDAO, UserControl userControl, NotificationControl notificationControl,WaitingListDAO waitingListDAO,BillingControl billingControl,OpeningHoursDAO openingHoursDAO) {
		this.tableDAO = tableDAO;
		this.seatingDAO = seatingDAO;
		this.reservationDAO = reservationDAO;
		this.billDAO = billDAO;
		this.userDAO = userDAO;
		this.userControl = userControl;
		this.notificationControl = notificationControl;
		this.waitingListDAO = waitingListDAO;
		this.billingControl = billingControl;
		this.openingHoursDAO=openingHoursDAO;
	}



	public Response<ManagerResponse> handleManagerRequest(ManagerRequest req){
		if(req == null) return new Response<>(false,"failed to get manager request",null);
		
		return switch(req.getManagerCommand()) {
		case ADD_NEW_USER -> addNewUser(req);
		case VIEW_ALL_TABLES -> getAllTables();
		case ADD_NEW_TABLE -> addNewTable(req);
		case EDIT_TABLES -> editTalbeCap(req);
		case DELETE_TABLE -> deactivateTableByNumber(req.getTableNumber());
		case VIEW_CURRENT_SEATING -> getCurrentSeating();
		case EDIT_OPENING_HOURS -> editOpenHours(req);
		case VIEW_ALL_OPENING_HOURS -> viewOpeningHoursNext30Days(req.getNewDate());
		case VIEW_WAITING_LIST -> viewCurrentWaitingList();
		case VIEW_SUBSCRIBERS -> viewAllSubscriber();
		case VIEW_RESERVATIONS -> viewReservationByDate(req.getNewDate());
		};
	}
	

	/**
	 * method to add a new user to the data base
	 * @param req request contains username password phone and email
	 * @return Response<ManagerResponse> with new userID (qrcode) 
	 */
	public Response<ManagerResponse> addNewUser(ManagerRequest req){
		
		try {
			String requestedRole = req.getRole();
			String role = normalizeRole(requestedRole);
			String newID = userControl.generateUserID();
			if(userDAO.insertNewUser(newID, req.getNewUsername(), req.getPassword(), role, req.getPhone(), req.getEmail())) {
				ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.NEW_USER_RESPONSE,newID);
				return new Response<>(true,"Weclome "+req.getNewUsername(),resp);
			}
			return new Response<>(false,"Failed registration",null);
		}catch(SQLException e) {
			System.err.println("NEW REGISTRAION DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "username used or a general db error", null);
		}
	}
	
	private String normalizeRole(String roleRaw) {
		if (roleRaw == null) return "SUBSCRIBER";
		String role = roleRaw.trim().toUpperCase();
		return switch (role) {
			case "SUBSCRIBER", "REPRESENTATIVE", "MANAGER" -> role;
			default -> "SUBSCRIBER";
		};
	}
	
	/**
	 * method for the manager to view all tables currently in data base
	 * @return Response<ManagerResponse> with list of tables currently at available
	 */
	public Response<ManagerResponse> getAllTables(){
		List<TableInfo> tables = new ArrayList<>();
		
		try(Connection conn = DBManager.getConnection()){
			tables=tableDAO.fetchAllTables(conn);
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.SHOW_ALL_TABLES_RESPONSE,tables);
			return new Response<>(true,"Restaurant tables",resp);
		}catch(SQLException e) {
			System.err.println("fetching all tables DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "failed fetching all tables", null);
		}
	}
	
	/**
	 * method to edit existing table
	 * @param req contains the table number to edit and the new capacity
	 * @return Response<ManagerResponse> with updated table info as an object and if victim contacts whos reservations was cancelled 
	 */
	public Response<ManagerResponse> editTalbeCap(ManagerRequest req){
		Connection conn = null;
		List<Reservation> cancelledReservation = new ArrayList<>();
		List<String> victimContacts = new ArrayList<>();
		
		try {
			conn = DBManager.getConnection();
			conn.setAutoCommit(false);

			Integer currentCap = tableDAO.getCapacityByTableNumber(conn, req.getTableNumber());
			if (currentCap == null) {
				conn.rollback();
				System.out.println("table not found for number: " + req.getTableNumber());
				return new Response<>(false, "table not found for number: " + req.getTableNumber(), null);
			}
			if (req.getNewCap() == currentCap) {
				conn.rollback();
				System.out.println("we are here");
				ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.EDIT_TABLE_RESPONSE,new TableInfo(req.getTableNumber(), req.getNewCap()),List.of());
																
				return new Response<>(true, "No changes needed (capacity unchanged).", resp);
			}
			if (req.getNewCap() < currentCap) {
				int newTotal = computeNewTotalAfterReduction(conn, currentCap);
				System.out.println(victimContacts+"req.getNewCap");
				cancelVictimsForOverbookedSlots(conn, currentCap, newTotal, cancelledReservation, victimContacts);
			}

			if (!tableDAO.updateTableByTableNumber(conn, req.getTableNumber(), req.getNewCap())) {
				conn.rollback();
				System.out.println("failed to edit table number: ");
				return new Response<>(false, "failed to edit table number: " + req.getTableNumber(), null);
			}

			conn.commit();
		}catch(Exception e) {
			safeRollback(conn,"failed to edit tableCap");
			return new Response<>(false,"DB fail to edit table",null);
		}finally {
			closeQuietly(conn);
		}
		
		notifyVictims(victimContacts);
		
		ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.EDIT_TABLE_RESPONSE,new TableInfo(req.getTableNumber(), req.getNewCap()),victimContacts);												
		String msg = "Edit successful";
		
		if (!cancelledReservation.isEmpty()) {
			msg += ". Cancelled " + cancelledReservation.size() + " reservation(s).";
		}
		
		return new Response<>(true, msg, resp);
	}
	
	/**
	 * method to calculate how much reservations needs to be cancelled
	 * @param conn
	 * @param cap
	 * @return
	 * @throws SQLException
	 */
	private int computeNewTotalAfterReduction(Connection conn, int cap) throws SQLException {
	    int totalActive = tableDAO.countActiveTablesByCapacity(conn, cap);
	    return Math.max(totalActive - 1, 0);
	}
	
	/**
	 * method to add new table to the data base
	 * @param req contain the capacity for the new table
	 * @return Response<ManagerResponse> with TableInfo object containing the new added table details
	 */
	public Response<ManagerResponse> addNewTable(ManagerRequest req){
		try(Connection conn  = DBManager.getConnection()){
			
			if(!tableDAO.insertNewTable(conn, req.getTableNumber(), req.getNewCap())){
				return new Response<>(false, "Table number already exists", null);
			}
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.NEW_TABLE_RESPONSE,new TableInfo(req.getTableNumber(),req.getNewCap()));
			return new Response<>(true,"Table Number: "+req.getTableNumber()+"was added",resp);
						
		}catch(SQLException e) {
			System.err.println("fetching all tables DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "DB fail to add new table", null);
		}
	}
	
	/**
	 * method for the manager to view all current seating in the restaurant:
	 * userID, username, checkInTime, estimated checkout time , partySize, table number
	 * @return Response<ManagerResponse> with a list of CurrentSeatingResponse objects containing current seating customers details
	 */
	public Response<ManagerResponse> getCurrentSeating(){
		List<CurrentSeatingResponse> currentSeatingList = new ArrayList<>();
		try(Connection conn = DBManager.getConnection()){
			currentSeatingList = seatingDAO.fetchCurrentSeating(conn);
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.VIEW_CURRENT_SEATING_RESPONSE,currentSeatingList);
			return new Response<>(true,"Current seating",resp);			
		}catch(SQLException e) {
			System.err.println("fetching seating DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "DB fail to fetch seatings", null);
		}
	}
	
	
	/**
	 * method to set disable a table (delete)
	 * @param tableNumber the number of the table to disable
	 * @return the list of contacts that were affected (cancelled reservation)
	 */
	public Response<ManagerResponse> deactivateTableByNumber(int tableNumber) {
		System.out.println("[DEACTIVATE] start table=" + tableNumber);
		Connection conn = null;

	    // collect victims in-memory
	    List<Reservation> cancelledReservation = new ArrayList<>();
	    List<String> victimContacts = new ArrayList<>();
	    

	    try {
	        conn = DBManager.getConnection();
	        conn.setAutoCommit(false);
	        
	        Integer cap = validateTableCanBeDeactivated(conn, tableNumber);
	        if (cap == null) safeRollback(conn, "Table not found (or already inactive)");
	        	        		            
	        int newTotal = computeNewTotalAfterDeactivation(conn, cap, tableNumber);
	        if (newTotal == -1) safeRollback(conn, "Cannot deactivate the last active table of capacity " + cap);
	        	        	           
	        cancelVictimsForOverbookedSlots(conn, cap, newTotal, cancelledReservation, victimContacts);
	        
	        System.out.println("[DEACTIVATE] deactivating table...");
	        if (!deactivateTable(conn, tableNumber)) safeRollback(conn, "Failed to deactivate table");
	            

	        conn.commit();

	    } catch (Exception e) {	    	
	        e.printStackTrace();
	        safeRollback(conn, "DB error: " + e.getMessage());
	        return new Response<>(false, "Deactivate failed: " + e.getMessage(), null);	        
	    } finally {closeQuietly(conn);}
	        
	    
	    // notify AFTER commit
	    notifyVictims(victimContacts);

	    ManagerResponse mr = new ManagerResponse(ManagerResponseCommand.DELETED_TABLE_RESPONSE,victimContacts);
	    
	    return new Response<>(true,"Table deactivated. Cancelled " + cancelledReservation.size() + " reservation(s).",mr);	   
	                     
	}

	
	
	
	/**
	 * method to check if the to delete is now seated 
	 * @param conn
	 * @param tableNumber
	 * @return table capacity if exists+active
	 * @throws SQLException
	 */
	private Integer validateTableCanBeDeactivated(Connection conn, int tableNumber) throws SQLException {
	    Integer cap = tableDAO.getCapacityByTableNumber(conn, tableNumber);
	    if (cap == null) return null;
	    if (tableDAO.isTableOccupiedNow(conn, tableNumber)) throw new IllegalStateException("Table is occupied right now");
	    
	    return cap;
	}

	
	/**
	 * 
	 * @param conn
	 * @param cap
	 * @param tableNumber
	 * @return Returns newTotal (after deactivation). If cannot deactivate -> return -1
	 * @throws SQLException
	 */
	private int computeNewTotalAfterDeactivation(Connection conn, int cap, int tableNumber) throws SQLException {
	    int totalActive = tableDAO.countActiveTablesByCapacity(conn, cap);
	    if (totalActive <= 1) return -1; // last one -> cannot deactivate
	    return totalActive - 1;
	}

	
	/**
	 * Cancels victims for all overbooked slots and collects cancelled codes + contacts (no notifications here)
	 * @param conn
	 * @param cap
	 * @param newTotal
	 * @param reservations
	 * @param victimContactsOut
	 * @throws SQLException
	 */
	private void cancelVictimsForOverbookedSlots(Connection conn,int cap,int newTotal,List<Reservation> reservations,
	        List<String> victimContactsOut) throws SQLException {
	        	        	        	        	
	    List<ReservationDAO.SlotOverbook> overbooked =reservationDAO.findOverbookedSlots(conn, cap, newTotal);
	    var slots = reservationDAO.findOverbookedSlots(conn, cap, newTotal);
	    System.out.println("cap=" + cap + " newTotal=" + newTotal + " overbookedSlots=" + slots.size());
	    for (var s : slots) {
	        System.out.println(s.getDate() + " " + s.getSlotStart() + " booked=" + s.getBooked());
	    }

	            
	    for (ReservationDAO.SlotOverbook slot : overbooked) {
	        int toCancel = slot.getBooked() - newTotal;
	        if (toCancel <= 0) continue;

	        List<Reservation> victims = reservationDAO.pickReservationToCancelDueToTable(conn, slot.getDate(), slot.getSlotStart(), cap, toCancel);
	                	        
	        if (victims == null || victims.isEmpty()) continue;

	        reservationDAO.cancelReservationsByReservationID(conn, victims);
	        reservations.addAll(victims);

	        collectVictimContacts(conn, victims, victimContactsOut);
	    }
	}

	
	/**
	 * Pulls email/guest contact for each cancelled confirmation code using THE SAME conn
	 * @param conn
	 * @param confirmationCodes
	 * @param contactsOut
	 * @throws SQLException
	 */
	private void collectVictimContacts(Connection conn, List<Reservation> reservation,List<String> contactsOut)
	        throws SQLException {

	    for (Reservation r : reservation) {
	        if (r == null) continue;
	        
	        String contact;

	        if (r.getUserID() == null || r.getUserID().isBlank()) {
	            contact = r.getGuestContact();
	        } else {
	            User u = userDAO.getUserByUserID(conn, r.getUserID());
	            contact = (u != null ? u.getEmail() : null);
	        }

	        if (contact != null && !contact.isBlank()) {
	            contactsOut.add(contact);
	        }
	    }
	}

	/**
	 * tell the DAO to update the table isActive to 0
	 * @param conn
	 * @param tableNumber
	 * @return
	 * @throws SQLException
	 */
	private boolean deactivateTable(Connection conn, int tableNumber) throws SQLException {
	    return tableDAO.deactivateTableByNumber(conn, tableNumber);
	}

	
	/**
	 * method to edit the time the restuarant opens and closes and why
	 * @param req the times and occasion
	 * @return list of contacts that were affected by the edit (cancelled reservation)
	 */
	public Response<ManagerResponse> editOpenHours(ManagerRequest req) {

	    Connection conn = null;
	    List<Reservation> reservationsToCancel = new ArrayList<>();
	    List<String> victimsContact = new ArrayList<>();
	    boolean committed = false;

	    try {
	        conn = DBManager.getConnection();
	        conn.setAutoCommit(false);

	        reservationsToCancel = reservationDAO.pickReservationToCancelDueToOpenHours(conn, req.getNewDate(), req.getNewOpenTime(), req.getNewCloseTime());
	        System.out.println("[EDIT HOURS] conflicts=" + (reservationsToCancel == null ? "null" : reservationsToCancel.size()));

	        if (reservationsToCancel == null) reservationsToCancel = List.of(); // treat as empty

	        for (Reservation r : reservationsToCancel) {

	            if (isWaiting(r.getStatus())) {
	                boolean wL = waitingListDAO.updateWaitingStatus(conn, r.getReservationID(), "CANCELLED");
	                if (!wL) throw new SQLException("Failed to cancel waiting-list record");
	            }

	            if (isSeated(r.getStatus())) {
	                Integer seatingId = seatingDAO.getSeatingIdByReservationId(conn, r.getReservationID());
	                if (seatingId == null) throw new SQLException("Missing seating id for reservationId=" + r.getReservationID());

	                boolean billSent = billingControl.sendBillAutomatically(conn, seatingId);
	                if (!billSent) throw new SQLException("Failed to send bill for seatingId=" + seatingId);
	            }

	            if (!"CANCELLED".equals(r.getStatus())) {
	                boolean statusUpdate = reservationDAO.updateStatusByReservationID(conn, r.getReservationID(), "CANCELLED");
	                if (!statusUpdate) throw new SQLException("Failed to cancel reservationId=" + r.getReservationID());
	            }
	        }

	        boolean updated = openingHoursDAO.updateOpeningHours(conn, req.getNewOpenTime(), req.getNewCloseTime(), req.getNewDate(), req.getOccasion());
	        if (!updated) throw new SQLException("Failed to update opening hours (0 rows updated?)");

	        collectVictimContacts(conn, reservationsToCancel, victimsContact);

	        conn.commit();
	        committed = true;

	    } catch (Exception e) {
	        safeRollback(conn, "Edit open hours failed: " + e.getMessage());
	        return new Response<>(false, "Edit open hours failed: " + e.getMessage(), null);
	    } finally {
	        closeQuietly(conn);
	    }

	    // notify ONLY if commit succeeded
	    if (committed && !victimsContact.isEmpty()) notifyVictims(victimsContact);
	        
	    

	    ManagerResponse mr = new ManagerResponse(ManagerResponseCommand.EDIT_HOURS_RESPONSE, victimsContact);
	    return new Response<>(true, "Opening hours updated" +
	            (victimsContact.isEmpty() ? "" : " and conflicting reservations were cancelled"), mr);
	}
	
	
	/**
	 * view the next 30 details of the next 30 dates from today
	 * @param date
	 * @return Response<ManagerResponse> a list of CurrentOpeningHoursResponse containing the details of each opening
	 */
	public Response<ManagerResponse> viewOpeningHoursNext30Days(LocalDate date){
		List<CurrentOpeningHoursResponse> openings = new ArrayList<>();

	    try (Connection conn = DBManager.getConnection()) {

	        List<OpeningHours> list = openingHoursDAO.fetchOpeningHoursNext30Days(conn, date);
	        if (list == null) return new Response<>(false, "Can't find opening hours", null);
	            	        
	        for (OpeningHours oh : list) {
	            if (oh == null) continue;
	            openings.add(new CurrentOpeningHoursResponse(oh.getDate(), oh.getDay(), oh.getOpenTime(), oh.getCloseTime(), oh.getOccasion() ));	                    	           
	        }

	        ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.ALL_OPENING_HOURS_RESPONSE,openings);
	        return new Response<>(true, "Here are the opening hours for the next 30 days", resp);

	    } catch (Exception e) {
	    	System.err.println("failed to view opening hours in manager control");
	        return new Response<>(false, "DB error: " + e.getMessage(), null);
	    }
	}
	
	/**
	 * view current waiting list for the bistro 
	 * @return Response<ManagerResponse> with a list of WaitingListResponse containing details about the customer
	 */
	public Response<ManagerResponse> viewCurrentWaitingList(){
		List<WaitingListResponse> currWaiting = new ArrayList<>();
		try(Connection conn = DBManager.getConnection()){
			for(WaitingList w :  waitingListDAO.fetchWaitingListByCurrentDate(conn)) {
				
				if(w==null) return new Response<>(false, "Can't find waiting list", null);
				String contact;
				
				Reservation r = reservationDAO.getReservationByReservationID(conn, w.getReservationID());
				
				if(r.getUserID()==null) contact = r.getGuestContact();
				else contact = userDAO.getUserByUserID(conn, r.getUserID()).getEmail();	
				
				WaitingListResponse currW = new WaitingListResponse(convertPriority(w.getPriority()), w.getCreatedAt(), contact);
				currWaiting.add(currW);
			}
			
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.CURRENT_WAITING_LIST_RESPONSE,currWaiting);
			return new Response<>(true,"Current customer in the waiting list",resp);
			
		}catch(Exception e) {
			System.err.println("failed to view waiting list hours in manager control");
	        return new Response<>(false, "DB error: " + e.getMessage(), null);
		}
		
	}
	
	/**
	 * method to view all the reservations for a specific date
	 * @param date
	 * @return Response<ManagerResponse> with List<ReservationResponse> cotaining the reservation details
	 */
	public Response<ManagerResponse> viewReservationByDate(LocalDate date){
		List<ReservationResponse> currRes = new ArrayList<>();
		try(Connection conn = DBManager.getConnection()){
			
			for(Reservation r : reservationDAO.fetchReservationsByDate(conn,date)) {
				if(r==null) return new Response<>(false, "Can't find reservation", null);
				
				String contact; 
				Reservation currR = reservationDAO.getReservationByReservationID(conn, r.getReservationID());
				
				if(r.getUserID()==null) contact = currR.getGuestContact();
				else contact = userDAO.getUserByUserID(conn, currR.getUserID()).getUsername();	
				
				ReservationResponse resResp = new ReservationResponse(contact, currR.getStartTime(), currR.getPartySize(),currR.getConfirmationCode());
				currRes.add(resResp);
			}
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.RESERVATION_BY_DATE_RESPONSE,currRes);
			return new Response<>(true,"Current customer in the waiting list",resp);
		}catch(Exception e) {
			System.err.println("failed to view reservations hours in manager control");
	        return new Response<>(false, "DB error: " + e.getMessage(), null);
		}
		
	}
	
	/**
	 * view all subscriber in the system 
	 * @return Response<ManagerResponse> with List<LoginResponse> containing subscriber details
	 */
	public Response<ManagerResponse> viewAllSubscriber(){
		List<LoginResponse> subs = new ArrayList<>();
		try(Connection conn = DBManager.getConnection()){
			for(User u : userDAO.fetchAllUsers(conn)) {
				if(u==null) return new Response<>(false, "Can't find reservation", null);
				
				LoginResponse log = new LoginResponse(u.getUserID(),u.getUsername(),u.getEmail(),u.getPhone());
				subs.add(log);
			}
			
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.ALL_SUBSCRIBERS_RESPONSE,subs);
			return new Response<>(true,"Current customer in the waiting list",resp);
			
		}catch(Exception e) {
			System.err.println("failed to view subscribers in manager control");
	        return new Response<>(false, "DB error: " + e.getMessage(), null);
		}
	}
	
	
	//---------------------------------------------SMALL HELPER METHODS_------------------------------------------------------------------
	
	private String convertPriority(int priority) {
		if(priority ==1 ) return "KING";
		return "PEASANT";
	}
	
	private boolean isSeated(String status) {
		return status.equals("SEATED");
	}
	
	private boolean isWaiting(String status) {
		return status.equals("WAITING");
	}
	
	
	/**
	 * send email/sms to victims about cancelling their reservation
	 * @param victimContacts
	 */
	private void notifyVictims(List<String> victimContacts) {
	    for (String contact : victimContacts) {
	        try {
	            notificationControl.sendCancelledReservation(contact,"Your reservation was cancelled due to table maintenance.");
	                    	                    	            
	        } catch (Exception ignore) {}
	    }
	}
	
	
		
	
	private Response<ManagerResponse> safeRollback(Connection conn,String msg) {
		try {
	        if (conn != null) conn.rollback();
	        return new Response<>(false, msg, null);
	    } catch (Exception e) {
	    	System.out.println("[DEACTIVATE] EXCEPTION: " + e);
	        e.printStackTrace();
	        safeRollback(conn, "DB error: " + e.getMessage());
	        return new Response<>(false, msg + " (rollback failed: " + e.getMessage() + ")", null);
	    }
	}

	private void closeQuietly(Connection conn) {
	    try {
	        if (conn != null) conn.close();
	    } catch (Exception ignore) {}
	}
}
