package dao_stubs;

import database.TableDAO;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TableDAOStub extends TableDAO {

    private final Map<Integer, Integer> totals = new HashMap<>();

    /** Set how many tables exist for a given capacity (2,4,6,8...). */
    public void setCapacityCount(int capacity, int count) {
        totals.put(capacity, count);
    }

    @Override
    public Map<Integer, Integer> getTotalTablesByCapacity() throws SQLException {
        return new HashMap<>(totals);
    }

    /**
     * Returns the minimal capacity that can fit partySize (round up).
     * Example: if totals contains {2,4,6} and partySize=3 -> returns 4.
     */
    @Override
    public int getMinimalTableSize(int partySize) throws SQLException {
        return totals.keySet().stream()
                .filter(cap -> cap >= partySize && totals.getOrDefault(cap, 0) > 0)
                .min(Integer::compareTo)
                .orElseThrow(() -> new SQLException("No suitable table size"));
    }
}
