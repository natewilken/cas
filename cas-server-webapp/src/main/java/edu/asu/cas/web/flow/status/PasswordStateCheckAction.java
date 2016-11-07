package edu.asu.cas.web.flow.status;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.jasig.cas.authentication.principal.WebApplicationService;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.AccountStatusException;
import edu.asu.cas.web.support.PasswordState;
import edu.asu.cas.web.support.edna.EDNAAccountStatus;
import edu.asu.cas.web.util.PasswordChangeReferral;

public class PasswordStateCheckAction extends AbstractAction {

	@NotNull
	protected String casLoginURL;
	
	@NotNull
	protected String passwordChangeBaseURL;
	
	@NotNull
	protected String passwordChangeReferralSecret;
	
	@Override
	protected Event doExecute(final RequestContext context) throws Exception {
		logger.trace("checking account status");
		
		Principal principal = (Principal)context.getFlowScope().get("principal");
		String serviceTicketId = context.getRequestScope().getString("serviceTicketId");
		logger.trace("serviceTicketId [" + serviceTicketId + "]");
		
		if (principal == null) {
			if (serviceTicketId == null) {
				logger.warn("principal and service ticket unavailable");
				return error();
			} else {
				logger.trace("not a login attempt; skipping password warning check");
				return success();
			}
		}
		
		try {
			AccountStatus status = EDNAAccountStatus.getAccountStatus(principal.getAttributes());
			PasswordState pwState = status.getPasswordState();
			
			if (pwState == PasswordState.OK || pwState == PasswordState.UNKNOWN) {
				logger.debug("password state [" + pwState + "] for principal [" + principal.getId() + "]");
				return success();
				
			} else {
				
				if (pwState == PasswordState.WARN) {
					logger.debug("password state [" + pwState + "], expiration [" + status.getPasswordExpirationDate() + "] for principal [" + principal.getId() + "]");
					context.getRequestScope().put("passwordDaysRemaining", new Integer(status.getPasswordDaysRemaining()));
					context.getRequestScope().put("passwordExpirationDate", status.getPasswordExpirationDate());
					context.getRequestScope().put("passwordLastChangeDate", status.getLastPasswordChangeDate());
					
					context.getFlowScope().put("ticketGrantingTicketId", WebUtils.getTicketGrantingTicketId(context));
					
					return result("warn");
					
				} else {
					// PasswordState.EXPIRED or PasswordState.ADMIN_FORCED_CHANGE
					logger.debug("password state [" + pwState + "] for principal [" + principal.getId() + "]");
					return result("expired");
				}
			}
			
		} catch (AccountStatusException e) {
			logger.error("account status lookup failed", e);
			return success(); // don't punish the user
		}
	}
	
	public String getPasswordChangeRedirectURL(RequestContext context, boolean forced)
	throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		
		UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)context.getFlowScope().get("credentials");
		String username = credentials.getUsername();
		
		WebApplicationService service = WebUtils.getService(context);
		String serviceURL = (service != null) ? service.getResponse(null).getUrl() : null; // sanitized
		
		String url = getRedirectURL(username, forced, serviceURL);
		logger.debug("redirect url [" + url + "] for username [" + username + "]");
		
		return url;
	}
	
	protected String getRedirectURL(String principal, boolean forced, String serviceURL)
	throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		
		StringBuilder relayURL = new StringBuilder(casLoginURL);
		if (serviceURL != null)
			relayURL.append((casLoginURL.contains("?") ? "&" : "?") + "service=" + URLEncoder.encode(serviceURL, "UTF-8"));
		
		PasswordChangeReferral referral = new PasswordChangeReferral(
				principal, forced, relayURL.toString(), System.currentTimeMillis(), passwordChangeReferralSecret);
		
		return passwordChangeBaseURL + (passwordChangeBaseURL.contains("?") ? "&" : "?")
				+ referral.getReferralQueryString();
	}

	public void setCasLoginURL(String casLoginURL) {
		this.casLoginURL = casLoginURL;
	}
	
	public void setPasswordChangeBaseURL(String passwordChangeBaseURL) {
		this.passwordChangeBaseURL = passwordChangeBaseURL;
	}
	
	public void setPasswordChangeReferralSecret(String passwordChangeReferralSecret) {
		this.passwordChangeReferralSecret = passwordChangeReferralSecret;
	}
	
}
