package controllers;

import database.OpeningHoursDAO;

import database.ReservationDAO;
import database.TableDAO;
import database.UserDAO;
import database.WaitingListDAO;
import requests.Request;
import requests.WaitingListRequest;
import responses.WaitingListResponse;
import responses.Response;

public class WaitingListControl {

	
	private final ReservationDAO reservationDAO;
	private final TableDAO tableDAO;
	private final OpeningHoursDAO openingHoursDAO;
	private final UserDAO userDAO;
	private final NotificationControl notificationControl;
	private final WaitingListDAO waitingListDAO;
	
	public WaitingListControl() {
	    this(new ReservationDAO(), new TableDAO(), new OpeningHoursDAO(),
	         new UserDAO(), new NotificationControl());
	}
	
	public WaitingListControl(ReservationDAO reservationDAO,TableDAO tableDAO,OpeningHoursDAO openingHoursDAO,                      
            UserDAO userDAO,NotificationControl notificationControl,WaitingListDAO waitingListDAO) {   
		
			this.reservationDAO = reservationDAO;
			this.tableDAO = tableDAO;
			this.openingHoursDAO = openingHoursDAO;
			this.userDAO = userDAO;
			this.notificationControl = notificationControl;
			this.waitingListDAO=waitingListDAO;
	}
	
	public Reponse<WaitingListResponse> handleWaitingListRequest(WaitingListRequest req) {
		if (req == null) {
	        return new Response<>(false, "Request is missing", null);
	    }
	    if (req.getType() == null) {
	        return new Response<>(false, "Phase is missing", null);
	    }
	    return switch (req.getType()) {
	    	case  REQ_WAIT -> 
	    };   		        	            	   
	}
	public void handleReqWait(WaitingListRequest req) {
		
	}
	
	}
	
	

