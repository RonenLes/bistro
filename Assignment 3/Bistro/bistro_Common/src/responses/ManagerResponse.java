package responses;

import java.util.List;

import requests.TableInfo;

public class ManagerResponse {
	
	private ManagerResponseCommand responseCommand;

	//for new user to show
	private String userID; //(qr-code) will try this for now instead of qr-code
	
	private TableInfo table;
	
	private List<TableInfo> tables;
	
	
	
	public enum ManagerResponseCommand{
		NEW_USER_RESPONSE,
		SHOW_ALL_TABLES_RESPONSE,
		EDIT_TABLE_RESPONSE,
		NEW_TABLE_RESPONSE
	}

	public ManagerResponse() {}
	
	
	//for adding new user
	public ManagerResponse(String userID) {
		this.responseCommand = ManagerResponseCommand.NEW_USER_RESPONSE;
		this.userID = userID;
	}
	
	
	//for showing all tables
	public ManagerResponse(List<TableInfo> tables) {
		this.responseCommand =ManagerResponseCommand.SHOW_ALL_TABLES_RESPONSE;
		this.tables = tables;
	}
	
	
	public ManagerResponse(ManagerResponseCommand cmd,TableInfo table) {
		this.responseCommand = cmd;
		this.table =table;
	}
	

	public TableInfo getTable() {
		return table;
	}

	public List<TableInfo> getTables() {
		return tables;
	}

	public ManagerResponseCommand getResponseCommand() {
		return responseCommand;
	}

	public String getUserID() {
		return userID;
	}
	
	
	
}
