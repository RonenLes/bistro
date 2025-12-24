package controllers;

import database.TableDAO;

import database.ReservationDAO;
import database.OpeningHoursDAO;
import database.UserDAO;
import entities.OpeningHours;
import entities.Reservation;
import entities.User;
import requests.ReservationRequest;
import responses.ReservationResponse;
import responses.ReservationResponse.ReservationResponseType;
import responses.Response;
import java.util.Random;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Controller responsible for handling reservation-related requests in the system.
 *
 * This controller handles client requests and the
 * database access objects (DAOs). It supports multiple flows:
 *
 *  FIRST_PHASE: Given a date and party size, return available start times. If none exist,
 *       return suggestions for upcoming days.
 *   SECOND_PHASE: Given a date, start time, party size and user/guest identity,
 *       re-check availability (anti race-condition), create a reservation, and send confirmation.
 *   EDIT_RESERVATION: Update an existing reservation (guest contact can change only for guests).
 *   CANCEL_RESERVATION: Cancel an existing reservation by confirmation code.
 *   WALKIN_RESERVATION: Creates a reservation for "now" (date = LocalDate.now, time = LocalTime.now).
 * 
 * Availability logic is based on table capacity groups: partySize is rounded up to the minimal
 * table size that can fit it, and availability is checked by comparing how many tables exist for that capacity
 * vs. how many are booked in overlapping reservations.
 *
 * Dependencies are injected via constructors to allow unit testing using stubs/mocks.
 */
public class ReservationControl {
	
	private final ReservationDAO reservationDAO;
	private final TableDAO tableDAO;
	private final OpeningHoursDAO openingHoursDAO;
	private final UserDAO userDAO;
	private final NotificationControl notificationControl;

	public ReservationControl() {
	    this(new ReservationDAO(), new TableDAO(), new OpeningHoursDAO(),
	         new UserDAO(), new NotificationControl());
	}

	public ReservationControl(ReservationDAO reservationDAO,TableDAO tableDAO,OpeningHoursDAO openingHoursDAO,
	                          UserDAO userDAO, NotificationControl notificationControl) {	                          	                          	                         
	    this.reservationDAO = reservationDAO;
	    this.tableDAO = tableDAO;
	    this.openingHoursDAO = openingHoursDAO;
	    this.userDAO = userDAO;
	    this.notificationControl = notificationControl;
	}
	
	/**
	 * Handles a reservation request by dispatching to the relevant flow according to request type.
	 * @param req the incoming reservation request
	 * @return with success/failure, message, and optionally a ReservationResponse payload
	 */
	public Response<ReservationResponse> handleReservationRequest(ReservationRequest req) {
	    if (req == null) return new Response<>(false, "Request is missing", null);
	        	    
	    if (req.getType() == null) return new Response<>(false, "Phase is missing", null);
	        	    
	    return switch (req.getType()) {
	        case FIRST_PHASE -> handleFirstPhase(req);
	        case SECOND_PHASE -> handleSecondPhase(req);
	        case EDIT_RESERVATION -> handleEdit(req);
	        case CANCEL_RESERVATION -> handleCancel(req);
	        case WALKIN_RESERVATION -> handleWalkIn(req);
	    };   		        	            	   
	}
	
	/**
	 * Handles the FIRST_PHASE logic
	 * Computes all available times for the given date and party size
	 * If none are found, computes suggestions for the next 7 days
	 * @param req containing date and party size
	 * @return response with available times/suggestions OR failure response in case of errors
	 */
	private Response<ReservationResponse> handleFirstPhase(ReservationRequest req){
		try {
			List<LocalTime> availableTimes = getAvailableTimes(req.getReservationDate(), req.getPartySize());
			
			if (availableTimes != null && !availableTimes.isEmpty()) {
	            ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_AVAILABILITY,	                    
	                    availableTimes, null, null);	            
	            return successResponse("Available times found", rr);	            	            
	        }
			
			//no available time found check for suggestions
			Map<LocalDate, List<LocalTime>> suggestions =getSuggestionsForNextDays(req.getReservationDate(), req.getPartySize());
			boolean hasAnySuggestion = suggestions != null &&
                    suggestions.values().stream().anyMatch(list -> list != null && !list.isEmpty());
			
			if (hasAnySuggestion) {
                ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_SUGGESTIONS,
                        null,suggestions,null );
                        
                return successResponse("No availability on requested date, showing suggestions", rr);               
			}
			
