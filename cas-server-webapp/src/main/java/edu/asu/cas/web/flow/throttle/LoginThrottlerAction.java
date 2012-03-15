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
	
	public String preSubmit(final RequestContext requestContext) throws Exception {
		ThrottleContext throttleContext = throttler.getThrottleContext(requestContext);
		if (throttler.isLockedOut(throttleContext)) {
			logger.warn("Authentication attempt blocked for [" + throttleContext + "]");
			return "lockout";
		}
		
		return "success";
	}
	
	public String postSubmit(final RequestContext requestContext) throws Exception {
		ThrottleContext throttleContext = throttler.getThrottleContext(requestContext);
		
		if (SUCCESSFUL_AUTHENTICATION_EVENT.equals(requestContext.getCurrentEvent().getId())) {
			logger.trace("auth was successful for [" + throttleContext + "]; clearing failures");
			throttler.clearFailures(throttleContext);
			return "success";
			
		} else {
			logger.trace("auth failed; registering failure for [" + throttleContext + "]");
			throttler.registerFailure(throttleContext, requestContext);
			
			if (throttler.isLockedOut(throttleContext)) {
				return "lockout";
			} else {
				return "error";
			}
		}
	}
	
	public void setLoginThrottler(final LoginThrottler throttler) {
		this.throttler = throttler;
	}

}
