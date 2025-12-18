package entities;

public class Table {
	
	private String tableID;
	private int tableNumber;
	private int capacity;
	
	public Table(String tableID, int tableNumber, int capacity) {
		this.tableID = tableID;
		this.tableNumber = tableNumber;
		this.capacity = capacity;
	}
	public String getTableID() {
		return tableID;
	}
	public int getTableNumber() {
		return tableNumber;
	}
	public int getCapacity() {
		return capacity;
	}
	
	
}
