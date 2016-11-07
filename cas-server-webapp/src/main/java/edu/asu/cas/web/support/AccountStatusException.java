package edu.asu.cas.web.support;

public class AccountStatusException extends Exception {
	private static final long serialVersionUID = 1151881716953977177L;

	public AccountStatusException() {
		super();
	}

	public AccountStatusException(String msg) {
		super(msg);
	}

	public AccountStatusException(Throwable t) {
		super(t);
	}

	public AccountStatusException(String msg, Throwable t) {
		super(msg, t);
	}

}
