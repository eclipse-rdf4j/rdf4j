/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.io.ResourceUtil;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public abstract class CustomGraphQueryInferencerTest {

	@BeforeAll
	public static void setUpClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void afterClass() {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	protected static class Expectation {

		private final int initialCount, countAfterRemove, subjCount, predCount, objCount;

		public Expectation(int initialCount, int countAfterRemove, int subjCount, int predCount, int objCount) {
			this.initialCount = initialCount;
			this.countAfterRemove = countAfterRemove;
			this.subjCount = subjCount;
			this.predCount = predCount;
			this.objCount = objCount;
		}
	}

	private static final String TEST_DIR_PREFIX = "/custom-query-inferencing/";

	private static final String BASE = "http://foo.org/bar#";

	private static final String PREDICATE = "predicate";

	public static final Stream<Arguments> parameters() {
		Expectation predExpect = new Expectation(8, 2, 0, 2, 0);
		return Stream.of(
				Arguments.of(PREDICATE, predExpect, QueryLanguage.SPARQL),
				Arguments.of("resource", new Expectation(4, 2, 2, 0, 2), QueryLanguage.SPARQL)
		);
	}

	private String initial;

	private String delete;

	protected void runTest(final CustomGraphQueryInferencer inferencer, String resourceFolder, Expectation testData)
			throws RepositoryException, RDFParseException,
			IOException, MalformedQueryException, UpdateExecutionException {
		// Initialize
		Repository sail = new SailRepository(inferencer);
		try (RepositoryConnection connection = sail.getConnection()) {
			connection.begin();
			connection.clear();
			connection.add(new StringReader(initial), BASE, RDFFormat.TURTLE);

			// Test initial inferencer state
			Collection<Value> watchPredicates = inferencer.getWatchPredicates();
			assertThat(watchPredicates).hasSize(testData.predCount);
			Collection<Value> watchObjects = inferencer.getWatchObjects();
			assertThat(watchObjects).hasSize(testData.objCount);
			Collection<Value> watchSubjects = inferencer.getWatchSubjects();
			assertThat(watchSubjects).hasSize(testData.subjCount);
			ValueFactory factory = connection.getValueFactory();
			if (resourceFolder.startsWith(PREDICATE)) {
				assertThat(watchPredicates.contains(factory.createIRI(BASE, "brotherOf"))).isTrue();
				assertThat(watchPredicates.contains(factory.createIRI(BASE, "parentOf"))).isTrue();
			} else {
				IRI bob = factory.createIRI(BASE, "Bob");
				IRI alice = factory.createIRI(BASE, "Alice");
				assertThat(watchSubjects).contains(bob, alice);
				assertThat(watchObjects).contains(bob, alice);
			}

			// Test initial inferencing results
			assertThat(Iterations.asSet(connection.getStatements(null, null, null, true)))
					.hasSize(testData.initialCount);

			// Test results after removing some statements
			connection.prepareUpdate(QueryLanguage.SPARQL, delete).execute();
			assertThat(Iterations.asSet(connection.getStatements(null, null, null, true)))
					.hasSize(testData.countAfterRemove);

			// Tidy up. Storage gets re-used for subsequent tests, so must clear here,
			// in order to properly clear out any inferred statements.
			connection.clear();
			connection.commit();
		}
		sail.shutDown();
	}

	protected CustomGraphQueryInferencer createRepository(boolean withMatchQuery, String resourceFolder,
			QueryLanguage language)
			throws IOException, MalformedQueryException, UnsupportedQueryLanguageException, RepositoryException,
			SailException, RDFParseException {
		String testFolder = TEST_DIR_PREFIX + resourceFolder;
		String rule = ResourceUtil.getString(testFolder + "/rule.rq");
		String match = withMatchQuery ? ResourceUtil.getString(testFolder + "/match.rq") : "";
		initial = ResourceUtil.getString(testFolder + "/initial.ttl");
		delete = ResourceUtil.getString(testFolder + "/delete.ru");

		NotifyingSail store = newSail();

		return new CustomGraphQueryInferencer(store, language, rule, match);
	}

	/**
	 * Gets an instance of the Sail that should be tested. The returned repository must not be initialized.
	 *
	 * @return an uninitialized NotifyingSail.
	 */
	protected abstract NotifyingSail newSail();

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	public void testCustomQueryInference(String resourceFolder, Expectation testData, QueryLanguage language)
			throws RepositoryException, RDFParseException, MalformedQueryException,
			UpdateExecutionException, IOException, UnsupportedQueryLanguageException, SailException {
		runTest(createRepository(true, resourceFolder, language), resourceFolder, testData);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	public void testCustomQueryInferenceImplicitMatcher(String resourceFolder, Expectation testData,
			QueryLanguage language)
			throws RepositoryException, RDFParseException, MalformedQueryException, UpdateExecutionException,
			IOException, UnsupportedQueryLanguageException, SailException {
		runTest(createRepository(false, resourceFolder, language), resourceFolder, testData);
	}

}
