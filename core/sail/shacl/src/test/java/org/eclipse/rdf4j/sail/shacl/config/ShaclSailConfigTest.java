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
import org.eclipse.rdf4j.model.vocabulary.Config;
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

		mb.add(Config.ShaclSail.parallelValidation, true);
		mb.add(Config.ShaclSail.logValidationPlans, true);
		mb.add(Config.ShaclSail.logValidationViolations, true);
		mb.add(Config.ShaclSail.validationEnabled, true);
		mb.add(Config.ShaclSail.cacheSelectNodes, true);
		mb.add(Config.ShaclSail.globalLogValidationExecution, true);
		mb.add(Config.ShaclSail.rdfsSubClassReasoning, false);
		mb.add(Config.ShaclSail.performanceLogging, true);
		mb.add(Config.ShaclSail.eclipseRdf4jShaclExtensions, true);
		mb.add(Config.ShaclSail.dashDataShapes, true);
		mb.add(Config.ShaclSail.serializableValidation, false);

		mb.add(Config.ShaclSail.validationResultsLimitTotal, 100);
		mb.add(Config.ShaclSail.validationResultsLimitPerConstraint, 3);
		mb.add(Config.ShaclSail.transactionalValidationLimit, 9);

		Set<IRI> shapesGraphs = Set.of(Values.iri("http://example.com/ex1"), Values.iri("http://example.com/ex2"));
		for (IRI shapesGraph : shapesGraphs) {
			mb.add(Config.ShaclSail.shapesGraph, shapesGraph);
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

		mb.add(Config.ShaclSail.parallelValidation, false);

		shaclSailConfig.parse(mb.build(), implNode);

		assertThat(shaclSailConfig.isParallelValidation()).isFalse();
		assertThat(shaclSailConfig.isCacheSelectNodes()).isTrue();
	}

	@Test
	public void parseInvalidModelGivesCorrectException() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(Config.ShaclSail.parallelValidation, "I'm not a boolean");

		assertThrows(SailConfigException.class, () -> {
			shaclSailConfig.parse(mb.build(), implNode);
		});
	}

	@Test
	public void parseInvalidModelGivesCorrectExceptionBnode() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();
		BNode implNode = SimpleValueFactory.getInstance().createBNode();
		ModelBuilder mb = new ModelBuilder().subject(implNode);

		mb.add(Config.ShaclSail.shapesGraph, Values.bnode());

		assertThrows(SailConfigException.class, () -> {
			shaclSailConfig.parse(mb.build(), implNode);
		});
	}

	@Test
	public void exportAddsAllConfigData() {
		ShaclSailConfig shaclSailConfig = new ShaclSailConfig();

		Model m = new TreeModel();
		Resource node = shaclSailConfig.export(m);
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.parallelValidation, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.logValidationPlans, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.logValidationViolations, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.validationEnabled, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.cacheSelectNodes, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.globalLogValidationExecution, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.rdfsSubClassReasoning, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.performanceLogging, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.serializableValidation, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.eclipseRdf4jShaclExtensions, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.dashDataShapes, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.validationResultsLimitTotal, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.validationResultsLimitPerConstraint, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.transactionalValidationLimit, null));
		Assertions.assertTrue(m.contains(node, Config.ShaclSail.shapesGraph, null));

	}

}
