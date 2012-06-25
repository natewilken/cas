package edu.asu.cas.web.flow;

import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.webflow.action.AbstractAction;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

public class SendSSONameCookieAction extends AbstractAction {

	private static Logger logger = Logger.getLogger(SendSSONameCookieAction.class);
	
	@Override
	protected Event doExecute(RequestContext context) throws Exception {
		String ssoName = null;
		
		try {
			Principal principal = (Principal)context.getFlowScope().get("principal");
			if (principal != null) {
				logger.trace("Principal found: " + principal.getId());
				Map<String,Object> attributes = principal.getAttributes();
				if (attributes != null) {
					ssoName = (String)attributes.get("givenName");
					if (ssoName == null) logger.warn("no givenName found for principal " + principal.getId());
				}
			}
			
			if (ssoName == null) {
				ssoName = getUsername(context);
			}
			
		} catch (Throwable t) {
			logger.warn("unable to determine SSONAME", t);
		}
		
		if (ssoName != null) {
			addCookie(WebUtils.getHttpServletRequest(context), WebUtils.getHttpServletResponse(context), ssoName);
		}
		
		return success();
	}

	protected String getUsername(RequestContext context) {
		UsernamePasswordCredentials credentials = (UsernamePasswordCredentials)context.getFlowScope().get("credentials");
		if (credentials != null) return credentials.getUsername();
		
		return null;
	}
	
	protected void addCookie(HttpServletRequest request, HttpServletResponse response, String ssoName) {
		try {
			Cookie cookie = new Cookie("SSONAME", ssoName);
			cookie.setMaxAge(-1);
			cookie.setDomain(".asu.edu");
			cookie.setPath("/");
			
			response.addCookie(cookie);
			if (logger.isTraceEnabled()) logger.trace("set-cookie: " + formatCookieString(cookie));
			
		} catch (Throwable t) {
			logger.warn("unable to add SSONAME cookie", t);
		}
	}
	
	protected static String formatCookieString(Cookie cookie) {
		StringBuilder buffer = new StringBuilder(cookie.getName() + "=" + cookie.getValue());
		if (cookie.getDomain() != null) buffer.append("; Domain=" + cookie.getDomain());
		buffer.append("; Path=" + cookie.getPath());
		if (cookie.getSecure()) buffer.append("; Secure");
		
		return buffer.toString();
	}

}
