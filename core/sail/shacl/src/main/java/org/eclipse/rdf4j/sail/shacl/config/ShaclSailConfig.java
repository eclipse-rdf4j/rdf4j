/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.CACHE_SELECT_NODES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.GLOBAL_LOG_VALIDATION_EXECUTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.IGNORE_NO_SHAPES_LOADED_EXCEPTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_PLANS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_VIOLATIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.NAMESPACE;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PARALLEL_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PERFORMANCE_LOGGING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.RDFS_SUB_CLASS_REASONING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.SERIALIZABLE_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_ENABLED;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/**
 * A {@link SailImplConfig} for {@link ShaclSail} configuration.
 *
 * @author Jeen Broekstra
 */
public class ShaclSailConfig extends AbstractDelegatingSailImplConfig {

	public static final boolean PARALLEL_VALIDATION_DEFAULT = true;
	public static final boolean UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS_DEFAULT = false;
	public static final boolean LOG_VALIDATION_PLANS_DEFAULT = false;
	public static final boolean LOG_VALIDATION_VIOLATIONS_DEFAULT = false;
	public static final boolean IGNORE_NO_SHAPES_LOADED_EXCEPTION_DEFAULT = false;
	public static final boolean VALIDATION_ENABLED_DEFAULT = true;
	public static final boolean CACHE_SELECT_NODES_DEFAULT = true;
	public static final boolean GLOBAL_LOG_VALIDATION_EXECUTION_DEFAULT = false;
	public static final boolean RDFS_SUB_CLASS_REASONING_DEFAULT = true;
	public static final boolean PERFORMANCE_LOGGING_DEFAULT = false;
	public static final boolean SERIALIZABLE_VALIDATION_DEFAULT = true;
	public static final boolean ECLIPSE_RDF4J_SHACL_EXTENSIONS_DEFAULT = false;
	public static final boolean DASH_DATA_SHAPES_DEFAULT = false;
	public final static long VALIDATION_RESULTS_LIMIT_TOTAL_DEFAULT = 1_000_000;
	public final static long VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT_DEFAULT = 1_000;

	private boolean parallelValidation = PARALLEL_VALIDATION_DEFAULT;
	private boolean undefinedTargetValidatesAllSubjects = UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS_DEFAULT;
	private boolean logValidationPlans = LOG_VALIDATION_PLANS_DEFAULT;
	private boolean logValidationViolations = LOG_VALIDATION_VIOLATIONS_DEFAULT;
	private boolean ignoreNoShapesLoadedException = IGNORE_NO_SHAPES_LOADED_EXCEPTION_DEFAULT;
	private boolean validationEnabled = VALIDATION_ENABLED_DEFAULT;
	private boolean cacheSelectNodes = CACHE_SELECT_NODES_DEFAULT;
	private boolean globalLogValidationExecution = GLOBAL_LOG_VALIDATION_EXECUTION_DEFAULT;
	private boolean rdfsSubClassReasoning = RDFS_SUB_CLASS_REASONING_DEFAULT;
	private boolean performanceLogging = PERFORMANCE_LOGGING_DEFAULT;
	private boolean serializableValidation = SERIALIZABLE_VALIDATION_DEFAULT;
	private boolean eclipseRdf4jShaclExtensions = ECLIPSE_RDF4J_SHACL_EXTENSIONS_DEFAULT;
	private boolean dashDataShapes = DASH_DATA_SHAPES_DEFAULT;
	private long validationResultsLimitTotal = VALIDATION_RESULTS_LIMIT_TOTAL_DEFAULT;
	private long validationResultsLimitPerConstraint = VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT_DEFAULT;

	public ShaclSailConfig() {
		super(ShaclSailFactory.SAIL_TYPE);
	}

	public ShaclSailConfig(SailImplConfig delegate) {
		super(ShaclSailFactory.SAIL_TYPE, delegate);
	}

	@Deprecated
	public boolean isUndefinedTargetValidatesAllSubjects() {
		return undefinedTargetValidatesAllSubjects;
	}

