package edu.asu.cas.web.support;

public class AccountStatusException extends Exception {
	private static final long serialVersionUID = 1L;

	public AccountStatusException(String message) {
		super(message);
	}

	public AccountStatusException(Throwable cause) {
		super(cause);
	}

	public AccountStatusException(String message, Throwable cause) {
		super(message, cause);
	}

}
