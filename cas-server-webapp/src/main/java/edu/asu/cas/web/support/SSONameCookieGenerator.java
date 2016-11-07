package edu.asu.cas.web.support;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.util.CookieGenerator;

public class SSONameCookieGenerator extends CookieGenerator {
	
	private static Logger logger = Logger.getLogger(SSONameCookieGenerator.class);

	@Override
	public void addCookie(HttpServletResponse response, String ssoName) {
		Cookie cookie = createCookie(ssoName);
		
		Integer maxAge = getCookieMaxAge();
		if (maxAge != null) cookie.setMaxAge(maxAge);
		
		if (isCookieSecure()) cookie.setSecure(true);
		
		response.addCookie(cookie);
		if (logger.isDebugEnabled()) logger.debug("set-cookie: " + formatCookieString(cookie));
	}
	
	protected static String formatCookieString(Cookie cookie) {
		StringBuilder buffer = new StringBuilder(cookie.getName() + "=" + cookie.getValue());
		if (cookie.getDomain() != null) buffer.append("; Domain=" + cookie.getDomain());
		buffer.append("; Path=" + cookie.getPath());
		if (cookie.getSecure()) buffer.append("; Secure");
		
		return buffer.toString();
	}

}
