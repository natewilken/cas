package edu.asu.cas.web.support.edna;

import java.util.Date;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Days;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.AccountStatusException;
import edu.asu.cas.web.support.PasswordResetFactorState;
import edu.asu.cas.web.support.PasswordState;

public class EDNAAccountStatus implements AccountStatus {
	protected PasswordState passwordState;
	protected Date passwordExpirationDate;
	protected Date lastPasswordChangeDate;
	protected PasswordResetFactorState passwordResetFactorState;
	
	public EDNAAccountStatus(PasswordState passwordState, Date passwordExpirationDate, Date lastPasswordChangeDate, PasswordResetFactorState passwordResetFactorState) {
		this.passwordState = passwordState;
		this.passwordExpirationDate = passwordExpirationDate;
		this.lastPasswordChangeDate = lastPasswordChangeDate;
		this.passwordResetFactorState = passwordResetFactorState;
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
	
	public PasswordResetFactorState getPasswordResetFactorState() {
		return passwordResetFactorState;
	}
	
	public static EDNAAccountStatus getAccountStatus(Map<String,Object> attributes) throws AccountStatusException {
		if (attributes == null || attributes.isEmpty()) {
			throw new AccountStatusException("account status attributes are missing");
		}
		
		int pwStateFlag = (attributes.get("passwordStateFlag") != null) ? ((Number) attributes.get("passwordStateFlag")).intValue() : 0; // null attr ==> "OK"
		Date pwExpirationDate = (Date) attributes.get("passwordExpirationDate");
		Date pwLastChangeDate = (Date) attributes.get("passwordLastChangeDate");
		
		int loginDiversionState = (attributes.get("loginDiversionState") != null) ? ((Number) attributes.get("loginDiversionState")).intValue() : -1; // null attr ==> "NOT_ENROLLED"
		
		PasswordState pwState = PasswordState.OK;
		PasswordResetFactorState pwResetFactorState = PasswordResetFactorState.NOT_ENROLLED;
		
		if (pwStateFlag == 2) {
			pwState = PasswordState.ADMIN_FORCED_CHANGE;
			
		} else if (pwStateFlag == 1) {
			
			Date now = new Date();
			
			if (pwExpirationDate == null) {
				pwState = PasswordState.UNKNOWN;
				
			} else if (now.before(pwExpirationDate)) {
				pwState = PasswordState.WARN;
				
			} else {
				pwState = PasswordState.EXPIRED;
			}
		}
		
		if (loginDiversionState == 0) {
			pwResetFactorState = PasswordResetFactorState.OK;
			
		} else if (loginDiversionState == 1) {
			pwResetFactorState = PasswordResetFactorState.WARN; 
			
		} else if (loginDiversionState == 2) {
			pwResetFactorState = PasswordResetFactorState.FORCE;
			
		} else if (loginDiversionState > 2) {
			// whatever state this is, it's unsupported
			pwResetFactorState = PasswordResetFactorState.OK;
		}
		
		return new EDNAAccountStatus(pwState, pwExpirationDate, pwLastChangeDate, pwResetFactorState);
	}
	
}