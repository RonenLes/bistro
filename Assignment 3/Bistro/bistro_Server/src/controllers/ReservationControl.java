package controllers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import database.ReservationDAO;

public class ReservationControl {
	private final ReservationDAO reservationDAO;
	
	public boolean isReservationDateTimeInAllowedRange(LocalDate reservationDate,LocalTime startTime) {
		if (reservationDate == null || startTime == null) return false;


		LocalDateTime arrivalLocal = LocalDateTime.of(reservationDate, startTime);


		ZoneId zone = ZoneId.of("Asia/Jerusalem");
		ZonedDateTime now = ZonedDateTime.now(zone);

		ZonedDateTime arrival = arrivalLocal.atZone(zone);
		
		ZonedDateTime minAllowed = now.plusHours(1);     
		ZonedDateTime maxAllowed = now.plusMonths(1);   


		return !arrival.isBefore(minAllowed) && !arrival.isAfter(maxAllowed);
	}	
	
}
