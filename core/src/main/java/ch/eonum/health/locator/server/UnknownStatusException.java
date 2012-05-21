package ch.eonum.health.locator.server;

public class UnknownStatusException extends RuntimeException {

	private String status;
	
	public UnknownStatusException(String status) {
		super();
		this.status = status;
	}
	
	public String getStatus() {
		return status;
	}

	

}
