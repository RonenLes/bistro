package responses;

import java.io.Serializable;

public class ReportResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum ReportCommand {
        VISITOR_REPORT_RES,
        SUBSRIBER_REPORT_RES
    }
    private boolean success;
    private String message;
    private ReportCommand type;
    private byte[] payload;   
    public ReportResponse(boolean success,String message,ReportCommand type,byte[] payload) {
        this.success = success;
        this.message = message;
        this.type = type;
        this.payload = payload;
    }
    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public ReportCommand getType() {
        return type;
    }
    public byte[] getPayload() {
        return payload;
    }
}
