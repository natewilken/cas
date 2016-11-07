package edu.asu.cas.web.flow.throttle;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;


public class LoginThrottlerAction {

	private static final Logger logger = LoggerFactory.getLogger(LoginThrottlerAction.class);

	protected static final String SUCCESSFUL_AUTHENTICATION_EVENT = "success";

	@NotNull
	protected LoginThrottler throttler;
	
	public boolean isLockedOut(final RequestContext requestContext) throws Exception {
		if (throttler.isEnabled()) {
			ThrottleContext throttleContext = throttler.getThrottleContext(requestContext);
			
			if (throttler.isLockedOut(throttleContext)) {
				logger.warn("auth attempt blocked for [" + throttleContext + "]");
				
				return true;
			}
		}
		
		return false;
	}
	
	public String postSubmit(final RequestContext requestContext) throws Exception {
		if (throttler.isEnabled()) {
			ThrottleContext throttleContext = throttler.getThrottleContext(requestContext);
			
			if (SUCCESSFUL_AUTHENTICATION_EVENT.equals(requestContext.getCurrentEvent().getId())) {
				logger.debug("auth was successful for [" + throttleContext + "]; clearing failures");
				throttler.clearFailures(throttleContext);
				
				return "success";
				
			} else {
				logger.debug("auth failed; registering failure for [" + throttleContext + "]");
				throttler.registerFailure(throttleContext, requestContext);
				
				if (throttler.isLockedOut(throttleContext)) {
					return "lockout";
					
				} else {
					return "error";
				}
			}
		}
		
		return requestContext.getCurrentEvent().getId();
	}
	
	public void setLoginThrottler(final LoginThrottler throttler) {
		this.throttler = throttler;
	}

}
