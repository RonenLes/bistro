package database;

import java.sql.*;
/**
 * Data Access Object (DAO) for storing and retrieving monthly reports from the {@code reports} table.
 *
 * <p>This DAO persists reports as raw byte arrays (typically produced by a serializer such as Kryo),
 * keyed by:
 * <ul>
 *   <li>{@code reportType} (e.g., {@code "VISITOR_REPORT"} or {@code "SUBSCRIBER_REPORT"})</li>
 *   <li>{@code reportMonth} (usually {@code YearMonth.toString()}, like {@code "2026-01"})</li>
 * </ul>
 *
 * <p><b>Important:</b> This class does not validate or interpret the payload.
 * Validation/deserialization should be done at the controller/service layer.
 *
 * <p><b>Schema assumptions:</b>
 * <ul>
 *   <li>There is a table named {@code reports}.</li>
 *   <li>Columns include: {@code reportType}, {@code reportMonth}, {@code payload}, {@code createdAt}.</li>
 *   <li>There is a UNIQUE constraint (or PRIMARY KEY) on ({@code reportType}, {@code reportMonth})
 *       to support the UPSERT.</li>
 *   <li>{@code payload} is a BLOB/LONGBLOB column.</li>
 * </ul>
 */
public class ReportDAO {

    private static final String UPSERT_REPORT_BYTES =
            "INSERT INTO reports (reportType, reportMonth, payload, createdAt) " +
            "VALUES (?, ?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE payload = VALUES(payload), createdAt = NOW()";

    private static final String EXISTS_REPORT = "SELECT 1 FROM reports WHERE reportType = ? AND reportMonth = ? LIMIT 1";
    private static final String SELECT_REPORT_BYTES = "SELECT payload FROM reports WHERE reportType = ? AND reportMonth = ?";
    
    
    
    /**
     * Inserts or updates (UPSERT) a report payload.
     *
     * <p>If a report row does not exist, inserts it.
     * If it already exists (based on UNIQUE key), updates {@code payload} and sets {@code createdAt = NOW()}.
     *
     * @param conn active JDBC connection (must not be null)
     * @param reportType logical report type key (e.g. {@code "VISITOR_REPORT"})
     * @param reportMonth month key (e.g. {@code "2026-01"})
     * @param payload serialized report payload bytes (BLOB)
     * @return {@code true} if at least one row was inserted/updated; {@code false} otherwise
     * @throws SQLException if a DB error occurs
     */
    public boolean upsertReportBytes(Connection conn, String reportType, String reportMonth, byte[] payload)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(UPSERT_REPORT_BYTES)) {
            ps.setString(1, reportType);
            ps.setString(2, reportMonth);
            ps.setBytes(3, payload);
            return ps.executeUpdate() >= 1;
        }
    }
    
    
    /**
     * Checks whether a report row exists for the given (reportType, reportMonth) key.
     *
     * @param conn active JDBC connection (must not be null)
     * @param reportType logical report type key
     * @param reportMonth month key (e.g. {@code "2026-01"})
     * @return {@code true} if a row exists; {@code false} otherwise
     * @throws SQLException if a DB error occurs
     */
    public boolean reportExists(Connection conn, String reportType, String reportMonth) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_REPORT)) {
            ps.setString(1, reportType);
            ps.setString(2, reportMonth);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Fetches the stored payload bytes for a report.
     *
     * @param conn active JDBC connection; if null, this method returns {@code null}
     * @param reportType logical report type key
     * @param reportMonth month key (e.g. {@code "2026-01"})
     * @return the raw payload bytes if found; {@code null} if {@code conn} is null or no matching row exists
     * @throws SQLException if a DB error occurs
     */
    public byte[] getReportBytes(Connection conn,String reportType,String reportMonth) throws SQLException {

    	if (conn == null) return null;

    	try (PreparedStatement ps = conn.prepareStatement(SELECT_REPORT_BYTES)) {
    		ps.setString(1, reportType);
    		ps.setString(2, reportMonth);

    		try (ResultSet rs = ps.executeQuery()) {
    			if (!rs.next()) {
    				return null; // report not found
    			}
    			return rs.getBytes("payload"); // LONGBLOB → byte[]
    		}
    	}
    }
}
