package edu.asu.cas.web.flow.status;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.jasig.cas.authentication.principal.WebApplicationService;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

import edu.asu.cas.web.support.AccountStatus;
import edu.asu.cas.web.support.AccountStatusException;
import edu.asu.cas.web.support.AccountStatusRegistry;
import edu.asu.cas.web.support.PasswordState;
import edu.asu.cas.web.util.PasswordChangeReferral;

public class AccountStatusCheckAction extends AbstractAction {

	@NotNull
	protected AccountStatusRegistry accountStatusRegistry;
	
	@NotNull
	protected String casLoginURL;
	
	@NotNull
	protected String passwordChangeBaseURL;
	
	@NotNull
	protected String passwordChangeReferralSecret;
	
	@Override
	protected Event doExecute(final RequestContext context) throws Exception {
		logger.debug("checking account status");
		
		UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)context.getFlowScope().get("credentials");
		String username = credentials.getUsername();
		logger.trace("username: " + username);
		
		String serviceTicketId = context.getRequestScope().getString("serviceTicketId");
		logger.trace("serviceTicketId: " + serviceTicketId);
		
		if (username == null) {
			if (serviceTicketId == null) {
				logger.warn("principal and service ticket unavailable");
				return error();
			} else {
				logger.debug("not a login attempt; skipping password warning check");
				return success();
			}
		}
		
		try {
			AccountStatus status = accountStatusRegistry.getAccountStatus(username);
			PasswordState pwState = status.getPasswordState();
			
			if (pwState == PasswordState.OK) {
				logger.trace("password state: " + pwState);
				return success();
				
			} else {
				
				if (pwState == PasswordState.WARN) {
					logger.trace("password state: " + pwState + ", expiration: " + status.getPasswordExpirationDate());
					context.getRequestScope().put("passwordDaysRemaining", new Integer(status.getPasswordDaysRemaining()));
					context.getFlowScope().put("ticketGrantingTicketId", WebUtils.getTicketGrantingTicketId(context));
					
					return result("warn");
					
				} else {
					// PasswordState.EXPIRED or PasswordState.ADMIN_FORCED_CHANGE
					logger.trace("password state: " + pwState);
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
		String serviceURL = service.getResponse(null).getUrl(); // sanitized
		
		String url = getRedirectURL(username, forced, serviceURL);
		logger.trace("redirect url: " + url);
		
		return url;
	}
	
	protected String getRedirectURL(String principal, boolean forced, String serviceURL)
	throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
		
		String relayURL = casLoginURL + (casLoginURL.contains("?") ? "&" : "?") + "service=" + URLEncoder.encode(serviceURL, "UTF-8");
		
		PasswordChangeReferral referral = new PasswordChangeReferral(
				principal, forced, relayURL, System.currentTimeMillis(), passwordChangeReferralSecret);
		
		return passwordChangeBaseURL + (passwordChangeBaseURL.contains("?") ? "&" : "?")
		+ referral.getReferralQueryString();
	}

	public void setAccountStatusRegistry(final AccountStatusRegistry accountStatusRegistry) {
		this.accountStatusRegistry = accountStatusRegistry;
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
