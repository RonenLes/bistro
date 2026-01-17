package requests;


/**
 * Lightweight DTO that describes a restaurant table by number and capacity.
 *
 * <p>Main idea:
 * Used in requests/responses where you want to show table details without sending the full
 * {@code Table} entity.</p>
 *
 * <p>Main fields:
 * <ul>
 *   <li>{@code tableNumber} - human-readable table number</li>
 *   <li>{@code capacity} - number of guests the table can seat</li>
 * </ul>
 */
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
