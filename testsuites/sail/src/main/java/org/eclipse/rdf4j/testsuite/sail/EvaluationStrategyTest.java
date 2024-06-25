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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;

import java.io.File;
import java.util.List;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DefaultEvaluationStrategy;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test cases for behavior of {@link DefaultEvaluationStrategy} and {@link QueryEvaluationMode} on base Sail
 * implementations.
 *
 * @author Jeen Broekstra
 */
public abstract class EvaluationStrategyTest {

	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void afterClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	@TempDir
	private File tempDir;

	private Repository strictRepo;

	private Repository standardRepo;

	private RepositoryManager manager;

	private Literal gYearLit = literal("2009", XSD.GYEAR);
	private Literal gYearMonthLit = literal("2009-01", CoreDatatype.XSD.GYEARMONTH);
	private IRI s1 = iri("urn:s1");
	private IRI s2 = iri("urn:s2");

	/**
	 */
	@BeforeEach
	public void setUp() {
		manager = RepositoryProvider.getRepositoryManager(tempDir);

		BaseSailConfig strictStoreConfig = getBaseSailConfig();
		strictStoreConfig.setDefaultQueryEvaluationMode(QueryEvaluationMode.STRICT);

		strictRepo = createRepo(strictStoreConfig, "test-strict");

		BaseSailConfig standardStoreConfig = getBaseSailConfig();
		standardStoreConfig.setDefaultQueryEvaluationMode(QueryEvaluationMode.STANDARD);
		standardRepo = createRepo(standardStoreConfig, "test-standard");
	}

	private Repository createRepo(BaseSailConfig config, String id) {
		RepositoryImplConfig ric = new SailRepositoryConfig(config);
		manager.addRepositoryConfig(new RepositoryConfig(id, ric));

		return manager.getRepository(id);
	}

	@Test
	public void testDatetimeSubtypesStrict() {
		try (RepositoryConnection conn = strictRepo.getConnection()) {

			conn.add(s1, RDFS.LABEL, gYearLit);
			conn.add(s2, RDFS.LABEL, gYearMonthLit);

			String query = "SELECT * WHERE { ?s rdfs:label ?l . FILTER(?l >= \"2008\"^^xsd:gYear) }";

			List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());

			assertThat(result).hasSize(1);
		}
	}

	@Test
	public void testDatetimeSubtypesExtended() {
		try (RepositoryConnection conn = standardRepo.getConnection()) {
			conn.add(s1, RDFS.LABEL, gYearLit);
			conn.add(s2, RDFS.LABEL, gYearMonthLit);

			String query = "SELECT * WHERE { ?s rdfs:label ?l . FILTER(?l >= \"2008\"^^xsd:gYear) }";

			List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
			assertThat(result).hasSize(2);
		}
	}

	@Test
	public void testQueryEvaluationMode_Override() {
		try (RepositoryConnection conn = strictRepo.getConnection()) {
			conn.add(s1, RDFS.LABEL, gYearLit);
			conn.add(s2, RDFS.LABEL, gYearMonthLit);

			String query = "SELECT * WHERE { ?s rdfs:label ?l . FILTER(?l >= \"2008\"^^xsd:gYear) }";

			{
				conn.begin(QueryEvaluationMode.STANDARD);
				List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
				conn.commit();
				assertThat(result).hasSize(2);
			}

			{
				List<BindingSet> result = QueryResults.asList(conn.prepareTupleQuery(query).evaluate());
				// evaluation should be back to strict mode outside of the previous transaction
				assertThat(result).hasSize(1);
			}

		}
	}

	/**
	 * Gets a configuration object for the base Sail that should be tested.
	 *
	 * @return a {@link BaseSailConfig}.
	 */
	protected abstract BaseSailConfig getBaseSailConfig();
}
