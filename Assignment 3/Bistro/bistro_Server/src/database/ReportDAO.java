package database;

import java.sql.*;

public class ReportDAO {

    private static final String UPSERT_REPORT_BYTES =
            "INSERT INTO reports (reportType, reportMonth, payload, createdAt) " +
            "VALUES (?, ?, ?, NOW()) " +
            "ON DUPLICATE KEY UPDATE payload = VALUES(payload), createdAt = NOW()";

    private static final String EXISTS_REPORT =
            "SELECT 1 FROM reports WHERE reportType = ? AND reportMonth = ? LIMIT 1";
    private static final String SELECT_REPORT_BYTES =
            "SELECT payload FROM reports WHERE reportType = ? AND reportMonth = ?";
    public boolean upsertReportBytes(Connection conn, String reportType, String reportMonth, byte[] payload)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(UPSERT_REPORT_BYTES)) {
            ps.setString(1, reportType);
            ps.setString(2, reportMonth);
            ps.setBytes(3, payload);
            return ps.executeUpdate() >= 1;
        }
    }

    public boolean reportExists(Connection conn, String reportType, String reportMonth) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(EXISTS_REPORT)) {
            ps.setString(1, reportType);
            ps.setString(2, reportMonth);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
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
