package responses;

import java.io.Serializable;

public class ReportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ReportCommand {
        VISITOR_REPORT_RES,
        SUBSRIBER_REPORT_RES
    }
    
    public ReportResponse() {}
    
    
    private ReportCommand type;
    private byte[] payload;   
    public ReportResponse(ReportCommand type,byte[] payload) {
        
        this.type = type;
        this.payload = payload;
    }
    
    public ReportCommand getType() {
        return type;
    }
    public byte[] getPayload() {
        return payload;
    }
}
