package ch.eonum.health.locator.server;

public class OverQueryLimitException extends RuntimeException {

	public OverQueryLimitException(String string) {
		super(string);
	}

}
