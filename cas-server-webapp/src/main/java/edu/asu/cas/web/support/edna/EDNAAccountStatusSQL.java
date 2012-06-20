package edu.asu.cas.web.support.edna;

import org.apache.commons.io.IOUtils;

public enum EDNAAccountStatusSQL {

	SELECT_ACCOUNT_STATUS ("/edu/asu/cas/web/support/edna/EDNASelectAccountStatus.sql");
	
	private final String sql;
	
	private EDNAAccountStatusSQL(String resourceName) {
		try {
			sql = IOUtils.toString(EDNAAccountStatusSQL.class.getResourceAsStream(resourceName));
		} catch (Exception e) {
			throw new RuntimeException("couldn't load SQL from \"" + resourceName + "\"", e);
		}
	}
	
	public String toString() {
		return sql;
	}
}
