package database;

import entities.OpeningHours;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class OpeningHoursDAO {
	//INSERT
	private final String INSERT_NEW_OPENING_HOUR = "INSERT INTO `opening_hours` (date, day, openTime, closeTime, occasion) VALUES(?, ?, ?, ?, ?)";

	//SELECT
	private final String SELECT_openingHoursByDate = "SELECT * FROM `opening_hours` WHERE date = ?";
	private final String SELECT_30_OPENING_HOURS = "SELECT * FROM `opening_hours` WHERE `date` >= ? AND `date` < DATE_ADD(?, INTERVAL 30 DAY) ORDER BY `date`"; 
	
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
	
	public boolean insertNewOpeningHour(Connection conn, LocalDate date, String day, LocalTime openTime,LocalTime closeTime)throws SQLException{
		java.sql.Date theDate = java.sql.Date.valueOf(date);
		java.sql.Time theOpenTime = java.sql.Time.valueOf(openTime);
		java.sql.Time theCloseTime = java.sql.Time.valueOf(closeTime);
		try(PreparedStatement ps = conn.prepareStatement(INSERT_NEW_OPENING_HOUR)){
			ps.setDate(1, theDate);
			ps.setTime(2, theOpenTime);
			ps.setTime(3, theCloseTime);
			ps.setString(4, "REGULAR");
			return ps.executeUpdate()==1;
		}
	}
	
	public List<OpeningHours> fetchOpeningHoursNext30Days(Connection conn,LocalDate startDate)throws SQLException{
		List<OpeningHours> out = new ArrayList<>();
		
		try(PreparedStatement ps = conn.prepareStatement(SELECT_30_OPENING_HOURS)){
			Date sqlDate = Date.valueOf(startDate);
			ps.setDate(1, sqlDate);
			ps.setDate(2, sqlDate);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				OpeningHours oh = new OpeningHours(rs.getDate("date").toLocalDate(),rs.getString("day"),rs.getTime("openTime").toLocalTime(), rs.getTime("closeTime").toLocalTime(), rs.getString("occasion"));
				out.add(oh);
			}
		}
		return out;
	}
}
