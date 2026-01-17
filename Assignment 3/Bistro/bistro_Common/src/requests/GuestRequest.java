
package requests;


/**
 * Request payload used when a client continues as a guest.
 *
 * <p>Main idea:
 * Stores a single guest contact string (email or phone) that identifies the guest during the session.</p>
 */
public class GuestRequest {

	private String contact;
	public GuestRequest() {}
	
			
	public GuestRequest(String contact) {
		this.contact = contact;
	}




	public String getContact() {
		return contact;
	}
	
	
}
