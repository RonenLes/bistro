package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.*;
import entities.Reservation;
import entities.User;
import requests.TableInfo;
import requests.ManagerRequest;
import responses.CurrentSeatingResponse;
import responses.ManagerResponse;
import responses.ManagerResponse.ManagerResponseCommand;
import responses.Response;

public class ManagementControl {

	private TableDAO tableDAO;
	private SeatingDAO seatingDAO;
	private ReservationDAO reservationDAO;
	private BillDAO billDAO;
	private UserDAO userDAO;
	private UserControl userControl;
	private NotificationControl notificationControl;
	
	
	
	public ManagementControl() {
		this(new TableDAO(),new SeatingDAO(),new ReservationDAO(),new BillDAO(),
				new UserDAO(),new UserControl(),new NotificationControl());
	}
	
	public ManagementControl(TableDAO tableDAO, SeatingDAO seatingDAO, ReservationDAO reservationDAO, BillDAO billDAO,
			UserDAO userDAO, UserControl userControl, NotificationControl notificationControl) {
		this.tableDAO = tableDAO;
		this.seatingDAO = seatingDAO;
		this.reservationDAO = reservationDAO;
		this.billDAO = billDAO;
		this.userDAO = userDAO;
		this.userControl = userControl;
		this.notificationControl = notificationControl;
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
		};
	}
	

	/**
	 * method to add a new user to the data base
	 * @param req request contains username password phone and email
	 * @return response 
	 */
	public Response<ManagerResponse> addNewUser(ManagerRequest req){
		
		try {
			String newID = userControl.generateUserID();
			if(userDAO.insertNewUser(newID, req.getNewUsername(), req.getPassword(), "SUBSCRIBER", req.getPhone(), req.getEmail())) {
				ManagerResponse resp = new ManagerResponse(newID);
				return new Response<>(true,"Weclome "+req.getNewUsername(),resp);
			}
			return new Response<>(false,"Failed registration",null);
		}catch(SQLException e) {
			System.err.println("NEW REGISTRAION DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "username used or a general db error", null);
		}
	}
	
	/**
	 * method for the manager to view all tables currently in data base
	 * @return
	 */
	public Response<ManagerResponse> getAllTables(){
		List<TableInfo> tables = new ArrayList<>();
		
		try(Connection conn = DBManager.getConnection()){
			tables=tableDAO.fetchAllTables(conn);
			ManagerResponse resp = new ManagerResponse(tables,ManagerResponseCommand.SHOW_ALL_TABLES_RESPONSE);
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
	 * @return
	 */
	public Response<ManagerResponse> editTalbeCap(ManagerRequest req){
		try(Connection conn = DBManager.getConnection()){
			if(!tableDAO.updateTableByTableNumber(conn, req.getTableNumber(), req.getNewCap())) {
				return new Response<>(false,"failed to edit table number: "+req.getTableNumber(),null);
			}
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.EDIT_TABLE_RESPONSE,new TableInfo(req.getTableNumber(),req.getNewCap()));
			return new Response<>(true,"Edit successful",resp);
		}catch(SQLException e) {
			return new Response<>(false,"DB fail to edit table",null);
		}
	}
	
	/**
	 * method to add new table to the data base
	 * @param req contain the capacity for the new table
	 * @return
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
	 * @return
	 */
	public Response<ManagerResponse> getCurrentSeating(){
		List<CurrentSeatingResponse> currentSeatingList = new ArrayList<>();
		try(Connection conn = DBManager.getConnection()){
			currentSeatingList = seatingDAO.fetchCurrentSeating(conn);
			ManagerResponse resp = new ManagerResponse(currentSeatingList,ManagerResponseCommand.VIEW_CURRENT_SEATING_RESPONSE);
			return new Response<>(true,"Current seating",resp);			
		}catch(SQLException e) {
			System.err.println("fetching seating DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "DB fail to fetch seatings", null);
		}
	}
	
	public Response<ManagerResponse> deactivateTableByNumber(int tableNumber) {
	    Connection conn = null;

	    // collect victims in-memory
	    List<Integer> cancelledCodes = new ArrayList<>();
	    List<String> victimContacts = new ArrayList<>();

	    try {
	        conn = DBManager.getConnection();
	        conn.setAutoCommit(false);

	        Integer cap = validateTableCanBeDeactivated(conn, tableNumber);
	        if (cap == null) {
	            conn.rollback();
	            return new Response<>(false, "Table not found (or already inactive)", null);
	        }

	        int newTotal = computeNewTotalAfterDeactivation(conn, cap, tableNumber);
	        if (newTotal == -1) {
	            conn.rollback();
	            // message already decided inside helper via exception
	            return new Response<>(false, "Cannot deactivate the last active table of capacity " + cap, null);
	        }

	        cancelVictimsForOverbookedSlots(conn, cap, newTotal, cancelledCodes, victimContacts);

	        if (!deactivateTable(conn, tableNumber)) {
	            conn.rollback();
	            return new Response<>(false, "Failed to deactivate table", null);
	        }

	        conn.commit();

	    } catch (Exception e) {
	        safeRollback(conn);
	        return new Response<>(false, "DB error: " + e.getMessage(), null);
	    } finally {closeQuietly(conn);}
	        
	    
	    // notify AFTER commit
	    notifyVictims(victimContacts);

	    ManagerResponse mr = new ManagerResponse(victimContacts, ManagerResponse.ManagerResponseCommand.DELETED_TABLE_RESPONSE);
	    return new Response<>(true,
	            "Table deactivated. Cancelled " + cancelledCodes.size() + " reservation(s).",mr);	            
	}

	
	
	
	/**
	 * 
	 * @param conn
	 * @param tableNumber
	 * @return table capacity if exists+active
	 * @throws SQLException
	 */
	private Integer validateTableCanBeDeactivated(Connection conn, int tableNumber) throws SQLException {
	    Integer cap = tableDAO.getCapacityByTableNumber(conn, tableNumber);
	    if (cap == null) return null;

	    if (tableDAO.isTableOccupiedNow(conn, tableNumber)) {
	        throw new IllegalStateException("Table is occupied right now");
	    }
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
	 * @param cancelledCodesOut
	 * @param victimContactsOut
	 * @throws SQLException
	 */
	private void cancelVictimsForOverbookedSlots(Connection conn,int cap,int newTotal,List<Integer> cancelledCodesOut,
	        List<String> victimContactsOut) throws SQLException {
	        	        	        	        	
	    List<ReservationDAO.SlotOverbook> overbooked =reservationDAO.findOverbookedSlots(conn, cap, newTotal);
	            
	    for (ReservationDAO.SlotOverbook slot : overbooked) {
	        int toCancel = slot.getBooked() - newTotal;
	        if (toCancel <= 0) continue;

	        List<Integer> victims = reservationDAO.pickConfirmationCodesToCancel(conn, slot.getDate(), slot.getSlotStart(), cap, toCancel);
	                	        
	        if (victims == null || victims.isEmpty()) continue;

	        reservationDAO.cancelReservationsByConfirmationCodes(conn, victims);
	        cancelledCodesOut.addAll(victims);

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
	private void collectVictimContacts(Connection conn, List<Integer> confirmationCodes, List<String> contactsOut)
	        throws SQLException {

	    for (Integer code : confirmationCodes) {
	        if (code == null) continue;

	        Reservation r = reservationDAO.getReservationByConfirmationCode(conn, code);
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

	
	private boolean deactivateTable(Connection conn, int tableNumber) throws SQLException {
	    return tableDAO.deactivateTableByNumber(conn, tableNumber);
	}

	
	private void notifyVictims(List<String> victimContacts) {
	    for (String contact : victimContacts) {
	        try {
	            notificationControl.sendCancelledReservation(
	                    contact,
	                    "Your reservation was cancelled due to table maintenance."
	            );
	        } catch (Exception ignore) {}
	    }
	}

	
	
	private void safeRollback(Connection conn) {
	    try {
	        if (conn != null) conn.rollback();
	    } catch (Exception ignore) {}
	}

	private void closeQuietly(Connection conn) {
	    try {
	        if (conn != null) conn.close();
	    } catch (Exception ignore) {}
	}
}
