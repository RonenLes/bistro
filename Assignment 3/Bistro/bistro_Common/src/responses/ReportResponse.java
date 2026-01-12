package responses;

import java.time.LocalDateTime;
import java.util.List;

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
