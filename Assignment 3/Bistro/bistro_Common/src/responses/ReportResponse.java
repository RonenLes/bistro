package responses;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for monthly reports.
 *
 * <p>Main idea:
 * Combines two report datasets for a given month:
 * <ul>
 *   <li>{@code visits}: list of visit time ranges (check-in/check-out pairs)</li>
 *   <li>{@code dailyCounts}: daily counts matrix (e.g., subscribers/visitors per day)</li>
 * </ul>
 */
public class ReportResponse {

    

    
    private final String month; 

    
    private final List<LocalDateTime[]> visits;

    
    private final Integer[][] dailyCounts;

    
    public ReportResponse(String month,List<LocalDateTime[]> visits,Integer[][] dailyCounts) {
        
        this.month = month;
        this.visits = visits;
        this.dailyCounts = dailyCounts;
    }
    
    public String getMonth() {
        return month;
    }
    public List<LocalDateTime[]> getVisits() {
        return visits;
    }
    public Integer[][] getDailyCounts() {
        return dailyCounts;
    }
}
