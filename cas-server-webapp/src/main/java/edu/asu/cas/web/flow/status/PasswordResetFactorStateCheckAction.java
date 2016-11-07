package edu.asu.cas.web.flow.status;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

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
import edu.asu.cas.web.support.PasswordResetFactorState;
import edu.asu.cas.web.support.edna.EDNAAccountStatus;

public class PasswordResetFactorStateCheckAction extends AbstractAction {

	@NotNull
	protected String casLoginURL;
	
	@NotNull
	protected String passwordResetFactorEnrollmentBaseURL;
	
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
				logger.trace("not a login attempt; skipping password reset factor check");
				return success();
			}
		}
		
		if (logger.isTraceEnabled()) {
			for (Map.Entry<String, ?> entry : principal.getAttributes().entrySet()) {
				logger.trace(entry.getKey() + " [" + entry.getValue().toString() + "]");
			}
		}
		
		try {
			AccountStatus status = EDNAAccountStatus.getAccountStatus(principal.getAttributes());
			PasswordResetFactorState pwResetFactorState = status.getPasswordResetFactorState();
			
			logger.debug("password reset factor state [" + pwResetFactorState + "] for principal [" + principal.getId() + "]");
			
			if (pwResetFactorState == PasswordResetFactorState.OK || pwResetFactorState == PasswordResetFactorState.NOT_ENROLLED) {
				return success();
				
			} else {
				return result("divert");
			}
			
		} catch (AccountStatusException e) {
			logger.error("account status lookup failed", e);
			return success(); // don't punish the user
		}
	}
	
	public String getPasswordResetFactorEnrollmentRedirectURL(RequestContext context) throws UnsupportedEncodingException {
		WebApplicationService service = WebUtils.getService(context);
		String serviceURL = (service != null) ? service.getResponse(null).getUrl() : null; // sanitized
		
		String url = getRedirectURL(serviceURL);
		
		if (logger.isDebugEnabled()) {
			UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)context.getFlowScope().get("credentials");
			logger.debug("redirect url [" + url + "] for username [" + credentials.getUsername() + "]");
		}
		
		return url;
	}
	
	protected String getRedirectURL(String serviceURL) throws UnsupportedEncodingException {
		StringBuilder relayURL = new StringBuilder(casLoginURL);
		if (serviceURL != null) {
			relayURL.append((casLoginURL.contains("?") ? "&" : "?") + "service=" + URLEncoder.encode(serviceURL, "UTF-8"));
		}
		
		return passwordResetFactorEnrollmentBaseURL + (passwordResetFactorEnrollmentBaseURL.contains("?") ? "&" : "?")
				+ "service=" + URLEncoder.encode(relayURL.toString(), "UTF-8");
	}

	public void setCasLoginURL(String casLoginURL) {
		this.casLoginURL = casLoginURL;
	}
	
	public void setPasswordResetFactorEnrollmentBaseURL(String passwordResetFactorEnrollmentBaseURL) {
		this.passwordResetFactorEnrollmentBaseURL = passwordResetFactorEnrollmentBaseURL;
	}
	
}
