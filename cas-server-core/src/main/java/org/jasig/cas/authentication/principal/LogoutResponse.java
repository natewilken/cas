package org.jasig.cas.authentication.principal;

/**
 *
 * @author alwold
 * @author wilken
 */
public class LogoutResponse {
	private boolean loggedOut;
	private String sessionIdentifier;
	private String signoutUrl;
	private String postData;

	public LogoutResponse(boolean loggedOut) {
		this.loggedOut = loggedOut;
	}

	public LogoutResponse(boolean loggedOut, String sessionIdentifier, String signoutUrl, String postData) {
		this.loggedOut = loggedOut;
		this.sessionIdentifier = sessionIdentifier;
		this.signoutUrl = signoutUrl;
		this.postData = postData;
	}

	public boolean isLoggedOut() {
		return loggedOut;
	}

	public void setLoggedOut(boolean loggedOut) {
		this.loggedOut = loggedOut;
	}

	public void setSessionIdentifier(String sessionIdentifier) {
		this.sessionIdentifier = sessionIdentifier;
	}
	public String getSessionIdentifier() {
		return sessionIdentifier;
	}

	public String getSignoutUrl() {
		return signoutUrl;
	}

	/**
	 * Specify a URL that will need to be accessed to complete the signout process
	 * 
	 * @param signoutUrl
	 */
	 public void setSignoutUrl(String signoutUrl) {
		this.signoutUrl = signoutUrl;
	}

	public String getPostData() {
		return postData;
	}

	public void setPostData(String postData) {
		this.postData = postData;
	}

}
