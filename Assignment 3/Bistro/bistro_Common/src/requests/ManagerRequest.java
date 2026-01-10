package requests;

import java.time.LocalDate;
import java.time.LocalTime;

public class ManagerRequest {
	private ManagerCommand managerCommand;
	
	//for new user
	private String newUsername;
	private String password;
	private String phone;
	private String email;
	
	//for new table/edit table/delete
	private int tableNumber;
	private int newCap;
	
	//for opening hours edit
	private LocalDate newDate;//also to be used for fetching opening hours by date
	private LocalTime newOpenTime;
	private LocalTime newCloseTime;
	private String occasion;

	public enum ManagerCommand{
		ADD_NEW_USER,
		VIEW_ALL_TABLES,
		EDIT_TABLES,
		ADD_NEW_TABLE,
		VIEW_CURRENT_SEATING,
		DELETE_TABLE,
		EDIT_OPENING_HOURS,
		VIEW_ALL_OPENING_HOURS,
		VIEW_WAITING_LIST,
		VIEW_SUBSCRIBERS,
		VIEW_RESERVATIONS
	}

	public ManagerRequest() {}
	
	public ManagerRequest(ManagerCommand managerCommand) {
		this.managerCommand = managerCommand;
	}
	
	public ManagerRequest(ManagerCommand managerCommand,LocalDate date) {
		this.managerCommand = managerCommand;
		this.newDate = date;
	}
	
	//for updating opening hours
	public ManagerRequest(ManagerCommand managerCommand, LocalDate date, LocalTime openTime, LocalTime closeTime, String occasion) {
		this.managerCommand = managerCommand;
		this.newDate = date;
		this.newOpenTime = openTime;
		this.newCloseTime = closeTime;
		this.occasion = occasion;
	}
	
	//for adding new user
	public ManagerRequest(ManagerCommand managerCommand, String newUsername, String password, String phone,
			String email) {
		this.managerCommand = managerCommand;
		this.newUsername = newUsername;
		this.password = password;
		this.phone = phone;
		this.email = email;
	}
	
	////for deleting table by number
	public ManagerRequest(ManagerCommand managerCommand,int tableNumber) {
		this.managerCommand = managerCommand;
		this.tableNumber=tableNumber;
	}
	
	//for editing table details
	public ManagerRequest(ManagerCommand managerCommand,int tableNumber,int newCap) {
		this.managerCommand = managerCommand;
		this.tableNumber = tableNumber;
		this.newCap = newCap;
	}

	public ManagerCommand getManagerCommand() {
		return managerCommand;
	}

	public String getNewUsername() {
		return newUsername;
	}

	public String getPassword() {
		return password;
	}

	public String getPhone() {
		return phone;
	}

	public String getEmail() {
		return email;
	}

	public int getTableNumber() {
		return tableNumber;
	}
	
	public int getNewCap() {
		return newCap;
	}

	public LocalDate getNewDate() {
		return newDate;
	}

	public LocalTime getNewOpenTime() {
		return newOpenTime;
	}

	public LocalTime getNewCloseTime() {
		return newCloseTime;
	}

	public String getOccasion() {
		return occasion;
	}
	
	
	
	
}
