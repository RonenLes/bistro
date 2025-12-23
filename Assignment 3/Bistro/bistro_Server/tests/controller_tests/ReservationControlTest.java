package controller_tests;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import controllers.ReservationControl;
import dao_stubs.OpeningHoursDAOStub;
import dao_stubs.ReservationDAOStub;
import dao_stubs.TableDAOStub;
import dao_stubs.UserDAOStub;
import dao_stubs.NotificationControlStub;

import entities.Reservation;
import entities.User;

import requests.ReservationRequest;
import requests.ReservationRequest.ReservationRequestType;

import responses.Response;
import responses.ReservationResponse;
import responses.ReservationResponse.ReservationResponseType;

public class ReservationControlTest {

    private ReservationControl control;

    private OpeningHoursDAOStub openingStub;
    private ReservationDAOStub reservationStub;
    private TableDAOStub tableStub;
    private UserDAOStub userStub;
    private NotificationControlStub notifyStub;

    private LocalDate date;
    private LocalTime t10, t1030, t11, t1130, t12;

    @Before
    public void setup() {
        openingStub = new OpeningHoursDAOStub();
        reservationStub = new ReservationDAOStub();
        tableStub = new TableDAOStub();
        userStub = new UserDAOStub();
        notifyStub = new NotificationControlStub();

        control = new ReservationControl(reservationStub, tableStub, openingStub, userStub, notifyStub);

        date = LocalDate.of(2025, 12, 20);
        t10 = LocalTime.of(10, 0);
        t1030 = LocalTime.of(10, 30);
        t11 = LocalTime.of(11, 0);
        t1130 = LocalTime.of(11, 30);
        t12 = LocalTime.of(12, 0);
    }

    // -------------------- basic guards --------------------

    @Test
    public void handleReservationRequest_nullRequest_returnsFailure() {
        Response<ReservationResponse> resp = control.handleReservationRequest(null);
        assertFalse(resp.isSuccess());
        assertEquals("Request is missing", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    public void handleReservationRequest_nullType_returnsFailure() {
        ReservationRequest req = new ReservationRequest();
        req.setType(null);

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Phase is missing", resp.getMessage());
        assertNull(resp.getData());
    }

    // -------------------- FIRST_PHASE --------------------

    @Test
    public void firstPhase_availabilityExists_returnsShowAvailability() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2);

        reservationStub.setBooked(date, t10, t10.plusHours(2), Map.of(4, 1));

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.FIRST_PHASE,
                date, null,
                3, "U1", null,
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.FIRST_PHASE_SHOW_AVAILABILITY, rr.getType());

        List<LocalTime> times = rr.getAvailableTimes();
        System.out.println("FIRST_PHASE times: " + times);

