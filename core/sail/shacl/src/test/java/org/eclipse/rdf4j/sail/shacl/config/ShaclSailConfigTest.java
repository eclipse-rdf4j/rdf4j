/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.CACHE_SELECT_NODES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.DASH_DATA_SHAPES;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.ECLIPSE_RDF4J_SHACL_EXTENSIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.GLOBAL_LOG_VALIDATION_EXECUTION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_PLANS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.LOG_VALIDATION_VIOLATIONS;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PARALLEL_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.PERFORMANCE_LOGGING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.RDFS_SUB_CLASS_REASONING;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.SERIALIZABLE_VALIDATION;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.SHAPES_GRAPH;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.TRANSACTIONAL_VALIDATION_LIMIT;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_ENABLED;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT;
import static org.eclipse.rdf4j.sail.shacl.config.ShaclSailSchema.VALIDATION_RESULTS_LIMIT_TOTAL;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShaclSailConfigTest {

	public static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void defaultsCorrectlySet() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		assertThat(shaclSailConfig.isParallelValidation()).isTrue();
		assertThat(shaclSailConfig.isLogValidationPlans()).isFalse();
		assertThat(shaclSailConfig.isLogValidationViolations()).isFalse();
		assertThat(shaclSailConfig.isValidationEnabled()).isTrue();
		assertThat(shaclSailConfig.isCacheSelectNodes()).isTrue();
		assertThat(shaclSailConfig.isGlobalLogValidationExecution()).isFalse();
		assertThat(shaclSailConfig.isRdfsSubClassReasoning()).isTrue();
		assertThat(shaclSailConfig.isPerformanceLogging()).isFalse();
		assertThat(shaclSailConfig.isSerializableValidation()).isTrue();
		assertThat(shaclSailConfig.isEclipseRdf4jShaclExtensions()).isFalse();
		assertThat(shaclSailConfig.isDashDataShapes()).isFalse();
		assertThat(shaclSailConfig.getValidationResultsLimitTotal()).isEqualTo(1000000);
		assertThat(shaclSailConfig.getValidationResultsLimitPerConstraint()).isEqualTo(1000);
		assertThat(shaclSailConfig.getTransactionalValidationLimit()).isEqualTo(500000);
		assertThat(shaclSailConfig.getShapesGraphs()).isEqualTo(Set.of(RDF4J.SHACL_SHAPE_GRAPH));

	}

	@Test
	public void parseFromModelSetValuesCorrectly() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();

		BNode implNode = vf.createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(PARALLEL_VALIDATION, true);
		mb.add(LOG_VALIDATION_PLANS, true);
		mb.add(LOG_VALIDATION_VIOLATIONS, true);
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
		mb.add(TRANSACTIONAL_VALIDATION_LIMIT, 9);

		Set<IRI> shapesGraphs = Set.of(Values.iri("http://example.com/ex1"), Values.iri("http://example.com/ex2"));
		for (IRI shapesGraph : shapesGraphs) {
			mb.add(SHAPES_GRAPH, shapesGraph);
		}

		shaclSailConfig.parse(mb.build(), implNode);

		assertThat(shaclSailConfig.isParallelValidation()).isTrue();
		assertThat(shaclSailConfig.isLogValidationPlans()).isTrue();
		assertThat(shaclSailConfig.isLogValidationViolations()).isTrue();
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
		assertThat(shaclSailConfig.getTransactionalValidationLimit()).isEqualTo(9);
		assertThat(shaclSailConfig.getShapesGraphs()).isEqualTo(shapesGraphs);

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

	@Test
	public void parseInvalidModelGivesCorrectException() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(PARALLEL_VALIDATION, "I'm not a boolean");

		assertThrows(SailConfigException.class, () -> {
			shaclSailConfig.parse(mb.build(), implNode);
		});
	}

	@Test
	public void parseInvalidModelGivesCorrectExceptionBnode() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(SHAPES_GRAPH, Values.bnode());

		assertThrows(SailConfigException.class, () -> {
			shaclSailConfig.parse(mb.build(), implNode);
		});
	}

	@Test
	public void exportAddsAllConfigData() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();

		Model m = new TreeModel();
		Resource node = shaclSailConfig.export(m);
		Assertions.assertTrue(m.contains(node, PARALLEL_VALIDATION, null));
		Assertions.assertTrue(m.contains(node, LOG_VALIDATION_PLANS, null));
		Assertions.assertTrue(m.contains(node, LOG_VALIDATION_VIOLATIONS, null));
		Assertions.assertTrue(m.contains(node, VALIDATION_ENABLED, null));
		Assertions.assertTrue(m.contains(node, CACHE_SELECT_NODES, null));
		Assertions.assertTrue(m.contains(node, GLOBAL_LOG_VALIDATION_EXECUTION, null));
		Assertions.assertTrue(m.contains(node, RDFS_SUB_CLASS_REASONING, null));
		Assertions.assertTrue(m.contains(node, PERFORMANCE_LOGGING, null));
		Assertions.assertTrue(m.contains(node, SERIALIZABLE_VALIDATION, null));
		Assertions.assertTrue(m.contains(node, ECLIPSE_RDF4J_SHACL_EXTENSIONS, null));
		Assertions.assertTrue(m.contains(node, DASH_DATA_SHAPES, null));
		Assertions.assertTrue(m.contains(node, VALIDATION_RESULTS_LIMIT_TOTAL, null));
		Assertions.assertTrue(m.contains(node, VALIDATION_RESULTS_LIMIT_PER_CONSTRAINT, null));
		Assertions.assertTrue(m.contains(node, TRANSACTIONAL_VALIDATION_LIMIT, null));
		Assertions.assertTrue(m.contains(node, SHAPES_GRAPH, null));

	}

}
