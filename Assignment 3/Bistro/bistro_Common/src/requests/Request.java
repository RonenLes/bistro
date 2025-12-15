package requests;

public class Request<T> {
	private String command;
	private T data;
	
	public Request() {}

	public Request(String command, T data) {
		super();
		this.command = command;
		this.data = data;
	}

	public String getCommand() {
		return command;
	}

	public T getData() {
		return data;
	}	
	
}
