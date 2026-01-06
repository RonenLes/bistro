package requests;

public class ManagerRequest {
	private ManagerCommand managerCommand;
	
	//for new user
	private String newUsername;
	private String password;
	private String phone;
	private String email;
	
	//for new talbe/edit table
	private int tableNumber;
	private int newCap;

	public enum ManagerCommand{
		ADD_NEW_USER,
		VIEW_ALL_TABLES,
		EDIT_TABLES,
		ADD_NEW_TABLE,
		VIEW_CURRENT_SEATING,
		DELETE_TABLE
	}

	public ManagerRequest() {}

	public ManagerRequest(ManagerCommand managerCommand, String newUsername, String password, String phone,
			String email) {
		this.managerCommand = managerCommand;
		this.newUsername = newUsername;
		this.password = password;
		this.phone = phone;
		this.email = email;
	}
	
	public ManagerRequest(ManagerCommand managerCommand,int tableNumber) {
		this.managerCommand = managerCommand;
		this.tableNumber=tableNumber;
	}

	public ManagerRequest(int tableNumber,int newCap,ManagerCommand managerCommand) {
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
	
	
	
	
}
