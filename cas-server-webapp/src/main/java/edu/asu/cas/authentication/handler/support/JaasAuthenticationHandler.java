package edu.asu.cas.authentication.handler.support;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.handler.support.AbstractUsernamePasswordAuthenticationHandler;
import org.jasig.cas.authentication.principal.UsernamePasswordCredentials;
import org.springframework.util.Assert;

public class JaasAuthenticationHandler extends AbstractUsernamePasswordAuthenticationHandler {
    private static final String DEFAULT_REALM = "CAS";

    private String realm = DEFAULT_REALM;
    
    public JaasAuthenticationHandler() {
        Assert.notNull(Configuration.getConfiguration(), "Static Configuration cannot be null. Did you remember to specify \"java.security.auth.login.config\"?");
    }

    protected final boolean authenticateUsernamePasswordInternal(final UsernamePasswordCredentials credentials) throws AuthenticationException {
        final String transformedUsername = getPrincipalNameTransformer().transform(credentials.getUsername());

        try {
            final LoginContext lc = new LoginContext(this.realm,
                new UsernamePasswordCallbackHandler(transformedUsername, credentials.getPassword()));

            lc.login();
            lc.logout();
            
        } catch (LoginException le) {
        	log.debug("authentication failed for \"" + transformedUsername + "\": " + le.getMessage());
        	Throwable cause = le.getCause();
        	if (cause != null) {
        		log.trace("caused by: " + cause, cause);
        	}
            return false;
        }
        
        return true;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    protected static final class UsernamePasswordCallbackHandler implements CallbackHandler {
        private final String username;
        private final String password;

        protected UsernamePasswordCallbackHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public void handle(final Callback[] callbacks) throws UnsupportedCallbackException {
            for (Callback callback : callbacks) {
            	if (callback instanceof NameCallback) {
            		((NameCallback)callback).setName(username);
            		
            	} else if (callback instanceof PasswordCallback) {
            		((PasswordCallback)callback).setPassword(this.password.toCharArray());
            		
            	} else {
            		throw new UnsupportedCallbackException(callback, "unrecognized callback");
            	}
            }
        }
    }
    
}
