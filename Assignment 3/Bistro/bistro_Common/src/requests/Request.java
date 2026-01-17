package requests;

/**
 * Generic network request wrapper sent from client to server.
 *
 * <p>Main idea:
 * A {@code Request} bundles a {@link Command} (what action the server should perform)
 * together with a payload object ({@code data}) that contains the command-specific data
 * (for example: login details, reservation details, seating actions, etc.).</p>
 *
 * <p>Typically serialized (e.g., with Kryo in your project) and dispatched on the server
 * using {@link #getCommand()} to decide how to interpret {@link #getData()}.</p>
 *
 * <p>Key parts:
 * <ul>
 *   <li>{@link Command} - enum of supported request types</li>
 *   <li>{@code data} - the payload object for the selected command</li>
 * </ul>
 */
public class Request<T> {
	private Command command;
	private T data;
	
	public enum Command{
		GUEST_REQUEST,
		USER_REQUEST,
		RESERVATION_REQUEST,	
		SEATING_REQUEST,
		MANAGER_REQUEST,
		BILLING_REQUEST,
		REPORT_REQUEST,
		LOST_CODE,
		LOGOUT_REQUEST,
		WAITING_LIST_REQUEST,
	}
	
	public Request() {}

	public Request(Command command, T data) {
		super();
		this.command = command;
		this.data = data;
	}

	public Command getCommand() {
		return command;
	}

	public T getData() {
		return data;
	}	
		 	
}
