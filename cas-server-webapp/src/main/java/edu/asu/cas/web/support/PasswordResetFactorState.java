package edu.asu.cas.web.support;

public enum PasswordResetFactorState {
	NOT_ENROLLED, // no record
	OK,
	WARN,         // request action
	FORCE         // require action
}
