package controller_tests;



import static org.junit.Assert.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import controllers.ReservationControl;
import dao_stubs.ReservationDAOStub;
import dao_stubs.TableDAOStub;
import dao_stubs.UserDAOStub;
import dao_stubs.NotificationControlStub;
import dao_stubs.OpeningHoursDAOStub;
import entities.Reservation;
import entities.User;
import requests.ReservationRequest;
import responses.Response;
import responses.ReservationResponse;


/**
 * This class uses dao stubs without touching the real database
 * TESTS:
 * 		FIRST_PHASE: Returns available times when capacity exists
 * 		FIRST_PHASE: Returns suggestions for next days when the requested date is full
 * 		
 * 		SECOND_PHASE: Rejects reservation if the selected time becomes unavailable
 * 		SECOND_PHASE: Creates a reservation when available and returns a confirmation code
 * 		SECOND_PHASE: Sends confirmation notification to either a guest or a logged-in user
 * 	
 * @code OpeningHoursDAOStub
 * @code ReservationDAOStub
 * @code TableDAOStub
 * @code UserDAOStub
 * @code NotificationControlStub
 */
public class ReservationControlTest {

    private ReservationControl control;

    private OpeningHoursDAOStub openingStub;
    private ReservationDAOStub reservationStub;
    private TableDAOStub tableStub;
    private UserDAOStub userStub;
    private NotificationControlStub notifyStub;

    private LocalDate d0; // main test date
    private LocalDate d1; // next day (for suggestions)
    
    /**
     * Initializes fresh stub instances and injects them into a new {@link ReservationControl}
     * @throws SQLException
     */
    @Before
    public void setup() throws SQLException {

    	openingStub = new OpeningHoursDAOStub();
        reservationStub = new ReservationDAOStub();
        tableStub = new TableDAOStub();
        userStub = new UserDAOStub();
        notifyStub = new NotificationControlStub();

        control = new ReservationControl(
                reservationStub,
                tableStub,
                openingStub,
                userStub,
                notifyStub
        );
    }
    
