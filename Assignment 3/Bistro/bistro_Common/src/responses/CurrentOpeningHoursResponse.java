package responses;

import java.time.LocalDate;
import java.time.LocalTime;

public class CurrentOpeningHoursResponse {

	private LocalDate date;
	private String day;
	private LocalTime open;
	private LocalTime close;
	private String occasion;
	
	public CurrentOpeningHoursResponse() {}

	public CurrentOpeningHoursResponse(LocalDate date, String day,LocalTime open, LocalTime close, String occasion) {
		this.date = date;
		this.day=day;
		this.open = open;
		this.close = close;
		this.occasion = occasion;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getOpen() {
		return open;
	}

	public LocalTime getClose() {
		return close;
	}

	public String getOccasion() {
		return occasion;
	}

	public String getDay() {
		return day;
	}
	
	
	
}
