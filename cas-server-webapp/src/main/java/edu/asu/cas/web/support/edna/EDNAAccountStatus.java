package edu.asu.cas.web.support.edna;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Days;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.PasswordState;

public class EDNAAccountStatus implements AccountStatus {
	protected PasswordState passwordState;
	protected Date passwordExpirationDate;
	protected Date lastPasswordChangeDate;
	
	public EDNAAccountStatus(PasswordState passwordState, Date passwordExpirationDate, Date lastPasswordChangeDate) {
		this.passwordState = passwordState;
		this.passwordExpirationDate = passwordExpirationDate;
		this.lastPasswordChangeDate = lastPasswordChangeDate;
	}

	public PasswordState getPasswordState() {
		return passwordState;
	}

	public Date getPasswordExpirationDate() {
		return passwordExpirationDate;
	}

	public Date getLastPasswordChangeDate() {
		return lastPasswordChangeDate;
	}

	public int getPasswordDaysRemaining() {
		if (passwordState == PasswordState.OK) return -1;
		
		else if (passwordState == PasswordState.EXPIRED
				|| passwordState == PasswordState.ADMIN_FORCED_CHANGE) return 0;
		
		else if (passwordExpirationDate == null) return -1;
		
		return Days.daysBetween(DateTime.now(), new DateTime(passwordExpirationDate)).getDays();
	}
	
}
