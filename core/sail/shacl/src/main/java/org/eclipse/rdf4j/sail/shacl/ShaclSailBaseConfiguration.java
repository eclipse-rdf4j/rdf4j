/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.shacl.config.ShaclSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class ShaclSailBaseConfiguration extends NotifyingSailWrapper {

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailBaseConfiguration.class);

	// Field used to control if the new SPARQL based validation should be enabled or disabled. Enabled by default.
	final boolean sparqlValidation;

	private boolean parallelValidation = ShaclSailConfig.PARALLEL_VALIDATION_DEFAULT;
	private boolean logValidationPlans = ShaclSailConfig.LOG_VALIDATION_PLANS_DEFAULT;
	private boolean logValidationViolations = ShaclSailConfig.LOG_VALIDATION_VIOLATIONS_DEFAULT;
	private boolean validationEnabled = ShaclSailConfig.VALIDATION_ENABLED_DEFAULT;
	private boolean cacheSelectNodes = ShaclSailConfig.CACHE_SELECT_NODES_DEFAULT;
	private boolean rdfsSubClassReasoning = ShaclSailConfig.RDFS_SUB_CLASS_REASONING_DEFAULT;
	private boolean serializableValidation = ShaclSailConfig.SERIALIZABLE_VALIDATION_DEFAULT;
	private boolean performanceLogging = ShaclSailConfig.PERFORMANCE_LOGGING_DEFAULT;
	private boolean eclipseRdf4jShaclExtensions = ShaclSailConfig.ECLIPSE_RDF4J_SHACL_EXTENSIONS_DEFAULT;
	private boolean dashDataShapes = ShaclSailConfig.DASH_DATA_SHAPES_DEFAULT;
	private long validationResultsLimitTotal = ShaclSailConfig.VALIDATION_RESULTS_LIMIT_TOTAL_DEFAULT;
	private long validationResultsLimitPerConstraint = ShaclSailConfig.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT_DEFAULT;
	private long transactionalValidationLimit = ShaclSailConfig.TRANSACTIONAL_VALIDATION_LIMIT_DEFAULT;
	private boolean logValidationExecution = false;
	private Set<IRI> shapesGraphs = ShaclSailConfig.SHAPES_GRAPHS_DEFAULT;

	public ShaclSailBaseConfiguration(NotifyingSail baseSail) {
		super(baseSail);
		this.sparqlValidation = !"false"
				.equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.sail.shacl.sparqlValidation"));
	}

	public ShaclSailBaseConfiguration() {
		super();
		this.sparqlValidation = !"false"
				.equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.sail.shacl.sparqlValidation"));
	}

	/**
	 * Check if logging of every execution steps is enabled.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 * @see #setGlobalLogValidationExecution(boolean)
	 */
	public boolean isGlobalLogValidationExecution() {
		return logValidationExecution;
	}

	/**
	 * Log (INFO) every execution step of the SHACL validation. This is fairly costly and should not be used in
	 * production. Recommended to disable parallel validation with setParallelValidation(false)
	 *
	 * @param loggingEnabled
	 */
	public void setGlobalLogValidationExecution(boolean loggingEnabled) {
		logValidationExecution = loggingEnabled;
	}

	/**
	 * Check if logging a list of violations and the triples that caused the violations is enabled. It is recommended to
	 * disable parallel validation with {@link #setParallelValidation(boolean)}
	 *
	 * @see #setLogValidationViolations(boolean)
	 */
	public boolean isLogValidationViolations() {
		return this.logValidationViolations;
	}

	/**
	 * Log (INFO) a list of violations and the triples that caused the violations (BETA). Recommended to disable
	 * parallel validation with setParallelValidation(false)
	 *
	 * @param logValidationViolations
	 */
	public void setLogValidationViolations(boolean logValidationViolations) {
		this.logValidationViolations = logValidationViolations;
	}

	/**
	 * Check if SHACL validation is run in parellel.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 */
	public boolean isParallelValidation() {
		return this.parallelValidation;
	}

	/**
	 * EXPERIMENTAL! Run SHACL validation in parallel. Default: false
	 * <p>
	 * May cause deadlock, especially when using NativeStore.
	 *
	 * @param parallelValidation default true
	 */
	public void setParallelValidation(boolean parallelValidation) {
		this.parallelValidation = parallelValidation;
	}

	/**
	 * Check if selected nodes caches is enabled.
	 *
	 * @return <code>true</code> if enabled, <code>false</code> otherwise.
	 * @see #setCacheSelectNodes(boolean)
	 */
	public boolean isCacheSelectNodes() {
		return this.cacheSelectNodes;
	}

	/**
	 * The ShaclSail retries a lot of its relevant data through running SPARQL Select queries against the underlying
	 * sail and against the changes in the transaction. This is usually good for performance, but while validating large
	 * amounts of data disabling this cache will use less memory. Default: true
	 *
	 * @param cacheSelectNodes default true
	 */
	public void setCacheSelectNodes(boolean cacheSelectNodes) {
		this.cacheSelectNodes = cacheSelectNodes;
	}

	public boolean isRdfsSubClassReasoning() {
		return rdfsSubClassReasoning;
	}

	public void setRdfsSubClassReasoning(boolean rdfsSubClassReasoning) {
		this.rdfsSubClassReasoning = rdfsSubClassReasoning;
	}

	/**
	 * Disable the SHACL validation on commit()
	 */
	public void disableValidation() {
		this.validationEnabled = false;
	}

	/**
	 * Enabled the SHACL validation on commit()
	 */
	public void enableValidation() {
		this.validationEnabled = true;
	}

	/**
	 * Check if SHACL validation on commit() is enabled.
	 *
	 * @return <code>true</code> if validation is enabled, <code>false</code> otherwise.
	 */
	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	/**
	 * Check if logging of validation plans is enabled.
	 *
	 * @return <code>true</code> if validation plan logging is enabled, <code>false</code> otherwise.
	 */
	public boolean isLogValidationPlans() {
		return this.logValidationPlans;
	}

	/**
	 * Log (INFO) the executed validation plans as GraphViz DOT Recommended to disable parallel validation with
	 * setParallelValidation(false)
	 *
	 * @param logValidationPlans
	 */
	public void setLogValidationPlans(boolean logValidationPlans) {
		this.logValidationPlans = logValidationPlans;
	}

	public boolean isPerformanceLogging() {
		return performanceLogging;
	}

	// @formatter:off

	/**
	 * Log (INFO) the execution time per shape. Recommended to disable the following:
	 * <ul>
	 * 		<li>setParallelValidation(false)</li>
	 * 		<li>setCacheSelectNodes(false)</li>
	 * </ul>
	 *
	 * @param performanceLogging default false
	 */
	// @formatter:on
	public void setPerformanceLogging(boolean performanceLogging) {
		this.performanceLogging = performanceLogging;
	}

	/**
	 * On transactions using SNAPSHOT isolation the ShaclSail can run the validation serializably. This stops the sail
	 * from becoming inconsistent due to race conditions between two transactions. Serializable validation limits TPS
	 * (transactions per second), it is however considerably faster than actually using SERIALIZABLE isolation.
	 *
	 * @return <code>true</code> if serializable validation is enabled, <code>false</code> otherwise.
	 */
	public boolean isSerializableValidation() {
		if (getBaseSail() instanceof SchemaCachingRDFSInferencer) {
			if (serializableValidation) {
				logger.warn("SchemaCachingRDFSInferencer is not supported when using serializable validation!");
			}
			return false;
		}
		return serializableValidation;
	}

	/**
	 * Enable or disable serializable validation.On transactions using SNAPSHOT isolation the ShaclSail can run the
	 * validation serializably. This stops the sail from becoming inconsistent due to race conditions between two
	 * transactions. Serializable validation limits TPS (transactions per second), it is however considerably faster
	 * than actually using SERIALIZABLE isolation.
	 *
	 * <p>
	 * To increase TPS, serializable validation can be disabled. Validation will then be limited to the semantics of the
	 * SNAPSHOT isolation level (or whichever is specified). If you use any other isolation level than SNAPSHOT,
	 * disabling serializable validation will make no difference on performance.
	 * </p>
	 *
	 * @param serializableValidation default true
	 */
	public void setSerializableValidation(boolean serializableValidation) {
		this.serializableValidation = serializableValidation;
	}

	/**
	 * Support for Eclipse RDF4J SHACL Extensions (http://rdf4j.org/shacl-extensions#). Enabling this currently enables
	 * support for rsx:targetShape.
	 *
	 * EXPERIMENTAL!
	 *
	 * @return true if enabled
	 */
	@Experimental
	public boolean isEclipseRdf4jShaclExtensions() {
		return eclipseRdf4jShaclExtensions;
	}

	/**
	 * Support for Eclipse RDF4J SHACL Extensions (http://rdf4j.org/shacl-extensions#). Enabling this currently enables
	 * support for rsx:targetShape.
	 *
	 * EXPERIMENTAL!
	 *
	 * @param eclipseRdf4jShaclExtensions true to enable (default: false)
	 */
	@Experimental
	public void setEclipseRdf4jShaclExtensions(boolean eclipseRdf4jShaclExtensions) {
		this.eclipseRdf4jShaclExtensions = eclipseRdf4jShaclExtensions;
	}

	/**
	 * Support for DASH Data Shapes Vocabulary Unofficial Draft (http://datashapes.org/dash). Currently this enables
	 * support for dash:hasValueIn, dash:AllObjectsTarget and dash:AllSubjectsTarget.
	 *
	 * EXPERIMENTAL!
	 *
	 * @return true if enabled
	 */
	@Experimental
	public boolean isDashDataShapes() {
		return dashDataShapes;
	}

	/**
	 * Support for DASH Data Shapes Vocabulary Unofficial Draft (http://datashapes.org/dash). Currently this enables
	 * support for dash:hasValueIn, dash:AllObjectsTarget and and dash:AllSubjectsTarget.
	 *
	 * EXPERIMENTAL!
	 *
	 * @param dashDataShapes true to enable (default: false)
	 */
	@Experimental
	public void setDashDataShapes(boolean dashDataShapes) {
		this.dashDataShapes = dashDataShapes;
	}

	/**
	 * ValidationReports contain validation results. The number of validation results can be limited by the user. This
	 * can be useful to reduce the size of reports when there are a lot of failures, which increases validation speed
	 * and reduces memory usage.
	 *
	 * @return the limit for validation results per validation report per constraint, -1 for no limit
	 */
	public long getValidationResultsLimitPerConstraint() {
		return validationResultsLimitPerConstraint;
	}

	/**
	 * ValidationReports contain validation results. The number of validation results can be limited by the user. This
	 * can be useful to reduce the size of reports when there are a lot of failures, which increases validation speed
	 * and reduces memory usage.
	 *
	 * @param validationResultsLimitPerConstraint the limit for the number of validation results per report per
	 *                                            constraint, -1 for no limit
	 */
	public void setValidationResultsLimitPerConstraint(long validationResultsLimitPerConstraint) {
		this.validationResultsLimitPerConstraint = validationResultsLimitPerConstraint;
	}

	/**
	 *
	 * @return the effective limit per constraint with an upper bound of the total limit
	 */
	public long getEffectiveValidationResultsLimitPerConstraint() {
		if (validationResultsLimitPerConstraint < 0) {
			return validationResultsLimitTotal;
		}
		if (validationResultsLimitTotal >= 0) {
			return Math.min(validationResultsLimitTotal, validationResultsLimitPerConstraint);
		}

		return validationResultsLimitPerConstraint;
	}

	/**
	 * ValidationReports contain validation results. The number of validation results can be limited by the user. This
	 * can be useful to reduce the size of reports when there are a lot of failures, which increases validation speed
	 * and reduces memory usage.
	 *
	 * @return the limit for validation results per validation report in total, -1 for no limit
	 */
	public long getValidationResultsLimitTotal() {
		return validationResultsLimitTotal;
	}

	/**
	 * ValidationReports contain validation results. The number of validation results can be limited by the user. This
	 * can be useful to reduce the size of reports when there are a lot of failures, which increases validation speed
	 * and reduces memory usage.
	 *
	 * @param validationResultsLimitTotal the limit for the number of validation results per report in total, -1 for no
	 *                                    limit
	 */
	public void setValidationResultsLimitTotal(long validationResultsLimitTotal) {
		this.validationResultsLimitTotal = validationResultsLimitTotal;
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return super.getDefaultIsolationLevel();
	}

	public long getTransactionalValidationLimit() {
		return transactionalValidationLimit;
	}

	public void setTransactionalValidationLimit(long transactionalValidationLimit) {
		this.transactionalValidationLimit = transactionalValidationLimit;
	}

	public Set<IRI> getShapesGraphs() {
		return shapesGraphs;
	}

	public void setShapesGraphs(Set<IRI> shapesGraphs) {
		this.shapesGraphs = shapesGraphs;
	}
}
