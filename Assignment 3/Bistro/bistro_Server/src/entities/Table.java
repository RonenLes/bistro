package entities;

public class Table {
	
	private int tableID;
	private int tableNumber;
	private int capacity;
	
	public Table(int tableID, int tableNumber, int capacity) {
		this.tableID = tableID;
		this.tableNumber = tableNumber;
		this.capacity = capacity;
	}
	public int getTableID() {
		return tableID;
	}
	public int getTableNumber() {
		return tableNumber;
	}
	public int getCapacity() {
		return capacity;
	}
	
	
}
