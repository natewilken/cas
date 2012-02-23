package edu.asu.cas.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.cas.authentication.principal.Service;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServicesManager;
import org.jasig.cas.web.support.ArgumentExtractor;
import org.jasig.cas.web.support.WebUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.theme.AbstractThemeResolver;

import com.handinteractive.mobile.UAgentInfo;

public class HttpHeaderOverrideThemeResolver extends AbstractThemeResolver implements InitializingBean {

    private final Log logger = LogFactory.getLog(this.getClass());
    private ServicesManager servicesManager;
    private List<ArgumentExtractor> argumentExtractors;

    public void afterPropertiesSet() throws Exception {
    	logger.trace("afterPropertiesSet()");
    }

    public String resolveThemeName(final HttpServletRequest request) {
        logger.debug("Attempting to resolve theme");
        
        String serviceTheme = resolveServiceThemeName(request);
        if (serviceTheme != null) {
        	logger.debug("Resolved service theme \"" + serviceTheme + "\"");
        	return serviceTheme;
        }
        
        final String userAgentHeader = request.getHeader("User-Agent");
        logger.trace("User-Agent: " + userAgentHeader);
        
        final String acceptHeader = request.getHeader("Accept");
        logger.trace("Accept: " + acceptHeader);
        
        final UAgentInfo uAgentInfo = new UAgentInfo(userAgentHeader, acceptHeader);
        if (uAgentInfo.detectMobileQuick()) {
        	logger.debug("Resolved theme \"mobile\"");
        	return "mobile";
        }
        
        String defaultTheme = getDefaultThemeName();
        logger.debug("Resolved default theme \"" + defaultTheme + "\"");
        return defaultTheme;
    }

    public String resolveServiceThemeName(final HttpServletRequest request) {
        if (this.servicesManager == null) {
            return null;
        }

        final Service service = WebUtils.getService(this.argumentExtractors, request);

        final RegisteredService rService = this.servicesManager.findServiceBy(service);

        return service != null && rService != null && StringUtils.hasText(rService.getTheme())
        	? rService.getTheme() : null;
     }
    
    public void setThemeName(final HttpServletRequest request, final HttpServletResponse response, final String themeName) {}

    public void setServicesManager(final ServicesManager servicesManager) {
        this.servicesManager = servicesManager;
    }

    public void setArgumentExtractors(final List<ArgumentExtractor> argumentExtractors) {
        this.argumentExtractors = argumentExtractors;
    }
}
