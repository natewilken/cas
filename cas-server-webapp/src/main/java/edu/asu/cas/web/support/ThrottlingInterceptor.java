package edu.asu.cas.web.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.webflow.execution.RequestContext;

public class ThrottlingInterceptor extends HandlerInterceptorAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(ThrottlingInterceptor.class);

	protected static final int DEFAULT_FAILURE_THRESHOLD = 25;
	protected static final int DEFAULT_FAILURE_RANGE_SECONDS = 600;
	protected static final int DEFAULT_LOCKOUT_PERIOD_SECONDS = 600;
	protected static final String DEFAULT_USERNAME_PARAMETER = "username";
	protected static final String SUCCESSFUL_AUTHENTICATION_EVENT = "success";

	protected final ConcurrentHashMap<ThrottleKey, AttackProfile> map = new ConcurrentHashMap<ThrottleKey, AttackProfile>();
	
	@Min(0)
	protected int failureThreshold = DEFAULT_FAILURE_THRESHOLD;

	@Min(0)
	protected int failureRangeSeconds = DEFAULT_FAILURE_RANGE_SECONDS;

	@Min(0)
	protected int lockoutPeriodSeconds = DEFAULT_LOCKOUT_PERIOD_SECONDS;

	@NotNull
	protected String usernameParameter = DEFAULT_USERNAME_PARAMETER;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (!"POST".equals(request.getMethod())) return true;

		ThrottleKey throttleKey = new ThrottleKey(request.getParameter(usernameParameter), request.getRemoteAddr());
		AttackProfile attackProfile = map.get(throttleKey);
		
		if (attackProfile != null && attackProfile.isLockedOut()) {
			logger.warn("Authentication attempt blocked for user [" + throttleKey.username + "] from [" + throttleKey.ipAddress + "]");
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
		
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
		if (!"POST".equals(request.getMethod())) return;

		RequestContext context = (RequestContext)request.getAttribute("flowRequestContext");
		if (context == null || context.getCurrentEvent() == null) return;

		ThrottleKey throttleKey = new ThrottleKey(request.getParameter(usernameParameter), request.getRemoteAddr());

		if (SUCCESSFUL_AUTHENTICATION_EVENT.equals(context.getCurrentEvent().getId())) {
			map.remove(throttleKey);
			return;
		}

		// this was an auth failure; queue it
		FailureIncident failure = new FailureIncident(
				request.getParameter(usernameParameter), request.getRemoteAddr(), request.getHeader("user-agent"));
		
		AttackProfile emptyAttackProfile = new AttackProfile(failureThreshold);
		AttackProfile attackProfile = map.putIfAbsent(throttleKey, emptyAttackProfile);
		if (attackProfile == null) attackProfile = emptyAttackProfile;
		
		attackProfile.add(failure);
	}
	
	public void vacuum() {
		logger.info("cleaning expired throttling data");
		for (ThrottleKey key : map.keySet()) {
			AttackProfile candidate = map.get(key);
			if (candidate.isEmpty()) {
				logger.debug("removing throttling data for key [" + key.toString() + "]");
				map.remove(key);
			}
		}
		logger.info("finished cleaning expired throttling data");
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

	static class ThrottleKey {
		final String username;
		final String ipAddress;

		public ThrottleKey(final String username, final String ipAddress) {
			this.username = username;
			this.ipAddress = ipAddress;
		}

		@Override
		public String toString() {
			return username + ", " + ipAddress;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
			result = prime * result + ((username == null) ? 0 : username.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ThrottleKey other = (ThrottleKey)obj;
			if (ipAddress == null) {
				if (other.ipAddress != null) return false;
			} else if (!ipAddress.equals(other.ipAddress)) return false;
			if (username == null) {
				if (other.username != null) return false;
			} else if (!username.equals(other.username)) return false;
			return true;
		}
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
				if (failureQueue.poll() == null) break;
			}
		}
		
		protected void cleanLockouts() {
			while (true) {
				if (lockoutQueue.poll() == null) break;
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
				logger.warn("Possible attack from [" + failure.ipAddress + "] for user ["
						+ failure.username + "]; user-agent: " + failure.userAgent);
				
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
	}

}
