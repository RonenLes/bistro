package entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class OpeningHours {

	private LocalDate date;
	private String day;
	private LocalTime openTime;
	private LocalTime closeTime;
	private String occasion;
	
	
	public OpeningHours(LocalDate date, String day, LocalTime openTime, LocalTime closeTime, String occasion) {		
		this.date = date;
		this.day = day;
		this.openTime = openTime;
		this.closeTime = closeTime;
		this.occasion = occasion;
	}


	public LocalDate getDate() {
		return date;
	}


	public String getDay() {
		return day;
	}


	public LocalTime getOpenTime() {
		return openTime;
	}


	public LocalTime getCloseTime() {
		return closeTime;
	}


	public String getOccasion() {
		return occasion;
	}
	 
	
	
	
}
