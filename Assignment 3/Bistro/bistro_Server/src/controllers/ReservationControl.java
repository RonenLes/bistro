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

	public ReservationControl(ReservationDAO reservationDAO,
	                          TableDAO tableDAO,
	                          OpeningHoursDAO openingHoursDAO,
	                          UserDAO userDAO,
	                          NotificationControl notificationControl) {
	    this.reservationDAO = reservationDAO;
	    this.tableDAO = tableDAO;
	    this.openingHoursDAO = openingHoursDAO;
	    this.userDAO = userDAO;
	    this.notificationControl = notificationControl;
	}
	
	public Response<ReservationResponse> handleReservationRequest(ReservationRequest req) {

	    if (req == null) {
	        return new Response<>(false, "Request is missing", null);
	    }

	    if (req.getType() == null) {
	        return new Response<>(false, "Phase is missing", null);
	    }

	    return switch (req.getType()) {

	        case FIRST_PHASE -> {
	            try {
	                List<LocalTime> availableTimes =
	                        getAvailableTimes(req.getReservationDate(), req.getPartySize());

	                // 1) Availability exists
	                if (availableTimes != null && !availableTimes.isEmpty()) {
	                    ReservationResponse rr = new ReservationResponse(
	                            ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_AVAILABILITY,
	                            availableTimes,
	                            null,
	                            null
	                    );
	                    yield new Response<>(true, "Available times found", rr);
	                }

	                // 2) No availability -> check suggestions (next 3 days)
	                Map<LocalDate, List<LocalTime>> suggestions =
	                        getSuggestionsForNextDays(req.getReservationDate(), req.getPartySize());

	                boolean hasAnySuggestion = suggestions != null &&
	                        suggestions.values().stream().anyMatch(list -> list != null && !list.isEmpty());

	                // 2a) Suggestions exist
	                if (hasAnySuggestion) {
	                    ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_SUGGESTIONS,
	                            null,
	                            suggestions,
	                            null
	                    );
	                    yield new Response<>(true, "No availability on requested date, showing suggestions", rr);
	                }

	                // 2b) No availability and no suggestions
	                ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS,
	                        null,
	                        null,
	                        null
	                );
	                yield new Response<>(true, "No availability or suggestions found", rr);

	            } catch (IllegalArgumentException e) {
	                // roundToCapacity can throw this
	                System.err.println("Party too large");
	                yield new Response<>(false, "Party too large", null);

	            } catch (Exception e) {
	                // DB errors etc.
	                System.err.println("Failed to interact with DB");
	                yield new Response<>(false, "Failed to interact with DB", null);
	            }
	        }

	        case SECOND_PHASE -> {
	            try {
	                // Minimal anti-crash checks (not “business validation”)
	                if (req.getReservationDate() == null || req.getStartTime() == null) {
	                    yield new Response<>(false, "Missing reservation date/time", null);
	                }

	                LocalDate date = req.getReservationDate();
	                LocalTime startTime = req.getStartTime();
	                int partySize = req.getPartySize();

	                int allocatedCapacity = roundToCapacity(partySize);

	                // Re-check availability right before insert
	                if (!isStillAvailable(date, startTime, allocatedCapacity)) {
	                    yield new Response<>(false, "Selected time is no longer available", null);
	                }

	                int confirmationCode = generateUniqueConfirmationCode();

	                boolean hasUser = req.getUserID() != null && !req.getUserID().isBlank();
	                String userId = hasUser ? req.getUserID() : null;
	                String guestContact = hasUser ? null : req.getGuestContact();

	                boolean inserted = reservationDAO.insertNewReservation(date, partySize, allocatedCapacity, confirmationCode, userId, startTime, "NEW", guestContact);

	                if (!inserted) {
	                    yield new Response<>(false, "Failed to create reservation", null);
	                }

	                // Non-real sending via NotificationControl
	                sendConfirmationNotification(req, confirmationCode);

	                ReservationResponse rr = new ReservationResponse(ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED,null,null,confirmationCode
	                );
	                yield new Response<>(true, "Reservation created", rr);

	            } catch (IllegalArgumentException e) {
	                yield new Response<>(false, "Party too large", null);

	            } catch (Exception e) {
	                yield new Response<>(false, "Failed to interact with DB", null);
	            }
	        }

	        case EDIT_RESERVATION -> {
	            try {

	                Response<ReservationResponse> response = editReservation(req.getReservationDate(),req.getStartTime(),req.getPartySize(),
	                        req.getGuestContact(),
	                        req.getConfirmationCode()
	                );

	                if (response == null) {
	                    yield new Response<>(false, "Edit reservation failed (no response)", null);
	                }

	                yield response;

	            } catch (IllegalArgumentException e) {
	                // Use for cases like invalid new party size, invalid time/date, etc.
	                yield new Response<>(false, e.getMessage() != null ? e.getMessage() : "Invalid edit request", null);

	            } catch (Exception e) {
	                // DB errors etc.
	                System.err.println("Failed to interact with DB (edit reservation)");
	                yield new Response<>(false, "Failed to interact with DB", null);
	            }
	        }
	        case CANCEL_RESERVATION -> {
	            try {
	                Response<ReservationResponse> response =cancelReservation(req.getConfirmationCode());

	                if (response == null) {
	                    yield new Response<>(false, "Cancel reservation failed (no response)", null);
	                }

	                yield response;

	            } catch (IllegalArgumentException e) {
	                yield new Response<>(false,(e.getMessage() != null ? e.getMessage() : "Invalid Cancel request"),null);

	            } catch (Exception e) {System.err.println("Failed to interact with DB (Cancel reservation)");
	                yield new Response<>(false, "Failed to interact with DB", null);
	            }
	        }
	    };
	   
	}
	
	
	
	
	/**
	 * Checks if there is still availability for the given date/time/capacity.
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

			ReservationResponse rr = new ReservationResponse(reservationDate,partySize,startTime,confirmationCode,guestContactToSave,ReservationResponse.ReservationResponseType.EDIT_RESERVATION);
			return new Response<>(true, "Reservation updated successfully", rr);

		} catch (IllegalArgumentException e) {
			return new Response<>(false, "Party too large", null);
			
		} catch (SQLException e) {
			System.err.println("DB error while editing reservation confirmationCode=" + confirmationCode);
			return new Response<>(false, "Failed to update reservation", null);
		}
	}

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

	        ReservationResponse rr = new ReservationResponse(reservation.getReservationDate(),reservation.getPartySize(), reservation.getStartTime(),reservation.getConfirmationCode(),reservation.getGuestContact(),
	        		ReservationResponse.ReservationResponseType.CANCEL_RESERVATION
	        );

	        return new Response<>(true, "Reservation cancelled successfully", rr);

	    } catch (SQLException e) {
	        System.err.println("DB error while cancelling reservation " + confirmationCode);
	        return new Response<>(false, "Database error", null);
	    }
	}
	
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
	
	
	private int roundToCapacity(int partySize) {
	    try {
			return tableDAO.getMinimalTableSize(partySize);
		} catch (SQLException e) {
			throw new IllegalArgumentException("party too large");
		}
	}
}