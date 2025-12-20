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
import entities.User;
import requests.ReservationRequest;
import responses.Response;
import responses.ReservationResponse;

public class ReservationControlTest {

    private ReservationControl control;

    private OpeningHoursDAOStub openingStub;
    private ReservationDAOStub reservationStub;
    private TableDAOStub tableStub;
    private UserDAOStub userStub;
    private NotificationControlStub notifyStub;

    private LocalDate d0; // main test date
    private LocalDate d1; // next day (for suggestions)

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
        System.out.println("firstPhase_availableTimes_returnsAvailabilityResponse: " + rr.getAvailableTimes()+"\n");

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
        System.out.println("firstPhase_noAvailability_hasSuggestions_returnsSuggestionsResponse: "+rr.getSuggestedDates()+"\n");
        assertNotNull(rr);
        assertEquals(ReservationResponse.ReservationResponseType.FIRST_PHASE_SHOW_SUGGESTIONS, rr.getType());
        assertNotNull(rr.getSuggestedDates());
        assertTrue(rr.getSuggestedDates().containsKey(nextDay));
        assertFalse(rr.getSuggestedDates().get(nextDay).isEmpty());
    }

    @Test
    public void firstPhase_invalidPartySize_returnsFailure() {
        ReservationRequest req = firstPhaseReq(LocalDate.of(2025, 12, 20), 0, "U1", null);

        Response<?> resp = control.handleReservationRequest(req);
        System.out.println("firstPhase_invalidPartySize_returnsFailure: "+resp.getMessage()+"\n");
        assertFalse(resp.isSuccess());
        assertEquals("Party size must be positive", resp.getMessage());
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
        System.out.println("secondPhase_timeNoLongerAvailable_returnsFailure: "+resp.getMessage()+"\n");
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
        System.out.println("secondPhase_success_insertsReservation_andSendsNotification_toUser: \n");
        assertEquals(ReservationResponse.ReservationResponseType.SECOND_PHASE_CONFIRMED, rr.getType());

        assertEquals("U1", notifyStub.lastUserId);
        assertEquals((int) rr.getConfirmationCode(), notifyStub.lastCode);
    }

    

    
    //------------------------------------------------building requests--------------------------------------------------------
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
