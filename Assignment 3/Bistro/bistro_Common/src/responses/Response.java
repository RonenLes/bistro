package responses;

/**
 * Generic server response wrapper.
 *
 * <p>Main idea:
 * Every server operation returns a {@code Response} indicating success/failure, a human-readable message,
 * and an optional payload {@code data} (usually one of the specific response DTOs).</p>
 *
 * <p>Main fields:
 * <ul>
 *   <li>{@code isSuccess} - operation outcome</li>
 *   <li>{@code message} - explanation for UI/logging</li>
 *   <li>{@code data} - typed payload (may be null)</li>
 * </ul>
 */
public class Response<T> {
	
	private boolean isSuccess;
	private String message;
	private T data;
	
	
	public Response() {}

	public Response(boolean isSuccess, String message, T data) {
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
