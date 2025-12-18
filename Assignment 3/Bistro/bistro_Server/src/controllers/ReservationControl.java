package controllers;

import database.TableDAO;
import database.ReservationDAO;
import database.OpeningHoursDAO;

import entities.OpeningHours;
import entities.Table;
import entities.Reservation;

import requests.ReservationRequest;
import responses.ReservationResponse;
import requests.Request;
import responses.Response;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReservationControl {
	
	private OpeningHoursDAO openingHoursDAO;
	private ReservationDAO reservationDAO;
	private TableDAO tableDAO;
	
	// for tests
	public ReservationControl(OpeningHoursDAO oh, ReservationDAO r, TableDAO t) {
	    this.openingHoursDAO = oh;
	    this.reservationDAO = r;
	    this.tableDAO = t;
	}
	
	public ReservationControl() {
	    this.openingHoursDAO = new OpeningHoursDAO();
	    this.reservationDAO = new ReservationDAO();
	    this.tableDAO = new TableDAO();
	}
	
	
	public Response<?> handleReservationRequest(ReservationRequest req){
		
		if (req.getPhase() == null) {
	        return new Response<>(false, "Phase is missing", null);
	    }

	    return switch (req.getPhase()) {

	        case FIRST_PHASE -> {
	            try {
	                List<LocalTime> availableTime =
	                        getAvailableTimes(req.getDateToReserve(), req.getPartySize());
	                yield new Response<>(true, "All available times to reserve", availableTime);

	            } catch (IllegalArgumentException e) {
	                System.err.println("Party too large");
	                yield new Response<>(false, "Party too large", null);

	            } catch (Exception e) {
	                System.err.println("Failed to interact with DB");
	                yield new Response<>(false, "Failed to interact with DB", null);
	            }
	        }

	        case SECOND_PHASE -> {
	            // TODO implement booking logic (insert reservation) and confirmation code generator
	            yield new Response<>(false, "SECOND_PHASE not implemented yet", null);
	        }
	    };
	}
	
	public List<LocalTime> getAvailableTimes(LocalDate date , int partySize) throws SQLException{
		
		OpeningHours openHour = openingHoursDAO.getOpeningHour(date);
		LocalTime open = openHour.getOpenTime();
		LocalTime close  = openHour.getCloseTime();
		
		int cap = roundToCapacity(partySize);
		int duration = 120;
		
		List<LocalTime> availableSpace = new ArrayList<>();
		
		Map<Integer,Integer> totalTablesByCapacity = tableDAO.getTotalTablesByCapacity();
		
		for(LocalTime start = open;!start.plusMinutes(duration).isAfter(close);start=start.plusMinutes(duration)) {
			LocalTime end = start.plusMinutes(30);
			
			Map<Integer,Integer> alreadyBooked = reservationDAO.getBookedTablesByCapacity(date, start, end);
			
			int total = totalTablesByCapacity.getOrDefault(cap, 0);
			int booked = alreadyBooked.getOrDefault(cap, 0);
			
			if(booked < total) availableSpace.add(start);
		}
		return availableSpace;
	}
	
	
	//TO-DO improve logic so the TableDAO will fetch the closest capacity that is received in the method
	private int roundToCapacity(int partySize) {
	    if (partySize <= 2) return 2;
	    if (partySize <= 4) return 4;
	    if (partySize <= 6) return 6;
	    if (partySize <= 8) return 8;
	    throw new IllegalArgumentException("Party too large");
	}
}
