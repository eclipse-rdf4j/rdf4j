/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.CACHE_SELECT_NODES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.GLOBAL_LOG_VALIDATION_EXECUTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.IGNORE_NO_SHAPES_LOADED_EXCEPTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_PLANS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_VIOLATIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PARALLEL_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.RDFS_SUB_CLASS_REASONING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_ENABLED;

public class ShaclSailConfigTest {

	private ShaclSailConfig subject;

	private BNode implNode;

	private ModelBuilder mb;

	@Before
	public void setUp() throws Exception {
		subject = new ShaclSailConfig();
		implNode = SimpleValueFactory.getInstance().createBNode();
		mb = new ModelBuilder().subject(implNode);
	}

	@Test
	public void defaultsCorrectlySet() {
		assertThat(subject.isParallelValidation()).isFalse();
		assertThat(subject.isUndefinedTargetValidatesAllSubjects()).isFalse();
		assertThat(subject.isLogValidationPlans()).isFalse();
		assertThat(subject.isLogValidationViolations()).isFalse();
		assertThat(subject.isIgnoreNoShapesLoadedException()).isFalse();
		assertThat(subject.isValidationEnabled()).isTrue();
		assertThat(subject.isCacheSelectNodes()).isTrue();
		assertThat(subject.isGlobalLogValidationExecution()).isFalse();
		assertThat(subject.isRdfsSubClassReasoning()).isTrue();
	}

	@Test
	public void parseFromModelSetValuesCorrectly() {

		// FIXME we need to set formatting guidelines for this kind of thing
		// @formatter:off
		mb.add(PARALLEL_VALIDATION, true)
		  .add(UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, true)
		  .add(LOG_VALIDATION_PLANS, true)
		  .add(LOG_VALIDATION_VIOLATIONS, true)
		  .add(IGNORE_NO_SHAPES_LOADED_EXCEPTION, true)
		  .add(VALIDATION_ENABLED, true)
		  .add(CACHE_SELECT_NODES, true)
		  .add(GLOBAL_LOG_VALIDATION_EXECUTION, true)
		  .add(RDFS_SUB_CLASS_REASONING, false);
		// @formatter:on

		subject.parse(mb.build(), implNode);

		assertThat(subject.isParallelValidation()).isTrue();
		assertThat(subject.isUndefinedTargetValidatesAllSubjects()).isTrue();
		assertThat(subject.isLogValidationPlans()).isTrue();
		assertThat(subject.isLogValidationViolations()).isTrue();
		assertThat(subject.isIgnoreNoShapesLoadedException()).isTrue();
		assertThat(subject.isValidationEnabled()).isTrue();
		assertThat(subject.isCacheSelectNodes()).isTrue();
		assertThat(subject.isGlobalLogValidationExecution()).isTrue();
		assertThat(subject.isRdfsSubClassReasoning()).isFalse();

	}

	@Test
	public void parseFromPartialModelSetValuesCorrectly() {
		mb.add(PARALLEL_VALIDATION, false);

		subject.parse(mb.build(), implNode);

		assertThat(subject.isParallelValidation()).isFalse();
		assertThat(subject.isCacheSelectNodes()).isTrue();
	}

	@Test(expected = SailConfigException.class)
	public void parseInvalidModelGivesCorrectException() {

		mb.add(PARALLEL_VALIDATION, "I'm not a boolean");

		subject.parse(mb.build(), implNode);

	}

	@Test
	public void exportAddsAllConfigData() {
		Model m = new TreeModel();
		Resource node = subject.export(m);
		assertThat(m.contains(node, PARALLEL_VALIDATION, null)).isTrue();
		assertThat(m.contains(node, UNDEFINED_TARGET_VALIDATES_ALL_SUBJECTS, null)).isTrue();
		assertThat(m.contains(node, LOG_VALIDATION_PLANS, null)).isTrue();
		assertThat(m.contains(node, LOG_VALIDATION_VIOLATIONS, null)).isTrue();
		assertThat(m.contains(node, IGNORE_NO_SHAPES_LOADED_EXCEPTION, null)).isTrue();
		assertThat(m.contains(node, VALIDATION_ENABLED, null)).isTrue();
		assertThat(m.contains(node, CACHE_SELECT_NODES, null)).isTrue();
		assertThat(m.contains(node, GLOBAL_LOG_VALIDATION_EXECUTION, null)).isTrue();
		assertThat(m.contains(node, RDFS_SUB_CLASS_REASONING, null)).isTrue();
	}

}
