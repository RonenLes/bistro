package database;

import entities.OpeningHours;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class OpeningHoursDAO {

	//SELECT
	private final String SELECT_openingHoursByDate = "SELECT * FROM `opening_hours` WHERE date = ?";
	
	/**
	 * 
	 * @param date of the desired date to check
	 * @return OpeningHours entity 
	 * @throws SQLException
	 */
	public OpeningHours getOpeningHour(LocalDate date) throws SQLException{
		OpeningHours openHour = null;
		java.sql.Date openDate = java.sql.Date.valueOf(date);
		try(Connection conn = DBManager.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(SELECT_openingHoursByDate)){
			
			pstmt.setDate(1, openDate);
			
			ResultSet rs = pstmt.executeQuery();
			
			if(rs.next()) {
				openHour = new OpeningHours(date,
								rs.getString("day"),
								rs.getTime("openTime").toLocalTime(),
								rs.getTime("closeTime").toLocalTime(),
								rs.getString("occasion"));
			}
			return openHour;
			
		}catch(SQLException e) {
			System.err.println("Database error: could not fetch opening for this date: "+date.toString());
		}
		return openHour;
	}
}
