package edu.asu.cas.web.support.edna;

import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Days;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.PasswordState;
import edu.asu.cas.web.support.PasswordStateException;

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
	
	public static EDNAAccountStatus getAccountStatus(Map<String,Object> attributes) throws PasswordStateException {
		if (attributes == null || attributes.isEmpty()) {
			throw new PasswordStateException("password state attributes are missing");
		}
		
		int pwState = (attributes.get("pwState") != null) ? ((Number)attributes.get("pwState")).intValue() : 0;
		Date pwExpirationDate = (Date)attributes.get("pwExpirationDate");
		Date pwLastChangeDate = (Date)attributes.get("pwLastChangeDate");
		
		if (pwState == 2) {
			return new EDNAAccountStatus(PasswordState.ADMIN_FORCED_CHANGE, pwExpirationDate, pwLastChangeDate);
			
		} else if (pwState == 1) {
			
			Date now = new Date();
			
			if (pwExpirationDate == null) {
				throw new PasswordStateException("password state == 1, but expiration date is null");
				
			} else if (now.before(pwExpirationDate)) {
				return new EDNAAccountStatus(PasswordState.WARN, pwExpirationDate, pwLastChangeDate);
				
			} else {
				return new EDNAAccountStatus(PasswordState.EXPIRED, pwExpirationDate, pwLastChangeDate);
			}
			
		} else {
			return new EDNAAccountStatus(PasswordState.OK, pwExpirationDate, pwLastChangeDate);
		}
	}
	
}
