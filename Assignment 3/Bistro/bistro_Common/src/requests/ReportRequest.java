package requests;

import java.time.YearMonth;

public class ReportRequest {

    

    
    private YearMonth month;

    public ReportRequest() {}

    public ReportRequest( YearMonth month) {
        this.month = month;
    }

    public YearMonth getMonth() {
        return month;
    }
}
