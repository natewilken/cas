package edu.asu.cas.web.support;

public interface AccountStatusRegistry {
	public AccountStatus getAccountStatus(String principal) throws AccountStatusException;
}
