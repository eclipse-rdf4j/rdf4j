/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.CACHE_SELECT_NODES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.DASH_DATA_SHAPES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.ECLIPSE_RDF4J_SHACL_EXTENSIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.GLOBAL_LOG_VALIDATION_EXECUTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.IGNORE_NO_SHAPES_LOADED_EXCEPTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_PLANS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_VIOLATIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PARALLEL_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PERFORMANCE_LOGGING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.RDFS_SUB_CLASS_REASONING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.SERIALIZABLE_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_ENABLED;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_RESULTS_LIMIT_TOTAL;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.shacl.GlobalValidationExecutionLogging;
import org.junit.AfterClass;
import org.junit.Test;

public class ShaclSailConfigTest {

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void defaultsCorrectlySet() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		assertThat(shaclSailConfig.isParallelValidation()).isTrue();
		assertThat(shaclSailConfig.isUndefinedTargetValidatesAllSubjects()).isFalse();
		assertThat(shaclSailConfig.isLogValidationPlans()).isFalse();
		assertThat(shaclSailConfig.isLogValidationViolations()).isFalse();
		assertThat(shaclSailConfig.isIgnoreNoShapesLoadedException()).isFalse();
		assertThat(shaclSailConfig.isValidationEnabled()).isTrue();
		assertThat(shaclSailConfig.isCacheSelectNodes()).isTrue();
		assertThat(shaclSailConfig.isGlobalLogValidationExecution()).isFalse();
		assertThat(shaclSailConfig.isRdfsSubClassReasoning()).isTrue();
		assertThat(shaclSailConfig.isPerformanceLogging()).isFalse();
		assertThat(shaclSailConfig.isSerializableValidation()).isTrue();
		assertThat(shaclSailConfig.isEclipseRdf4jShaclExtensions()).isFalse();
		assertThat(shaclSailConfig.isDashDataShapes()).isFalse();
		assertThat(shaclSailConfig.getValidationResultsLimitTotal()).isEqualTo(-1);
		assertThat(shaclSailConfig.getValidationResultsLimitPerConstraint()).isEqualTo(-1);

	}

	@Test
	public void parseFromModelSetValuesCorrectly() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();

		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(PARALLEL_VALIDATION, true);
		mb.add(UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, true);
		mb.add(LOG_VALIDATION_PLANS, true);
		mb.add(LOG_VALIDATION_VIOLATIONS, true);
		mb.add(IGNORE_NO_SHAPES_LOADED_EXCEPTION, true);
		mb.add(VALIDATION_ENABLED, true);
		mb.add(CACHE_SELECT_NODES, true);
		mb.add(GLOBAL_LOG_VALIDATION_EXECUTION, true);
		mb.add(RDFS_SUB_CLASS_REASONING, false);
		mb.add(PERFORMANCE_LOGGING, true);
		mb.add(ECLIPSE_RDF4J_SHACL_EXTENSIONS, true);
		mb.add(DASH_DATA_SHAPES, true);
		mb.add(SERIALIZABLE_VALIDATION, false);

		mb.add(VALIDATION_RESULTS_LIMIT_TOTAL, 100);
		mb.add(VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT, 3);

		shaclSailConfig.parse(mb.build(), implNode);

		assertThat(shaclSailConfig.isParallelValidation()).isTrue();
		assertThat(shaclSailConfig.isUndefinedTargetValidatesAllSubjects()).isTrue();
		assertThat(shaclSailConfig.isLogValidationPlans()).isTrue();
		assertThat(shaclSailConfig.isLogValidationViolations()).isTrue();
		assertThat(shaclSailConfig.isIgnoreNoShapesLoadedException()).isTrue();
		assertThat(shaclSailConfig.isValidationEnabled()).isTrue();
		assertThat(shaclSailConfig.isCacheSelectNodes()).isTrue();
		assertThat(shaclSailConfig.isGlobalLogValidationExecution()).isTrue();
		assertThat(shaclSailConfig.isRdfsSubClassReasoning()).isFalse();
		assertThat(shaclSailConfig.isPerformanceLogging()).isTrue();
		assertThat(shaclSailConfig.isSerializableValidation()).isFalse();
		assertThat(shaclSailConfig.isEclipseRdf4jShaclExtensions()).isTrue();
		assertThat(shaclSailConfig.isDashDataShapes()).isTrue();
		assertThat(shaclSailConfig.getValidationResultsLimitTotal()).isEqualTo(100);
		assertThat(shaclSailConfig.getValidationResultsLimitPerConstraint()).isEqualTo(3);

	}

	@Test
	public void parseFromPartialModelSetValuesCorrectly() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(PARALLEL_VALIDATION, false);

		shaclSailConfig.parse(mb.build(), implNode);

		assertThat(shaclSailConfig.isParallelValidation()).isFalse();
		assertThat(shaclSailConfig.isCacheSelectNodes()).isTrue();
	}

	@Test(expected = SailConfigException.class)
	public void parseInvalidModelGivesCorrectException() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(PARALLEL_VALIDATION, "I'm not a boolean");

		shaclSailConfig.parse(mb.build(), implNode);

	}

	@Test
	public void exportAddsAllConfigData() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();

		Model m = new TreeModel();
		Resource node = shaclSailConfig.export(m);
		assertTrue(m.contains(node, PARALLEL_VALIDATION, null));
		assertTrue(m.contains(node, UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, null));
		assertTrue(m.contains(node, LOG_VALIDATION_PLANS, null));
		assertTrue(m.contains(node, LOG_VALIDATION_VIOLATIONS, null));
		assertTrue(m.contains(node, IGNORE_NO_SHAPES_LOADED_EXCEPTION, null));
		assertTrue(m.contains(node, VALIDATION_ENABLED, null));
		assertTrue(m.contains(node, CACHE_SELECT_NODES, null));
		assertTrue(m.contains(node, GLOBAL_LOG_VALIDATION_EXECUTION, null));
		assertTrue(m.contains(node, RDFS_SUB_CLASS_REASONING, null));
		assertTrue(m.contains(node, PERFORMANCE_LOGGING, null));
		assertTrue(m.contains(node, SERIALIZABLE_VALIDATION, null));
		assertTrue(m.contains(node, ECLIPSE_RDF4J_SHACL_EXTENSIONS, null));
		assertTrue(m.contains(node, DASH_DATA_SHAPES, null));
		assertTrue(m.contains(node, VALIDATION_RESULTS_LIMIT_TOTAL, null));
		assertTrue(m.contains(node, VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT, null));

	}

}