    //--------------------------------------------------availability test--------------------------------------------------------
    @Test
    public void firstPhase_availableTimes_returnsAvailabilityResponse() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2);

        reservationStub.setBooked(date, LocalTime.of(10, 0), LocalTime.of(12, 0), Map.of(4, 1));

        ReservationRequest req = firstPhaseReq(date, 3, "U1", null);

        Response<?> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = (ReservationResponse) resp.getData();

        assertNotNull(rr);        
        assertEquals(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_AVAILABILITY, rr.getType());
        assertNotNull(rr.getAvailableTimes());
        assertTrue(rr.getAvailableTimes().contains(LocalTime.of(10, 0)));
    }
    
    @Test
    public void firstPhase_noAvailability_hasSuggestions_returnsSuggestionsResponse() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        // fully book the requested date
        for (LocalTime start : List.of(
                LocalTime.of(10,0), LocalTime.of(10,30), LocalTime.of(11,0),
                LocalTime.of(11,30), LocalTime.of(12,0)
        )) {
            reservationStub.setBooked(date, start, start.plusHours(2), Map.of(4, 1));
        }

        // next day is open and has free tables
        LocalDate nextDay = date.plusDays(1);
        openingStub.put(nextDay, "Sunday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");

        ReservationRequest req = firstPhaseReq(date, 4, null, "guest@mail.com");

        Response<?> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = (ReservationResponse) resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_SUGGESTIONS, rr.getType());
        assertNotNull(rr.getSuggestedDates());
        assertTrue(rr.getSuggestedDates().containsKey(nextDay));
        assertFalse(rr.getSuggestedDates().get(nextDay).isEmpty());
    }

    @Test
    public void firstPhase_partyTooLarge_returnsFailure() {
        LocalDate date = LocalDate.of(2025, 12, 20);

        // IMPORTANT: without this, controller returns "No availability..." instead of checking capacity
        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");

        // restaurant has only up to 8 seats tables
        tableStub.setCapacityCount(2, 5);
        tableStub.setCapacityCount(4, 5);
        tableStub.setCapacityCount(6, 2);
        tableStub.setCapacityCount(8, 1);

        ReservationRequest req = firstPhaseReq(date, 30, "U1", null);

        Response<?> resp = control.handleReservationRequest(req);

        System.out.println("DEBUG resp.success=" + resp.isSuccess() + ", msg=" + resp.getMessage());

        assertFalse(resp.isSuccess());
        assertEquals("Party too large", resp.getMessage());
    }

       
    
    @Test
    public void secondPhase_timeNoLongerAvailable_returnsFailure() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);
        LocalTime start = LocalTime.of(10, 0);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        // slot is fully booked
        reservationStub.setBooked(date, start, start.plusHours(2), Map.of(4, 1));

        ReservationRequest req = secondPhaseReq(date,start, 4, "U1", null);

        Response<?> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Selected time is no longer available", resp.getMessage());
    }
    
    @Test
    public void secondPhase_success_insertsReservation_andSendsNotification_toGuest() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);
        LocalTime start = LocalTime.of(10, 0);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2); // enough tables

        ReservationRequest req = secondPhaseReq(date,start, 4, null, "guest@mail.com");

        Response<?> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = (ReservationResponse) resp.getData();
        assertEquals(ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED, rr.getType());
        assertNotNull(rr.getConfirmationCode());

        // notification stub should have recorded the send
        assertEquals("guest@mail.com", notifyStub.lastGuestContact);
        assertEquals((int) rr.getConfirmationCode(), notifyStub.lastCode);
    }
    
    
    @Test
    public void secondPhase_success_insertsReservation_andSendsNotification_toUser() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);
        LocalTime start = LocalTime.of(10, 0);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2);

        // make sure userDAO can return the user for notification
        userStub.putUser(new User("U1", "ronen", "pass", "MANAGER","0523334444","king@gmail.com")); // adjust to your User ctor

        ReservationRequest req = secondPhaseReq(date,start, 4, "U1", null);

        Response<?> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = (ReservationResponse) resp.getData();
        assertEquals(ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED, rr.getType());

        assertEquals("U1", notifyStub.lastUserId);
        assertEquals((int) rr.getConfirmationCode(), notifyStub.lastCode);
    }

   //------------------------------------------------edit reservation tests--------------------------------------------------------
    
    @Test
    public void editReservation_notFound_returnsFailure() throws Exception {
        Response<?> resp = control.editReservation(
                123456,
                LocalDate.of(2025, 12, 20),
                LocalTime.of(10, 0),
                4,
                "guest@mail.com"
        );

        assertFalse(resp.isSuccess());
        assertEquals("Reservation not found", resp.getMessage());
    }
    
    @Test
    public void editReservation_guest_allowsUpdatingGuestContact() throws Exception {
        int code = 111111;

        // existing reservation is guest (userID = null)
        reservationStub.putExistingReservation(code,
                new Reservation(1,
                        LocalDate.of(2025, 12, 20),
                        "NEW",
                        2,
                        2,
                        code,
                        "old@mail.com",
                        null,
                        LocalTime.of(10, 0)));

        // availability ok
        tableStub.setMinimalTableSizeFor(3, 4);
        tableStub.setCapacityCount(4, 2);
        reservationStub.setBooked(LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                Map.of(4, 1));

        Response<?> resp = control.editReservation(
                code,
                LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                3,
                "new@mail.com"
        );

        assertTrue(resp.isSuccess());
        ReservationResponse rr = (ReservationResponse) resp.getData();
        assertEquals(ReservationResponse.ReservationResponseType.EDIT_RESERVATION, rr.getType());

        // verify updateReservation got new guest contact
        assertEquals("new@mail.com", reservationStub.lastUpdate_guestContact);
        assertNull(reservationStub.lastUpdate_userID); // still guest
    }
    
    @Test
    public void editReservation_subscriber_ignoresNewGuestContact() throws Exception {
        int code = 222222;

        // existing reservation is subscriber
        reservationStub.putExistingReservation(code,
                new Reservation(2,
                        LocalDate.of(2025, 12, 20),
                        "CONFIRMED",
                        4,
                        4,
                        code,
                        null,
                        "U1",
                        LocalTime.of(10, 0)));

        // availability ok
        tableStub.setMinimalTableSizeFor(4, 4);
        tableStub.setCapacityCount(4, 2);
        reservationStub.setBooked(LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                Map.of(4, 0));

        Response<?> resp = control.editReservation(
                code,
                LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                4,
                "SHOULD_NOT_BE_SAVED@mail.com"
        );

        assertTrue(resp.isSuccess());

        // should ignore guest contact for subscriber reservations
        assertNull(reservationStub.lastUpdate_guestContact);

        // identity fields kept
        assertEquals("U1", reservationStub.lastUpdate_userID);
        assertEquals("CONFIRMED", reservationStub.lastUpdate_status);
    }
    
    @Test
    public void editReservation_timeNotAvailable_returnsFailure() throws Exception {
        int code = 333333;

        reservationStub.putExistingReservation(code,
                new Reservation(3,
                        LocalDate.of(2025, 12, 20),
                        "NEW",
                        4,
                        4,
                        code,
                        null,
                        "U1",
                        LocalTime.of(10, 0)));

        // availability FAIL (total=1, booked=1)
        tableStub.setMinimalTableSizeFor(4, 4);
        tableStub.setCapacityCount(4, 1);
        reservationStub.setBooked(LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                LocalTime.of(14, 0),
                Map.of(4, 1));

        Response<?> resp = control.editReservation(
                code,
                LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                4,
                null
        );

        assertFalse(resp.isSuccess());
        assertEquals("Requested time is not available", resp.getMessage());
    }
    
    
    @Test
    public void editReservation_partyTooLarge_returnsFailure() throws Exception {
        int code = 444444;

        reservationStub.putExistingReservation(code,
                new Reservation(4,
                        LocalDate.of(2025, 12, 20),
                        "NEW",
                        4,
                        4,
                        code,
                        null,
                        "U1",
                        LocalTime.of(10, 0)));

        // make TableDAOStub throw SQLException for minimal size lookup -> roundToCapacity throws IllegalArgumentException
        tableStub.throwOnMinimalTableSize(true);

        Response<?> resp = control.editReservation(
                code,
                LocalDate.of(2025, 12, 21),
                LocalTime.of(12, 0),
                999,
                null
        );

        assertFalse(resp.isSuccess());
        assertEquals("Party too large", resp.getMessage());
    }





    
    //------------------------------------------------building requests--------------------------------------------------------
    
    /**
     * Builds a FIRST_PHASE {@link ReservationRequest}
     * FIRST_PHASE requests include only the reservation date and party size
     * @param date the requested reservation date
     * @param partySize number of guests requested
     * @param userId subscriber user id, or @code null if the requester is a guest
     * @param guestContact guest email/phone, or @code null if the requester is a subscriber
     * @return a populated FIRST_PHASE {@link ReservationRequest}
     */
    private ReservationRequest firstPhaseReq(LocalDate date, int partySize, String userId, String guestContact) {
        return new ReservationRequest(
                ReservationRequest.ReservationRequestType.FIRST_PHASE,
                date,
                null,          // startTime not needed in first phase
                partySize,
                userId,
                guestContact
        );
    }
    
    /**
     * Builds a SECOND_PHASE {@link ReservationRequest}
     * SECOND_PHASE requests represent the final confirmation step:
     * the client has selected a specific start time
     * @param date reservation date
     * @param start selected start time
     * @param partySize number of guests requested
     * @param userId subscriber user id, or {@code null} if the requester is a guest
     * @param guestContact guest email/phone, or {@code null} if the requester is a subscriber
     * @return a populated SECOND_PHASE {@link ReservationRequest}
     */
    private ReservationRequest secondPhaseReq(LocalDate date, LocalTime start, int partySize, String userId, String guestContact) {
        return new ReservationRequest(
                ReservationRequest.ReservationRequestType.SECOND_PHASE,
                date,
                start,
                partySize,
                userId,
                guestContact
        );
    }



}
