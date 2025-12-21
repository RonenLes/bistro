package dao_stubs;

import database.TableDAO;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TableDAOStub extends TableDAO {

    private final Map<Integer, Integer> totals = new HashMap<>();

    // Optional override: partySize -> minimalCapacity
    private final Map<Integer, Integer> minimalOverride = new HashMap<>();

    private boolean throwOnMinimal = false;

    /** Set how many tables exist for a given capacity (2,4,6,8...). */
    public void setCapacityCount(int capacity, int count) {
        totals.put(capacity, count);
    }

    /** Force the minimal capacity result for a specific partySize (used by tests). */
    public void setMinimalTableSizeFor(int partySize, int allocatedCapacity) {
        minimalOverride.put(partySize, allocatedCapacity);
    }

    /** Simulate DAO failure in getMinimalTableSize (used to trigger "Party too large"). */
    public void throwOnMinimalTableSize(boolean value) {
        this.throwOnMinimal = value;
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
        if (throwOnMinimal) {
            throw new SQLException("Forced failure (no suitable table size)");
        }

        Integer forced = minimalOverride.get(partySize);
        if (forced != null) return forced;

        return totals.keySet().stream()
                .filter(cap -> cap >= partySize && totals.getOrDefault(cap, 0) > 0)
                .min(Integer::compareTo)
                .orElseThrow(() -> new SQLException("No suitable table size"));
    }
}
