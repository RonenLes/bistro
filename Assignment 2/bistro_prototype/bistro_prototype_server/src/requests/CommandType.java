package requests;

import java.io.Serializable;

public enum CommandType implements Serializable {
	
	READ_ALL_EXISTING_RESERVATIONWS,//used by ShowDataRequest
	READ_RESERVATIONS_BY_DATE;//used by ShowDataRequest
}
