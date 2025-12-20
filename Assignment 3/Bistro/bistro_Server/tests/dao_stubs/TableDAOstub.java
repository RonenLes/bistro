package dao_stubs;

import java.util.HashMap;
import java.util.Map;

import database.TableDAO;

public class TableDAOstub extends TableDAO {

	
	@Override
    public Map<Integer, Integer> getTotalTablesByCapacity() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(2, 3);
        map.put(4, 2);
        map.put(6, 1);
        return map;
    }
	
	@Override
	public int getMinimalTableSize(int partySize) {
		if(partySize % 2 !=0) return partySize +1;
		return partySize;
	}
	
	@Override
	public boolean insertNewTable(int tableNumber,int capacity) {
		return true;
	}
}
