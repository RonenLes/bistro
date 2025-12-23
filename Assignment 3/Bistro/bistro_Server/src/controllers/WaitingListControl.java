package controllers;

import database.OpeningHoursDAO;
import database.ReservationDAO;
import database.TableDAO;
import database.UserDAO;
import requests.Request;
import responses.Response;

public class WaitingListControl {

	
	private final ReservationDAO reservationDAO;
	private final TableDAO tableDAO;
	private final OpeningHoursDAO openingHoursDAO;
	private final UserDAO userDAO;
	private final NotificationControl notificationControl;
	
	
	public WaitingListControl() {
	    this(new ReservationDAO(), new TableDAO(), new OpeningHoursDAO(),
	         new UserDAO(), new NotificationControl());
	}
	
	public WaitingListControl(ReservationDAO reservationDAO,TableDAO tableDAO,OpeningHoursDAO openingHoursDAO,                      
            UserDAO userDAO,NotificationControl notificationControl) {   
		
			this.reservationDAO = reservationDAO;
			this.tableDAO = tableDAO;
			this.openingHoursDAO = openingHoursDAO;
			this.userDAO = userDAO;
			this.notificationControl = notificationControl;
	}
	
	
}