	@Deprecated
	public void setUndefinedTargetValidatesAllSubjects(boolean undefinedTargetValidatesAllSubjects) {
		this.undefinedTargetValidatesAllSubjects = undefinedTargetValidatesAllSubjects;
	}

	public boolean isLogValidationPlans() {
		return logValidationPlans;
	}

	public void setLogValidationPlans(boolean logValidationPlans) {
		this.logValidationPlans = logValidationPlans;
	}

	public boolean isLogValidationViolations() {
		return logValidationViolations;
	}

	public void setLogValidationViolations(boolean logValidationViolations) {
		this.logValidationViolations = logValidationViolations;
	}

	public boolean isGlobalLogValidationExecution() {
		return globalLogValidationExecution;
	}

	public void setGlobalLogValidationExecution(boolean globalLogValidationExecution) {
		this.globalLogValidationExecution = globalLogValidationExecution;
	}

	@Deprecated
	public boolean isIgnoreNoShapesLoadedException() {
		return ignoreNoShapesLoadedException;
	}

	@Deprecated
	public void setIgnoreNoShapesLoadedException(boolean ignoreNoShapesLoadedException) {
		this.ignoreNoShapesLoadedException = ignoreNoShapesLoadedException;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	public void setValidationEnabled(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	public boolean isParallelValidation() {
		return parallelValidation;
	}

	public void setParallelValidation(boolean parallelValidation) {
		this.parallelValidation = parallelValidation;
	}

	public boolean isCacheSelectNodes() {
		return cacheSelectNodes;
	}

	public void setCacheSelectNodes(boolean cacheSelectNodes) {
		this.cacheSelectNodes = cacheSelectNodes;
	}

	public boolean isRdfsSubClassReasoning() {
		return rdfsSubClassReasoning;
	}

	public void setRdfsSubClassReasoning(boolean rdfsSubClassReasoning) {
		this.rdfsSubClassReasoning = rdfsSubClassReasoning;
	}

	public boolean isPerformanceLogging() {
		return performanceLogging;
	}

	public void setPerformanceLogging(boolean performanceLogging) {
		this.performanceLogging = performanceLogging;
	}

	public boolean isSerializableValidation() {
		return serializableValidation;
	}

	public void setSerializableValidation(boolean serializableValidation) {
		this.serializableValidation = serializableValidation;
	}

	@Experimental
	public boolean isEclipseRdf4jShaclExtensions() {
		return eclipseRdf4jShaclExtensions;
	}

	@Experimental
	public void setEclipseRdf4jShaclExtensions(boolean eclipseRdf4jShaclExtensions) {
		this.eclipseRdf4jShaclExtensions = eclipseRdf4jShaclExtensions;
	}

	@Experimental
	public boolean isDashDataShapes() {
		return dashDataShapes;
	}

	@Experimental
	public void setDashDataShapes(boolean dashDataShapes) {
		this.dashDataShapes = dashDataShapes;
	}

	public long getValidationResultsLimitTotal() {
		return validationResultsLimitTotal;
	}

	public long getValidationResultsLimitPerConstraint() {
		return validationResultsLimitPerConstraint;
	}

	public void setValidationResultsLimitTotal(long validationResultsLimitTotal) {
		this.validationResultsLimitTotal = validationResultsLimitTotal;
	}

	public void setValidationResultsLimitPerConstraint(long validationResultsLimitPerConstraint) {
		this.validationResultsLimitPerConstraint = validationResultsLimitPerConstraint;
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);

		m.setNamespace("sail-shacl", NAMESPACE);
		m.add(implNode, PARALLEL_VALIDATION, BooleanLiteral.valueOf(isParallelValidation()));
		m.add(implNode, UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS,
				BooleanLiteral.valueOf(isUndefinedTargetValidatesAllSubjects()));
		m.add(implNode, LOG_VALIDATION_PLANS, BooleanLiteral.valueOf(isLogValidationPlans()));
		m.add(implNode, LOG_VALIDATION_VIOLATIONS, BooleanLiteral.valueOf(isLogValidationViolations()));
		m.add(implNode, IGNORE_NO_SHAPES_LOADED_EXCEPTION, BooleanLiteral.valueOf(isIgnoreNoShapesLoadedException()));
		m.add(implNode, VALIDATION_ENABLED, BooleanLiteral.valueOf(isValidationEnabled()));
		m.add(implNode, CACHE_SELECT_NODES, BooleanLiteral.valueOf(isCacheSelectNodes()));
		m.add(implNode, GLOBAL_LOG_VALIDATION_EXECUTION, BooleanLiteral.valueOf(isGlobalLogValidationExecution()));
		m.add(implNode, RDFS_SUB_CLASS_REASONING, BooleanLiteral.valueOf(isRdfsSubClassReasoning()));
		m.add(implNode, PERFORMANCE_LOGGING, BooleanLiteral.valueOf(isPerformanceLogging()));
		m.add(implNode, SERIALIZABLE_VALIDATION, BooleanLiteral.valueOf(isSerializableValidation()));
		m.add(implNode, ShaclSailSchema.ECLIPSE_RDF4J_SHACL_EXTENSIONS,
				BooleanLiteral.valueOf(isEclipseRdf4jShaclExtensions()));
		m.add(implNode, ShaclSailSchema.DASH_DATA_SHAPES, BooleanLiteral.valueOf(isDashDataShapes()));

		m.add(implNode, ShaclSailSchema.VALIDATION_RESULTS_LIMIT_TOTAL,
				literal(getValidationResultsLimitTotal()));
		m.add(implNode, ShaclSailSchema.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT,
				literal(getValidationResultsLimitPerConstraint()));
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.getStatements(implNode, PARALLEL_VALIDATION, null))
					.ifPresent(l -> setParallelValidation(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, null))
					.ifPresent(l -> setUndefinedTargetValidatesAllSubjects(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, LOG_VALIDATION_PLANS, null))
					.ifPresent(l -> setLogValidationPlans(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, LOG_VALIDATION_VIOLATIONS, null))
					.ifPresent(l -> setLogValidationViolations(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, IGNORE_NO_SHAPES_LOADED_EXCEPTION, null))
					.ifPresent(l -> setIgnoreNoShapesLoadedException(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, VALIDATION_ENABLED, null))
					.ifPresent(l -> setValidationEnabled(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, CACHE_SELECT_NODES, null))
					.ifPresent(l -> setCacheSelectNodes(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, GLOBAL_LOG_VALIDATION_EXECUTION, null))
					.ifPresent(l -> setGlobalLogValidationExecution(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, RDFS_SUB_CLASS_REASONING, null))
					.ifPresent(l -> setRdfsSubClassReasoning(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, PERFORMANCE_LOGGING, null))
					.ifPresent(l -> setPerformanceLogging(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, SERIALIZABLE_VALIDATION, null))
					.ifPresent(l -> setSerializableValidation(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, ShaclSailSchema.ECLIPSE_RDF4J_SHACL_EXTENSIONS, null))
					.ifPresent(l -> setEclipseRdf4jShaclExtensions(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, ShaclSailSchema.DASH_DATA_SHAPES, null))
					.ifPresent(l -> setDashDataShapes(l.booleanValue()));

			Models.objectLiteral(m.getStatements(implNode, ShaclSailSchema.VALIDATION_RESULTS_LIMIT_TOTAL, null))
					.ifPresent(l -> setValidationResultsLimitTotal(l.longValue()));

			Models.objectLiteral(
					m.getStatements(implNode, ShaclSailSchema.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT, null))
					.ifPresent(l -> setValidationResultsLimitPerConstraint(l.longValue()));

		} catch (IllegalArgumentException e) {
			throw new SailConfigException("error parsing Sail configuration", e);
		}

	}

}
