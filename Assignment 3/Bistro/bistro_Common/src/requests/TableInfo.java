package requests;

public class TableInfo {
	
	private int tableNumber;
	private int capacity;
	
	public TableInfo() {}
	
	public TableInfo(int tableNumber, int capacity) {
		
		this.tableNumber = tableNumber;
		this.capacity = capacity;
	}
	
	public int getTableNumber() {
		return tableNumber;
	}
	public int getCapacity() {
		return capacity;
	}
}
