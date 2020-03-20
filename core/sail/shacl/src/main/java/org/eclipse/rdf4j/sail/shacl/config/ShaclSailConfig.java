/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

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

	public ShaclSailConfig() {
		super(ShaclSailFactory.SAIL_TYPE);
	}

	public ShaclSailConfig(SailImplConfig delegate) {
		super(ShaclSailFactory.SAIL_TYPE, delegate);
	}

	public boolean isUndefinedTargetValidatesAllSubjects() {
		return undefinedTargetValidatesAllSubjects;
	}

	public boolean isLogValidationPlans() {
		return logValidationPlans;
	}

	public boolean isLogValidationViolations() {
		return logValidationViolations;
	}

	public boolean isGlobalLogValidationExecution() {
		return globalLogValidationExecution;
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

	public void setParallelValidation(boolean parallelValidation) {
		this.parallelValidation = parallelValidation;
	}

	public void setUndefinedTargetValidatesAllSubjects(boolean undefinedTargetValidatesAllSubjects) {
		this.undefinedTargetValidatesAllSubjects = undefinedTargetValidatesAllSubjects;
	}

	public void setLogValidationPlans(boolean logValidationPlans) {
		this.logValidationPlans = logValidationPlans;
	}

	public void setLogValidationViolations(boolean logValidationViolations) {
		this.logValidationViolations = logValidationViolations;
	}

	public void setIgnoreNoShapesLoadedException(boolean ignoreNoShapesLoadedException) {
		this.ignoreNoShapesLoadedException = ignoreNoShapesLoadedException;
	}

	public void setValidationEnabled(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	public void setCacheSelectNodes(boolean cacheSelectNodes) {
		this.cacheSelectNodes = cacheSelectNodes;
	}

	public void setGlobalLogValidationExecution(boolean globalLogValidationExecution) {
		this.globalLogValidationExecution = globalLogValidationExecution;
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
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.filter(implNode, PARALLEL_VALIDATION, null))
					.ifPresent(l -> setParallelValidation(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, null))
					.ifPresent(l -> setUndefinedTargetValidatesAllSubjects(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, LOG_VALIDATION_PLANS, null))
					.ifPresent(l -> setLogValidationPlans(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, LOG_VALIDATION_VIOLATIONS, null))
					.ifPresent(l -> setLogValidationViolations(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, IGNORE_NO_SHAPES_LOADED_EXCEPTION, null))
					.ifPresent(l -> setIgnoreNoShapesLoadedException(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, VALIDATION_ENABLED, null))
					.ifPresent(l -> setValidationEnabled(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, CACHE_SELECT_NODES, null))
					.ifPresent(l -> setCacheSelectNodes(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, GLOBAL_LOG_VALIDATION_EXECUTION, null))
					.ifPresent(l -> setGlobalLogValidationExecution(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, RDFS_SUB_CLASS_REASONING, null))
					.ifPresent(l -> setRdfsSubClassReasoning(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, PERFORMANCE_LOGGING, null))
					.ifPresent(l -> setPerformanceLogging(l.booleanValue()));
			Models.objectLiteral(m.filter(implNode, SERIALIZABLE_VALIDATION, null))
					.ifPresent(l -> setSerializableValidation(l.booleanValue()));
		} catch (IllegalArgumentException e) {
			throw new SailConfigException("error parsing Sail configuration", e);
		}

	}

}
