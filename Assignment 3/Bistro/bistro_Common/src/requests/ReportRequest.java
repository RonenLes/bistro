package requests;

import java.time.YearMonth;


/**
 * Request payload for manager report retrieval.
 *
 * <p>Main idea:
 * Specifies an optional target month for which the server should return stored monthly reports.
 * If the month is not provided, the server may default to the previous month.</p>
 *
 * <p>Main fields:
 * <ul>
 *   <li>{@code month} - target month (YearMonth)</li>
 * </ul>
 */
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
