package responses;

import java.io.Serializable;
import java.util.List;
import entities.Reservation;

public class ShowDataResponse implements Serializable {

    private boolean isSuccess;
    private List<Reservation> reservationList;
    private String msg;

    public ShowDataResponse(boolean isSuccess, List<Reservation> reservationList, String msg) {
        this.isSuccess = isSuccess;
        this.reservationList = reservationList;
        this.msg = msg;
    }

    public boolean getIsSuccess() {
        return this.isSuccess;
    }

    public List<Reservation> getReservationList() {
        return this.reservationList;
    }

    @Override
    public String toString() {
        return this.msg;
    }
}
