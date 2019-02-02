package org.eclipse.rdf4j.sail.shacl;

public class ShaclSailConfig {

	boolean parallelValidation = true;
	boolean undefinedTargetValidatesAllSubjects = false;
	boolean logValidationPlans = false;
	boolean logValidationViolations = false;
	boolean ignoreNoShapesLoadedException = false;
	boolean validationEnabled = true;
	boolean cacheSelectNodes = true;

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

	public boolean isParallelValidation() {
		return parallelValidation;
	}

	public boolean isCacheSelectNodes() {
		return cacheSelectNodes;
	}
}