        assertNotNull(times);
        assertTrue(times.contains(t10));
    }

    @Test
    public void firstPhase_noAvailability_hasSuggestions_returnsShowSuggestions() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        for (LocalTime start : List.of(t10, t1030, t11, t1130, t12)) {
            reservationStub.setBooked(date, start, start.plusHours(2), Map.of(4, 1));
        }

        LocalDate nextDay = date.plusDays(1);
        openingStub.put(nextDay, "Sunday", t10, LocalTime.of(14, 0), "REGULAR");

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.FIRST_PHASE,
                date, null,
                4, null, "guest@mail.com",
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.FIRST_PHASE_SHOW_SUGGESTIONS, rr.getType());

        assertNotNull(rr.getSuggestedDates());
        System.out.println("FIRST_PHASE suggestions: " + rr.getSuggestedDates());

        assertTrue(rr.getSuggestedDates().containsKey(nextDay));
        assertFalse(rr.getSuggestedDates().get(nextDay).isEmpty());
    }

    @Test
    public void firstPhase_noAvailability_noSuggestions_returnsNoAvailabilityOrSuggestions() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        for (LocalTime start : List.of(t10, t1030, t11, t1130, t12)) {
            reservationStub.setBooked(date, start, start.plusHours(2), Map.of(4, 1));
        }

        // next 7 days: no opening hours => suggestions empty
        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.FIRST_PHASE,
                date, null,
                4, null, "guest@mail.com",
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.FIRST_PHASE_NO_AVAILABILITY_OR_SUGGESTIONS, rr.getType());
    }

    @Test
    public void firstPhase_partyTooLarge_returnsFailure() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(2, 5);
        tableStub.setCapacityCount(4, 5);
        tableStub.setCapacityCount(6, 5);
        tableStub.setCapacityCount(8, 5);

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.FIRST_PHASE,
                date, null,
                30, "U1", null,
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Party too large", resp.getMessage());
    }

    // -------------------- SECOND_PHASE --------------------

    @Test
    public void secondPhase_missingDateOrTime_returnsFailure() {
        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.SECOND_PHASE,
                null, null,
                4, "U1", null,
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Missing reservation date/time", resp.getMessage());
    }

    @Test
    public void secondPhase_timeNoLongerAvailable_returnsFailure() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        reservationStub.setBooked(date, t10, t10.plusHours(2), Map.of(4, 1));

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.SECOND_PHASE,
                date, t10,
                4, "U1", null,
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Selected time is no longer available", resp.getMessage());
    }

    @Test
    public void secondPhase_success_guest_insertsAndNotifiesGuest() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2);

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.SECOND_PHASE,
                date, t10,
                4, null, "guest@mail.com",
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.SECOND_PHASE_CONFIRMED, rr.getType());
        assertNotNull(rr.getConfirmationCode());

        // notified guest
        assertEquals("guest@mail.com", notifyStub.lastGuestContact);
        assertEquals((int) rr.getConfirmationCode(), notifyStub.lastCode);

        // inserted into stub
        Reservation inserted = reservationStub.getReservationByConfirmationCode(rr.getConfirmationCode());
        assertNotNull(inserted);
        assertEquals("NEW", inserted.getStatus());
        assertEquals("guest@mail.com", inserted.getGuestContact());
        assertNull(inserted.getUserID());
    }

    @Test
    public void secondPhase_success_user_insertsAndNotifiesUser() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 2);

        userStub.putUser(new User("U1", "ronen", "pass", "MANAGER", "0523334444", "king@gmail.com"));

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.SECOND_PHASE,
                date, t10,
                4, "U1", null,
                0
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.SECOND_PHASE_CONFIRMED, rr.getType());

        assertEquals("U1", notifyStub.lastUserId);
        assertEquals((int) rr.getConfirmationCode(), notifyStub.lastCode);
    }

    // -------------------- EDIT_RESERVATION --------------------

    @Test
    public void editReservation_notFound_returnsFailure() throws Exception {
        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.EDIT_RESERVATION,
                date, t10,
                4, null, "new@mail.com",
                999999
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Reservation not found", resp.getMessage());
    }

    @Test
    public void editReservation_timeNotAvailable_returnsFailure() throws Exception {
        openingStub.put(date, "Saturday", t10, LocalTime.of(14, 0), "REGULAR");
        tableStub.setCapacityCount(4, 1);

        Reservation existing = new Reservation(
                1, date, "NEW", 4, 4, 123456, "old@mail.com", null, t10
        );
        reservationStub.addReservation(existing);

        // New requested slot fully booked
        reservationStub.setBooked(date, t1030, t1030.plusHours(2), Map.of(4, 1));

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.EDIT_RESERVATION,
                date, t1030,
                4, null, "new@mail.com",
                123456
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Requested time is not available", resp.getMessage());
    }

    @Test
    public void editReservation_guest_canChangeGuestContact_updatesReservation() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 20);
        LocalTime oldStart = LocalTime.of(10, 0);
        LocalTime newStart = LocalTime.of(10, 30);

        openingStub.put(date, "Saturday", LocalTime.of(10, 0), LocalTime.of(14, 0), "REGULAR");

        // ✅ must be 2, because the existing reservation overlaps the new slot
        tableStub.setCapacityCount(4, 2);

        Reservation existing = new Reservation(
                1, date, "NEW", 4, 4, 111111, "old@mail.com", null, oldStart
        );
        reservationStub.addReservation(existing);

        ReservationRequest req = new ReservationRequest(
                ReservationRequest.ReservationRequestType.EDIT_RESERVATION,
                date,
                newStart,
                4,
                null,
                "new@mail.com",
                111111
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);

        System.out.println("EDIT resp.success=" + resp.isSuccess() + " msg=" + resp.getMessage());

        assertTrue(resp.isSuccess());
        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponse.ReservationResponseType.EDIT_RESERVATION, rr.getType());

        Reservation updated = reservationStub.getReservationByConfirmationCode(111111);
        assertNotNull(updated);
        assertEquals("new@mail.com", updated.getGuestContact());
        assertEquals(newStart, updated.getStartTime());
    }

    

    // -------------------- CANCEL_RESERVATION --------------------

    @Test
    public void cancelReservation_notFound_returnsFailure() throws Exception {
        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.CANCEL_RESERVATION,
                null, null,
                0, null, null,
                777777
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertFalse(resp.isSuccess());
        assertEquals("Reservation not found", resp.getMessage());
    }

    @Test
    public void cancelReservation_success_setsCancelled_andReturnsResponse() throws Exception {
        Reservation existing = new Reservation(
                1, date, "NEW", 4, 4, 333333, "guest@mail.com", null, t10
        );
        reservationStub.addReservation(existing);

        ReservationRequest req = new ReservationRequest(
                ReservationRequestType.CANCEL_RESERVATION,
                null, null,
                0, null, null,
                333333
        );

        Response<ReservationResponse> resp = control.handleReservationRequest(req);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.CANCEL_RESERVATION, rr.getType());
        assertEquals(333333, (int) rr.getConfirmationCode());

        Reservation updated = reservationStub.getReservationByConfirmationCode(333333);
        assertNotNull(updated);
        assertEquals("CANCELLED", updated.getStatus());
    }

    // -------------------- showReservation() --------------------

    @Test
    public void showReservation_notFound_returnsFailure() {
        Response<ReservationResponse> resp = control.showReservation(444444);
        assertFalse(resp.isSuccess());
        assertEquals("Reservation not found", resp.getMessage());
    }

    @Test
    public void showReservation_success_returnsShowReservationType() throws Exception {
        Reservation existing = new Reservation(
                1, date, "NEW", 4, 4, 555555, "guest@mail.com", null, t10
        );
        reservationStub.addReservation(existing);

        Response<ReservationResponse> resp = control.showReservation(555555);
        assertTrue(resp.isSuccess());

        ReservationResponse rr = resp.getData();
        assertNotNull(rr);
        assertEquals(ReservationResponseType.SHOW_RESERVATION, rr.getType());

        // in your showReservation(), you set guestContact null in the response ctor
        assertEquals(date, rr.getNewDate());
        assertEquals(4, rr.getNewPartySize());
        assertEquals(t10, rr.getNewTime());
        assertEquals(555555, rr.getConfirmationCode().intValue());
    }
}
