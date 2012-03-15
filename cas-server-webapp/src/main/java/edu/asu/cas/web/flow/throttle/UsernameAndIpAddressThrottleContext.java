package edu.asu.cas.web.flow.throttle;

public class UsernameAndIpAddressThrottleContext extends ThrottleContext {
	final String username;
	final String ipAddress;

	public UsernameAndIpAddressThrottleContext(final String username, final String ipAddress) {
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
		UsernameAndIpAddressThrottleContext other = (UsernameAndIpAddressThrottleContext)obj;
		if (ipAddress == null) {
			if (other.ipAddress != null) return false;
		} else if (!ipAddress.equals(other.ipAddress)) return false;
		if (username == null) {
			if (other.username != null) return false;
		} else if (!username.equals(other.username)) return false;
		return true;
	}
	
}
