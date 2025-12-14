package responses;

public class Response<T> {
	
	private boolean isSuccess;
	private String message;
	private T data;
	
	public Response() {}

	public Response(boolean isSuccess, String message, T data) {
		super();
		this.isSuccess = isSuccess;
		this.message = message;
		this.data = data;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public String getMessage() {
		return message;
	}

	public T getData() {
		return data;
	}
	
	
}
