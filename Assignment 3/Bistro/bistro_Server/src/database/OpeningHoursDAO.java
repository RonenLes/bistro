package database;

import entities.OpeningHours;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class OpeningHoursDAO {

	//SELECT
	private final String SELECT_openingHoursByDate = "SELECT * FROM `opening_hours` WHERE date = ?";
	
	//UPDATE
	private final String UPDATE_OPENINGHOURS ="UPDATE `opening_hours` SET openTime = ?, closeTime = ?, occasion = ? WHERE date = ?";
	
	/**
	 * 
	 * @param date of the desired date to check
	 * @return OpeningHours entity 
	 * @throws SQLException
	 */
	public OpeningHours getOpeningHour(Connection conn,LocalDate date) throws SQLException{
		OpeningHours openHour = null;
		java.sql.Date openDate = java.sql.Date.valueOf(date);
		try(PreparedStatement pstmt = conn.prepareStatement(SELECT_openingHoursByDate)){
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
			throw e;
		}
		
	}
	
	public boolean updateOpeningHours(Connection conn,LocalTime open,LocalTime close,LocalDate date,String occasion)throws SQLException{
		java.sql.Date theDate = java.sql.Date.valueOf(date);
		java.sql.Time newOpenTime = java.sql.Time.valueOf(open);
		java.sql.Time newCloseTime = java.sql.Time.valueOf(close);
		try(PreparedStatement ps = conn.prepareStatement(UPDATE_OPENINGHOURS)){
			ps.setDate(1, theDate);
			ps.setTime(2, newOpenTime);
			ps.setTime(3, newCloseTime);
			ps.setString(4, occasion);
			return ps.executeUpdate()==1;
		}
	}
}
