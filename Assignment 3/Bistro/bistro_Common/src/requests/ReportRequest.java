package requests;

public class ReportRequest {
	public enum ReportCommand{
		VISITOR_REPORT,
		SUBSRIBER_REPORT;
	}
	
	private ReportCommand type;
	
	public ReportRequest(ReportCommand type) {
		this.type = type;
	}
	
	public ReportCommand getType() {
		return type;
	}
	
	
}
