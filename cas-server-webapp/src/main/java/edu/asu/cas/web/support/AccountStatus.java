package edu.asu.cas.web.support;

import java.util.Date;

public interface AccountStatus {

	public PasswordState getPasswordState();
	public Date getPasswordExpirationDate();
	public Date getLastPasswordChangeDate();
	public int getPasswordDaysRemaining();
	
}
