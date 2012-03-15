package org.jasig.cas.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;

import org.jasig.cas.authentication.principal.LogoutResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * @author wilken@asu.edu
 */
public final class LogoutCallbackController extends AbstractController {

    /** Logout Callback view name. */
    @NotNull
    private String logoutCallbackView;

    public LogoutCallbackController() {
        setCacheSeconds(0);
    }

    protected ModelAndView handleRequestInternal(final HttpServletRequest request, final HttpServletResponse response)
    throws Exception {

    	String logoutResponseKey = request.getParameter("k");
    	HttpSession session = request.getSession(false);

    	Map<String,LogoutResponse> model = null;

    	if (session != null && logoutResponseKey != null) {
    		LogoutResponse logoutResponse = (LogoutResponse)session.getAttribute("logoutResponseKey." + logoutResponseKey);
    		if (logoutResponse != null) {
    			model = new HashMap<String,LogoutResponse>();
    			model.put("logoutResponse", logoutResponse);
    		}
    	}

    	return new ModelAndView(this.logoutCallbackView, model);
    }

    public void setLogoutCallbackView(final String logoutCallbackView) {
    	this.logoutCallbackView = logoutCallbackView;
    }

}
