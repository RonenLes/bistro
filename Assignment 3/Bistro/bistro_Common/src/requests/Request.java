package requests;

public class Request<T> {
	private Command command;
	private T data;
	
	public enum Command{
		LOGIN_REQUEST,
		RESERVATION_REQUEST,	
		EDIT_RESERVATION;
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
