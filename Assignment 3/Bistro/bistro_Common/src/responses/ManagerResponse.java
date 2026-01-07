package responses;

import java.util.List;

import requests.TableInfo;

public class ManagerResponse {
	
	private ManagerResponseCommand responseCommand;

	//for new user to show
	private String userID; //(qr-code) will try this for now instead of qr-code
	
	private TableInfo table;
	
	private List<?> infoList;
	
	
	public enum ManagerResponseCommand{
		NEW_USER_RESPONSE,
		SHOW_ALL_TABLES_RESPONSE,
		EDIT_TABLE_RESPONSE,
		NEW_TABLE_RESPONSE,
		VIEW_CURRENT_SEATING_RESPONSE,
		DELETED_TABLE_RESPONSE,
	}
	
	public ManagerResponse() {}
	
	public ManagerResponse(List<?> infoList, ManagerResponseCommand cmd) {
		this.responseCommand = cmd;
		this.infoList = infoList;
	}
	
	//for adding new user
	public ManagerResponse(String userID) {
		this.responseCommand = ManagerResponseCommand.NEW_USER_RESPONSE;
		this.userID = userID;
	}
	
			
	public ManagerResponse(ManagerResponseCommand cmd,TableInfo table) {
		this.responseCommand = cmd;
		this.table =table;
	}
	

	public TableInfo getTable() {
		return table;
	}

	public List<?> getTables() {
		return infoList;
	}

	public ManagerResponseCommand getResponseCommand() {
		return responseCommand;
	}

	public String getUserID() {
		return userID;
	}
	
	
	
}
