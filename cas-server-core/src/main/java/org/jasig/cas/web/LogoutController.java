/*
 * Copyright 2007 The JA-SIG Collaborative. All rights reserved. See license
 * distributed with this file and available online at
 * http://www.ja-sig.org/products/cas/overview/license/
 */
package org.jasig.cas.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.constraints.NotNull;

import org.jasig.cas.CentralAuthenticationService;
import org.jasig.cas.authentication.principal.LogoutResponse;
import org.jasig.cas.web.support.CookieRetrievingCookieGenerator;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller to delete ticket granting ticket cookie in order to log out of
 * single sign on. This controller implements the idea of the ESUP Portail's
 * Logout patch to allow for redirecting to a url on logout. It also exposes a
 * log out link to the view via the WebConstants.LOGOUT constant.
 * 
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @since 3.0
 */
public final class LogoutController extends AbstractController {

    /** The CORE to which we delegate for all CAS functionality. */
    @NotNull
    private CentralAuthenticationService centralAuthenticationService;

    /** CookieGenerator for TGT Cookie */
    @NotNull
    private CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    /** CookieGenerator for Warn Cookie */
    @NotNull
    private CookieRetrievingCookieGenerator warnCookieGenerator;

    /** Logout view name. */
    @NotNull
    private String logoutView;

    /**
     * Boolean to determine if we will redirect to any url provided in the
     * service request parameter.
     */
    private boolean followServiceRedirects;
    
    public LogoutController() {
        setCacheSeconds(0);
    }

    protected ModelAndView handleRequestInternal(
        final HttpServletRequest request, final HttpServletResponse response)
        throws Exception {
        final String ticketGrantingTicketId = this.ticketGrantingTicketCookieGenerator.retrieveCookieValue(request);
        final String service = request.getParameter("service");

        List<LogoutResponse> logoutResponses = null;
        List<String> logoutResponseKeys = new ArrayList<String>();

        boolean foundGoogleSession = false;
        
        if (ticketGrantingTicketId != null) {
            logoutResponses = this.centralAuthenticationService
                .destroyTicketGrantingTicket(ticketGrantingTicketId);
            
            if (!logoutResponses.isEmpty()) {
            	HttpSession session = request.getSession(true);
            	
            	for (LogoutResponse logoutResponse : logoutResponses) {
            		String id = logoutResponse.getSessionIdentifier();
            		if (id != null && !id.matches("\\s*")) {
            			session.setAttribute("logoutResponseKey." + id, logoutResponse);
            			logoutResponseKeys.add(id);
            		}
            		if (logoutResponse.getSignoutUrl().contains("/google-sso/Authn?")) {
            			foundGoogleSession = true;
            		}
            	}
            }
            
            this.ticketGrantingTicketCookieGenerator.removeCookie(response);
            this.warnCookieGenerator.removeCookie(response);
        }

        Map<String,Object> model = new HashMap<String,Object>();
        model.put("logoutResponseKeys", logoutResponseKeys);
        model.put("foundGoogleSession", foundGoogleSession);

        if (this.followServiceRedirects && service != null) {
            return new ModelAndView(new RedirectView(service));
        }

        return new ModelAndView(this.logoutView, model);
    }

    public void setTicketGrantingTicketCookieGenerator(
        final CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator) {
        this.ticketGrantingTicketCookieGenerator = ticketGrantingTicketCookieGenerator;
    }

    public void setWarnCookieGenerator(final CookieRetrievingCookieGenerator warnCookieGenerator) {
        this.warnCookieGenerator = warnCookieGenerator;
    }

    /**
     * @param centralAuthenticationService The centralAuthenticationService to
     * set.
     */
    public void setCentralAuthenticationService(
        final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    public void setFollowServiceRedirects(final boolean followServiceRedirects) {
        this.followServiceRedirects = followServiceRedirects;
    }

    public void setLogoutView(final String logoutView) {
        this.logoutView = logoutView;
    }
}