			//no suggestions found ---NOTICE: decide later if to send generic request or specified request
			ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS,
                    null,null,null);
              return failResponse("No availability or suggestions found");                                           
                   
		}catch(IllegalArgumentException e) {
			return failResponse("Party too large");
			
		}catch(Exception e) {		
			return failResponse("Failed to interact with DB");
		}
		
	}
	
	/**
	 * Handles the SECOND_PHASE logic:
	 * Validates that date and startTime exist (minimal anti-crash validation)
	 * Delegates to {@link createReservation(ReservationRequest)}
	 * @param req request containing date, time, party size and identity
	 * @return response containing confirmation code if successful
	 */
	private Response<ReservationResponse> handleSecondPhase(ReservationRequest req){
		try {
	        if (req.getReservationDate() == null || req.getStartTime() == null) {
	            return failResponse("Missing reservation date/time");
	        }
	        return createReservation(req);
	    } catch (IllegalArgumentException e) {
	        return failResponse("Party too large");
	    } catch (Exception e) {
	        return failResponse("Failed to interact with DB");
	    }
	}
	
	
	/**
	 * Creates a reservation in the database after verifying availability.
	 * flow of work:
	 * Check availability for the slot using {@link #isStillAvailable(LocalDate, LocalTime, int)}
	 * Generate a unique 6-digit confirmation code
	 * Insert reservation into DB using {@link database.ReservationDAO#insertNewReservation(...)}
	 * Send notification to subscriber or guest
	 * @param req the request to create reservation from
     * @return success response with {@link ReservationResponseType#SECOND_PHASE_CONFIRMED} and confirmation code,or failure response        
	 * @throws Exception
	 */
	private Response<ReservationResponse> createReservation(ReservationRequest req) throws Exception{
		
		int partySize = req.getPartySize();
        int allocatedCapacity = roundToCapacity(partySize);
        
        //check if can reserve based on other reservations
        if (!isStillAvailable(req.getReservationDate(), req.getStartTime(), allocatedCapacity)) 
            return failResponse("Selected time is no longer available");
        
        int confirmationCode = generateUniqueConfirmationCode();
        boolean hasUser = req.getUserID() != null && !req.getUserID().isBlank();
        String userId = hasUser ? req.getUserID() : null;
        String guestContact = hasUser ? null : req.getGuestContact();
        
        int inserted = reservationDAO.insertNewReservation(req.getReservationDate(), partySize, allocatedCapacity, 
        		confirmationCode, userId, req.getStartTime(), "NEW", guestContact);
        
        if (inserted == -1) return failResponse("Failed to create reservation");
        
        sendConfirmationNotification(req, confirmationCode);
        
        ReservationResponse rr = new ReservationResponse(req.getReservationDate(),
        		partySize,req.getStartTime(),confirmationCode,
        		userId !=null ? userId  : guestContact,ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED);
        
        return successResponse("Reservation created", rr);                      
	}
	
	private Response<ReservationResponse> handleWalkIn(ReservationRequest req){
		try {
			//TO_DO implment a walked in customer with no reservation 
			//if available table give add his reservation ,seating database and give him a table
			//if no send back available time and wait for another reservation with fixed date and add to waiting list
		
			
			
			
		}catch(IllegalArgumentException e) {
			return failResponse("Party too large");
		}catch(Exception e) {
			return failResponse("Failed to interact with DB");
		}
		
	}
	
	/**
	 * Handles edit reservation request by delegating to {@link #editReservation(LocalDate, LocalTime, int, String, int)}
	 * @param req request containing new date/time/party size + confirmation code
	 * @return describing the edit result
	 */
	private Response<ReservationResponse> handleEdit(ReservationRequest req){
		try {
			Response<ReservationResponse> response = editReservation(req.getReservationDate(),req.getStartTime(),req.getPartySize(),
                    req.getGuestContact(),req.getConfirmationCode());
			return response != null ? response : failResponse("Edit reservation failed");
                    
            
		}catch (IllegalArgumentException e) {
	        return failResponse(e.getMessage() != null ? e.getMessage() : "Invalid edit request");
	    } catch (Exception e) {
	        return failResponse("Failed to interact with DB(edit)");
	    }
	}
	
	/**
	 * Handles cancel reservation request by delegating to {@link #cancelReservation(int)}
	 * @param req request containing confirmation code
	 * @return response describing cancel result
     */
	private Response<ReservationResponse> handleCancel(ReservationRequest req){
		try {
			
			Response<ReservationResponse> response =cancelReservation(req.getConfirmationCode());
			
			return response != null ? response : failResponse("Cancel reservation failed");
			
		}catch (IllegalArgumentException e) {
	        return failResponse(e.getMessage() != null ? e.getMessage() : "Invalid cancel request");
	    } catch (Exception e) {
	        return failResponse("Failed to interact with DB");
	    }
	}
	
	// ---------- generic response generators ----------
	private Response<ReservationResponse> successResponse(String msg, ReservationResponse rr) {
	    return new Response<>(true, msg, rr);
	}

	private Response<ReservationResponse> failResponse(String msg) {
	    return new Response<>(false, msg, null);
	}
	//----------------------------------------------------
	//---------------HELPER METHODS-----------------------
	
	/**
	 * Checks if there is still availability for a requested slot.
	 * Availability is determined by:
	 * 
	 *  Total tables available for allocatedCapacity (from {@link TableDAO#getTotalTablesByCapacity()})
     *	Booked tables for overlapping reservations in the requested time range
     *  (from {@link ReservationDAO#getBookedTablesByCapacity(LocalDate, LocalTime, LocalTime)})
     *   
     *@param date reservation date
     * @param startTime desired start time
     * @param allocatedCapacity rounded capacity size
     * @return true if booked tables are less than total tables for that capacity
     * @throws SQLException if DB access fails
	 */
	private boolean isStillAvailable(LocalDate date, LocalTime startTime, int allocatedCapacity) throws SQLException {

	    int durationMinutes = 120;
	    LocalTime end = startTime.plusMinutes(durationMinutes);

	    Map<Integer, Integer> totals = tableDAO.getTotalTablesByCapacity();
	    int totalForCap = totals.getOrDefault(allocatedCapacity, 0);
	    if (totalForCap <= 0)
	        return false;

	    Map<Integer, Integer> booked = reservationDAO.getBookedTablesByCapacity(date, startTime, end);
	    int bookedForCap = booked.getOrDefault(allocatedCapacity, 0);

	    return bookedForCap < totalForCap;
	}
	
	
	
	
	/**
	 * Computes the list of available start times for a given date and party size
	 * @param date 
	 * @param partySize
	 * @return
	 * @throws SQLException
	 */
	public List<LocalTime> getAvailableTimes(LocalDate date , int partySize) throws SQLException{
		
		if (date == null) return new ArrayList<>();

	    OpeningHours openHour = openingHoursDAO.getOpeningHour(date);
	    if (openHour == null) return new ArrayList<>();

	    LocalTime open = openHour.getOpenTime();
	    LocalTime close = openHour.getCloseTime();
	    if (open == null || close == null) return new ArrayList<>();
		
		int cap = roundToCapacity(partySize);
		int duration = 120;
		
		List<LocalTime> availableSpace = new ArrayList<>();
		
		Map<Integer,Integer> totalTablesByCapacity = tableDAO.getTotalTablesByCapacity();
		
		for(LocalTime start = open;!start.plusMinutes(duration).isAfter(close);start=start.plusMinutes(30)) {
			LocalTime end = start.plusMinutes(duration);
			
			Map<Integer,Integer> alreadyBooked = reservationDAO.getBookedTablesByCapacity(date, start, end);
			
			int total = totalTablesByCapacity.getOrDefault(cap, 0);
			int booked = alreadyBooked.getOrDefault(cap, 0);
			
			if(booked < total) availableSpace.add(start);
		}
		return availableSpace;
	}
	
	
	
	/**
	 * Builds suggestions for available reservation times for the next 7 days after a requested date.
     *
	 * @param requestedDate
	 * @param partySize
	 * @return
	 * @throws SQLException
	 */
	public Map<LocalDate, List<LocalTime>> getSuggestionsForNextDays(LocalDate requestedDate,int partySize) throws SQLException {

	    Map<LocalDate, List<LocalTime>> suggestions = new LinkedHashMap<>();

	    // Check the next 3 days after the requested date
	    for (int i = 1; i <= 7; i++) {

	        LocalDate dateToCheck = requestedDate.plusDays(i);

	        List<LocalTime> availableTimes =
	                getAvailableTimes(dateToCheck, partySize);

	        suggestions.put(dateToCheck, availableTimes);
	    }

	    return suggestions;
	}
	
	
	/**
	 * Generates a random 6-digit confirmation code and checks for uniqueness using the DAO.
	 * @return unique confirmation code
	 * @throws SQLException
	 */
	private int generateUniqueConfirmationCode() throws SQLException {

	    Random rnd = new Random();

	    // 6-digit code example
	    while (true) {
	        int code = 100000 + rnd.nextInt(900000);
	        if (reservationDAO.getReservationByConfirmationCode(code)==null) {
	            return code;
	        }
	    }
	}
	
	
	
	/**
	 * Sends confirmation code notification based on whether the requester is a subscriber or a guest.
	 * For subscribers, the code is sent to both email and phone (if present).
	 * For guests, the code is sent to the single contact they provided (email OR phone).
	 */
	private void sendConfirmationNotification(ReservationRequest req, int confirmationCode) {
	    try {
	        boolean hasUser = req.getUserID() != null && !req.getUserID().isBlank();

	        if (hasUser) {
	            User user = userDAO.getUserByUserID(req.getUserID());
	            if (user == null) {
	                System.err.println("User not found for userID=" + req.getUserID());
	                return;
	            }
	            notificationControl.sendConfirmationToUser(user, confirmationCode);
	        } else {
	            notificationControl.sendConfirmationToGuest(req.getGuestContact(), confirmationCode);
	        }

	    } catch (SQLException e) {
	        System.err.println("[NOTIFY] Failed to fetch user contact info: " + e.getMessage());
	    }
	}

	
	/**
	 * Edits an existing reservation identified by confirmation code.
	 * flow of work:
	 * If reservation not found -> failure.
	 * UserID and status are preserved from existing reservation.
	 * Guest contact can be changed only if reservation belongs to a guest
	 * Availability is rechecked for the new slot before updating
	 * @param reservationDate
	 * @param startTime
	 * @param partySize
	 * @param guestContact
	 * @param confirmationCode
	 * @return
	 */
	public Response<ReservationResponse> editReservation(LocalDate reservationDate,LocalTime startTime,int partySize,String guestContact,int confirmationCode) {
		try {
			
			Reservation existing = reservationDAO.getReservationByConfirmationCode(confirmationCode);
			if (existing == null) return new Response<>(false, "Reservation not found", null);
							
			
			String userID = existing.getUserID();        
			String status = existing.getStatus();      

			
			String guestContactToSave = existing.getGuestContact();

			boolean isGuestReservation = (userID == null || userID.isBlank());
			if (isGuestReservation) {
				// allow update for guest
				guestContactToSave = guestContact; 
			}
			
			int newAllocatedCapacity = roundToCapacity(partySize);

			if (!isStillAvailable(reservationDate, startTime,newAllocatedCapacity)) {
				return new Response<>(false, "Requested time is not available", null);
			}

			
			boolean updated = reservationDAO.updateReservation(reservationDate,status,partySize,confirmationCode,guestContactToSave,userID,startTime);

			if (!updated) {
				return new Response<>(false, "Reservation was not updated", null);
			}

			ReservationResponse rr = new ReservationResponse(reservationDate,partySize,startTime,confirmationCode,guestContactToSave,
					ReservationResponse.ReservationResponseType.EDIT_RESERVATION);
			return new Response<>(true, "Reservation updated successfully", rr);

		} catch (IllegalArgumentException e) {
			return new Response<>(false, "Party too large", null);
			
		} catch (SQLException e) {
			System.err.println("DB error while editing reservation confirmationCode=" + confirmationCode);
			return new Response<>(false, "Failed to update reservation", null);
		}
	}

	/**
	 * Cancels an existing reservation by updating its status to "CANCELLED".
	 * @param confirmationCode
	 * @return
	 */
	public Response<ReservationResponse> cancelReservation(int confirmationCode) {
	    try {
	        Reservation reservation =reservationDAO.getReservationByConfirmationCode(confirmationCode);

	        if (reservation == null) {
	            return new Response<>(false, "Reservation not found", null);
	        }

	        boolean hasSuccessful = reservationDAO.updateStatus(confirmationCode,"CANCELLED");

	        if (!hasSuccessful) {
	            return new Response<>(false, "Failed to cancel reservation", null);
	        }

	        ReservationResponse rr = new ReservationResponse(reservation.getReservationDate(),reservation.getPartySize(), 
	        		reservation.getStartTime(),reservation.getConfirmationCode(),reservation.getGuestContact(),
	        		ReservationResponse.ReservationResponseType.CANCEL_RESERVATION
	        );

	        return new Response<>(true, "Reservation cancelled successfully", rr);

	    } catch (SQLException e) {
	        System.err.println("DB error while cancelling reservation " + confirmationCode);
	        return new Response<>(false, "Database error", null);
	    }
	}
	
	/**
	 *  Fetches and returns an existing reservation details by confirmation code.
	 * @param confirmationCode
	 * @return
	 */
	public Response<ReservationResponse> showReservation(int confirmationCode){
		
		try {
			
			Reservation existing = reservationDAO.getReservationByConfirmationCode(confirmationCode);
			if(existing == null) return new Response<>(false,"Reservation not found",null);
			ReservationResponse reservationResp = new ReservationResponse(existing.getReservationDate(),existing.getPartySize(),
					existing.getStartTime(),existing.getConfirmationCode(),null,ReservationResponseType.SHOW_RESERVATION);
			return new Response<>(true,"Here is your reservation",reservationResp);
			
		}catch(Exception e) {
			System.err.println("DB error while fetching reservation from db" );
			return new Response<>(false, "Failed to fetch reservation", null);
		}
	}
	
	/**
	 * Rounds party size to the minimal table capacity that can fit it using {@link TableDAO#getMinimalTableSize(int)}.
	 * @param partySize
	 * @return
	 */
	private int roundToCapacity(int partySize) {
	    try {
			return tableDAO.getMinimalTableSize(partySize);
		} catch (SQLException e) {
			throw new IllegalArgumentException("party too large");
		}
	}
}