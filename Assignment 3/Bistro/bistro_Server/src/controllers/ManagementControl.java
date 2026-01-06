package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.*;
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
		case DELETE_TABLE -> deleteTableByNumber(req.getTableNumber());
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
	
	/**
	 * method to delete an existing table with checking if occupied 
	 * @param tableNumber that the manager wants to delete
	 * @return
	 */
	public Response<ManagerResponse> deleteTableByNumber(int tableNumber){
		try(Connection conn = DBManager.getConnection()){
			conn.setAutoCommit(false);
			
			boolean deleted = tableDAO.deleteTableByNumberIfNotOccupied(conn, tableNumber);
			if(!deleted) {
				 conn.rollback();
				return new Response<>(false, "Table not deleted (not found or occupied)", null);
			}
			
			TableInfo ti = new TableInfo(tableNumber,-1);
			ManagerResponse resp = new ManagerResponse(ManagerResponseCommand.DELETED_TABLE_RESPONSE,ti);
			return new Response<>(true,"Table #" + tableNumber + " deleted successfully",resp);
			
			
		}catch(SQLException e) {
			System.out.println("delete table DB ERROR: " + e.getMessage());
			return new Response<>(false, "DB fail to delete table", null);
		}
	}
}
