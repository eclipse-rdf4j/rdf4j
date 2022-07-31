/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sail;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ExtendedEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ExtendedEvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for behavior of {@link StrictEvaluationStrategy} and {@link ExtendedEvaluationStrategy} on base Sail
 * implementations.
 *
 * @author Jeen Broekstra
 */
public abstract class EvaluationStrategyTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private Repository strictRepo;

	private Repository extendedRepo;

	private RepositoryManager manager;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		manager = RepositoryProvider.getRepositoryManager(tempDir.newFolder());

		BaseSailConfig strictStoreConfig = getBaseSailConfig();
		strictStoreConfig.setEvaluationStrategyFactoryClassName(StrictEvaluationStrategyFactory.class.getName());

		strictRepo = createRepo(strictStoreConfig, "test-strict");

		BaseSailConfig extendedStoreConfig = getBaseSailConfig();
		extendedStoreConfig.setEvaluationStrategyFactoryClassName(ExtendedEvaluationStrategyFactory.class.getName());

		extendedRepo = createRepo(extendedStoreConfig, "test-extended");
	}

	private Repository createRepo(BaseSailConfig config, String id) {
		RepositoryImplConfig ric = new SailRepositoryConfig(config);
		manager.addRepositoryConfig(new RepositoryConfig(id, ric));

		return manager.getRepository(id);
	}

	@Test
	public void testDatetimeSubtypesStrict() {
		ValueFactory vf = strictRepo.getValueFactory();

		try (RepositoryConnection conn = strictRepo.getConnection()) {
			Literal l1 = vf.createLiteral("2009", XSD.GYEAR);
			Literal l2 = vf.createLiteral("2009-01", CoreDatatype.XSD.GYEARMONTH);
			IRI s1 = vf.createIRI("urn:s1");
			IRI s2 = vf.createIRI("urn:s2");
			conn.add(s1, RDFS.LABEL, l1);
			conn.add(s2, RDFS.LABEL, l2);

			String query = "SELECT * WHERE { ?s rdfs:label ?l . FILTER(?l >= \"2008\"^^xsd:gYear) }";

			List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
			assertEquals(1, result.size());
		}
	}

	@Test
	public void testDatetimeSubtypesExtended() {
		ValueFactory vf = extendedRepo.getValueFactory();

		try (RepositoryConnection conn = extendedRepo.getConnection()) {
			Literal l1 = vf.createLiteral("2009", CoreDatatype.XSD.GYEAR);
			Literal l2 = vf.createLiteral("2009-01", XSD.GYEARMONTH);
			IRI s1 = vf.createIRI("urn:s1");
			IRI s2 = vf.createIRI("urn:s2");
			conn.add(s1, RDFS.LABEL, l1);
			conn.add(s2, RDFS.LABEL, l2);

			String query = "SELECT * WHERE { ?s rdfs:label ?l . FILTER(?l >= \"2008\"^^xsd:gYear) }";

			List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
			assertEquals(2, result.size());
		}
	}

	/**
	 * Gets a configuration object for the base Sail that should be tested.
	 *
	 * @return a {@link BaseSailConfig}.
	 */
	protected abstract BaseSailConfig getBaseSailConfig();
}
