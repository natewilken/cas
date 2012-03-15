package edu.asu.cas.web.flow.throttle;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.jasig.cas.web.support.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.webflow.execution.RequestContext;


public class LoginThrottler {
	
	private static final Logger logger = LoggerFactory.getLogger(LoginThrottler.class);

	protected static final int DEFAULT_FAILURE_THRESHOLD = 25;
	protected static final int DEFAULT_FAILURE_RANGE_SECONDS = 600;
	protected static final int DEFAULT_LOCKOUT_PERIOD_SECONDS = 600;
	protected static final String DEFAULT_USERNAME_PARAMETER = "username";
	
	protected final ConcurrentHashMap<ThrottleContext, AttackProfile> map = new ConcurrentHashMap<ThrottleContext, AttackProfile>();
	
	@Min(0)
	protected int failureThreshold = DEFAULT_FAILURE_THRESHOLD;

	@Min(0)
	protected int failureRangeSeconds = DEFAULT_FAILURE_RANGE_SECONDS;

	@Min(0)
	protected int lockoutPeriodSeconds = DEFAULT_LOCKOUT_PERIOD_SECONDS;

	@NotNull
	protected String usernameParameter = DEFAULT_USERNAME_PARAMETER;
	
	public ThrottleContext getThrottleContext(final RequestContext requestContext) {
		HttpServletRequest request = WebUtils.getHttpServletRequest(requestContext);
		return new UsernameAndIpAddressThrottleContext(request.getParameter(usernameParameter), request.getRemoteAddr());
	}
	
	public boolean isLockedOut(final ThrottleContext throttleContext) {
		AttackProfile attackProfile = map.get(throttleContext);
		if (logger.isTraceEnabled()) {
			logger.trace("throttle context [" + throttleContext + "] failures: " + (attackProfile == null || attackProfile.isEmpty() ? "empty" : attackProfile.failureQueue.size()));
		}
		
		if (attackProfile != null && attackProfile.isLockedOut()) {
			return true;
		}
		
		return false;
	}
	
	public void registerFailure(final ThrottleContext throttleContext, final RequestContext requestContext) {
		HttpServletRequest request = WebUtils.getHttpServletRequest(requestContext);
		
		FailureIncident failure = new FailureIncident(
				request.getParameter(usernameParameter), request.getRemoteAddr(), request.getHeader("user-agent"));
		
		AttackProfile emptyAttackProfile = new AttackProfile(failureThreshold);
		AttackProfile attackProfile = map.putIfAbsent(throttleContext, emptyAttackProfile);
		if (attackProfile == null) attackProfile = emptyAttackProfile;
		
		attackProfile.add(failure);
		
		if (logger.isTraceEnabled()) {
			logger.trace("throttle context [" + throttleContext + "] failures: " + attackProfile.failureQueue.size());
		}
	}
	
	public void clearFailures(final ThrottleContext throttleContext) {
		map.remove(throttleContext);
	}
	
	public void vacuum() {
		logger.info("cleaning expired throttling data...");
		for (ThrottleContext key : map.keySet()) {
			AttackProfile candidate = map.get(key);
			if (candidate.isEmpty()) {
				logger.debug("removing throttling data for [" + key + "]");
				map.remove(key);
			}
		}
		logger.info("finished cleaning expired throttling data; remaining attack profile count: " + map.keySet().size());
	}

	public void setFailureThreshold(final int failureThreshold) {
		this.failureThreshold = failureThreshold;
	}

	public void setFailureRangeSeconds(final int failureRangeSeconds) {
		this.failureRangeSeconds = failureRangeSeconds;
	}

	public void setLockoutPeriodSeconds(final int lockoutPeriodSeconds) {
		this.lockoutPeriodSeconds = lockoutPeriodSeconds;
	}

	public void setUsernameParameter(final String usernameParameter) {
		this.usernameParameter = usernameParameter;
	}

	class AttackProfile {
		final DelayQueue<FailureIncident> failureQueue = new DelayQueue<FailureIncident>();
		final DelayQueue<Lockout> lockoutQueue = new DelayQueue<Lockout>();
		final int failureThreshold;
		
		public AttackProfile(final int failureThreshold) {
			this.failureThreshold = failureThreshold;
		}
		
		protected void cleanFailures() {
			while (true) {
				FailureIncident expiredIncident = failureQueue.poll();
				if (expiredIncident == null) break;
				logger.trace("removed expired failure incident [" + expiredIncident + "]");
			}
		}
		
		protected void cleanLockouts() {
			while (true) {
				Lockout expiredLockout = lockoutQueue.poll();
				if (expiredLockout == null) break;
				logger.trace("removed expired lockout [" + expiredLockout + "]");
			}
		}
		
		public boolean isEmpty() {
			cleanFailures();
			cleanLockouts();
			return (failureQueue.size() == 0) && (lockoutQueue.size() == 0);
		}
		
		public boolean isLockedOut() {
			cleanLockouts();
			return lockoutQueue.size() > 0;
		}
		
		public void add(FailureIncident failure) {
			failureQueue.offer(failure);
			cleanFailures();
			
			if (failureQueue.size() >= failureThreshold) {
				logger.warn("Possible attack: [" + failure + "]; user-agent: " + failure.userAgent);
				
				lockoutQueue.offer(new Lockout());
			}
		}
	}
	
	static class ExpiringEvent implements Delayed {
		final long eventTimeMillis;
		final long delayMillis;
		
		protected ExpiringEvent(long eventTimeMillis, long delayMillis) {
			this.eventTimeMillis = eventTimeMillis;
			this.delayMillis = delayMillis;
		}

		public int compareTo(Delayed other) {
			if (other instanceof ExpiringEvent) {
				return new Long(eventTimeMillis).compareTo(((ExpiringEvent)other).eventTimeMillis);
			}
			return new Long(getDelay(TimeUnit.NANOSECONDS)).compareTo(other.getDelay(TimeUnit.NANOSECONDS));
		}

		public long getDelay(TimeUnit unit) {
			long elapsedMillis = System.currentTimeMillis() - eventTimeMillis;
			return unit.convert(delayMillis - elapsedMillis, TimeUnit.MILLISECONDS);
		}
	}
	
	class Lockout extends ExpiringEvent {
		public Lockout() {
			super(System.currentTimeMillis(), lockoutPeriodSeconds * 1000);
		}
		
		@Override
		public String toString() {
			return super.eventTimeMillis + "ms, " + super.delayMillis + "ms";
		}
	}
	
	class FailureIncident extends ExpiringEvent {
		final String username;
		final String ipAddress;
		final String userAgent;

		public FailureIncident(final String username, final String ipAddress, final String userAgent) {
			super(System.currentTimeMillis(), failureRangeSeconds * 1000);
			this.username = username;
			this.ipAddress = ipAddress;
			this.userAgent = userAgent;
		}
		
		@Override
		public String toString() {
			return username + ", " + ipAddress;
		}
	}

}
