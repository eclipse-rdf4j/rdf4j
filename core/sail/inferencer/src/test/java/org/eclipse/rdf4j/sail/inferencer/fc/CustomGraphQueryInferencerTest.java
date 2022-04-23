/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public abstract class CustomGraphQueryInferencerTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
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

	@Parameters(name = "{0}")
	public static final Collection<Object[]> parameters() {
		Expectation predExpect = new Expectation(8, 2, 0, 2, 0);
		return Arrays.asList(new Object[][] { { PREDICATE, predExpect, QueryLanguage.SPARQL },
				{ "resource", new Expectation(4, 2, 2, 0, 2), QueryLanguage.SPARQL } }
		);
	}

	private String initial;

	private String delete;

	private final String resourceFolder;

	private final Expectation testData;

	private final QueryLanguage language;

	protected void runTest(final CustomGraphQueryInferencer inferencer) throws RepositoryException, RDFParseException,
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

	public CustomGraphQueryInferencerTest(String resourceFolder, Expectation testData, QueryLanguage language) {
		this.resourceFolder = resourceFolder;
		this.testData = testData;
		this.language = language;
	}

	protected CustomGraphQueryInferencer createRepository(boolean withMatchQuery)
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

	@Test
	public void testCustomQueryInference() throws RepositoryException, RDFParseException, MalformedQueryException,
			UpdateExecutionException, IOException, UnsupportedQueryLanguageException, SailException {
		runTest(createRepository(true));
	}

	@Test
	public void testCustomQueryInferenceImplicitMatcher()
			throws RepositoryException, RDFParseException, MalformedQueryException, UpdateExecutionException,
			IOException, UnsupportedQueryLanguageException, SailException {
		runTest(createRepository(false));
	}

}
