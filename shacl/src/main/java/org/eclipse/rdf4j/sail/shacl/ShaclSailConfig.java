package org.eclipse.rdf4j.sail.shacl;

public class ShaclSailConfig {

	boolean undefinedTargetValidatesAllSubjects = false;
	boolean logValidationPlans = false;
	boolean logValidationViolations = false;
	boolean ignoreNoShapesLoadedException = false;
	boolean validationEnabled = true;

	public boolean isUndefinedTargetValidatesAllSubjects() {
		return undefinedTargetValidatesAllSubjects;
	}

	public boolean isLogValidationPlans() {
		return logValidationPlans;
	}

	public boolean isLogValidationViolations() {
		return logValidationViolations;
	}

	public boolean isIgnoreNoShapesLoadedException() {
		return ignoreNoShapesLoadedException;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}
}
