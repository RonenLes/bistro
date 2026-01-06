package controllers;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import database.*;
import requests.TableInfo;
import requests.ManagerRequest;
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
	
	
	public Response<ManagerResponse> getAllTables(){
		List<TableInfo> tables = new ArrayList<>();
		
		try(Connection conn = DBManager.getConnection()){
			tables=tableDAO.fetchAllTables(conn);
			ManagerResponse resp = new ManagerResponse(tables);
			return new Response<>(true,"Restaurant tables",resp);
		}catch(SQLException e) {
			System.err.println("fetching all tables DB ERROR: " + e.getMessage());
    		e.printStackTrace();
    		return new Response<>(false, "failed fetching all tables", null);
		}
	}
	
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
	
}
