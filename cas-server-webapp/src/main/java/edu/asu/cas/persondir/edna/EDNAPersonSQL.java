package edu.asu.cas.persondir.edna;

import org.apache.commons.io.IOUtils;

public enum EDNAPersonSQL {

	SELECT_PERSON ("/edu/asu/cas/persondir/edna/EDNASelectPerson.sql");
	
	private final String sql;
	
	private EDNAPersonSQL(String resourceName) {
		try {
			sql = IOUtils.toString(EDNAPersonSQL.class.getResourceAsStream(resourceName));
		} catch (Exception e) {
			throw new RuntimeException("couldn't load SQL from \"" + resourceName + "\"", e);
		}
	}
	
	public String toString() {
		return sql;
	}
}
