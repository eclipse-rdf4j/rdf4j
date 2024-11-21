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
package org.eclipse.testsuite.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.NUM_DOCS;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.PROPERTY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SNIPPET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class AbstractLuceneSailTest {

	protected static final ValueFactory vf = SimpleValueFactory.getInstance();

	public static final String QUERY_STRING;

	public static final IRI SUBJECT_1 = vf.createIRI("urn:subject1");

	public static final IRI SUBJECT_2 = vf.createIRI("urn:subject2");

	public static final IRI SUBJECT_3 = vf.createIRI("urn:subject3");

	public static final IRI SUBJECT_4 = vf.createIRI("urn:subject4");

	public static final IRI SUBJECT_5 = vf.createIRI("urn:subject5");

	public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

	public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

	public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

	public static final IRI PREDICATE_1 = vf.createIRI("urn:predicate1");

	public static final IRI PREDICATE_2 = vf.createIRI("urn:predicate2");

	public static final IRI PREDICATE_3 = vf.createIRI("urn:predicate3");

	protected LuceneSail sail;

	protected Repository repository;

	private final Random random = new Random(43252333);

	static {
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Subject ?Score\n");
		buffer.append("WHERE { ?Subject <" + MATCHES + "> [\n");
		buffer.append("                              <" + QUERY + "> ?Query; \n");
		buffer.append("                              <" + SCORE + "> ?Score ].}");
		QUERY_STRING = buffer.toString();
	}

	protected abstract void configure(LuceneSail sail);

	private void createTestSail(Consumer<LuceneSail> config) {
		if (repository != null) {
			repository.shutDown();
			repository = null;
		}
		// setup a LuceneSail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		sail = new LuceneSail();

		configure(sail);
		config.accept(sail);
		sail.setBaseSail(memoryStore);

		// create a Repository wrapping the LuceneSail
		repository = new SailRepository(sail);

		// add some statements to it
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("one"));
			connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("five"));
			connection.add(SUBJECT_1, PREDICATE_2, vf.createLiteral("two"));
			connection.add(SUBJECT_2, PREDICATE_1, vf.createLiteral("one"));
			connection.add(SUBJECT_2, PREDICATE_2, vf.createLiteral("three"));
			connection.add(SUBJECT_3, PREDICATE_1, vf.createLiteral("four"));
			connection.add(SUBJECT_3, PREDICATE_2, vf.createLiteral("one"));
			connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_1);
			connection.add(SUBJECT_3, PREDICATE_3, SUBJECT_2);
			connection.commit();
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		// set logging, uncomment this to get better logging for debugging
		// org.apache.log4j.BasicConfigurator.configure();
		createTestSail(lc -> {
		});
	}

	@AfterEach
	public void tearDown() throws RepositoryException {
		if (repository != null) {
			repository.shutDown();
			repository = null;
		}
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(false);
	}

	@Test
	public void testTriplesStored() {
		try (RepositoryConnection connection = repository.getConnection()) {
			// are the triples stored in the underlying sail?
			assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_1, vf.createLiteral("one"), false));
			assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_1, vf.createLiteral("five"), false));
			assertTrue(connection.hasStatement(SUBJECT_1, PREDICATE_2, vf.createLiteral("two"), false));
			assertTrue(connection.hasStatement(SUBJECT_2, PREDICATE_1, vf.createLiteral("one"), false));
			assertTrue(connection.hasStatement(SUBJECT_2, PREDICATE_2, vf.createLiteral("three"), false));
			assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_1, vf.createLiteral("four"), false));
			assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_2, vf.createLiteral("one"), false));
			assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_3, SUBJECT_1, false));
			assertTrue(connection.hasStatement(SUBJECT_3, PREDICATE_3, SUBJECT_2, false));
		}
	}

	@Test
	public void testRegularQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			TupleQuery query = connection.prepareTupleQuery(QUERY_STRING);
			query.setBinding("Query", vf.createLiteral("one"));
			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				ArrayList<IRI> uris = new ArrayList<>();

				BindingSet bindings;

				assertTrue(result.hasNext());
				bindings = result.next();
				uris.add((IRI) bindings.getValue("Subject"));
				assertNotNull(bindings.getValue("Score"));

				assertTrue(result.hasNext());
				bindings = result.next();
				uris.add((IRI) bindings.getValue("Subject"));
				assertNotNull(bindings.getValue("Score"));

				assertTrue(result.hasNext());
				bindings = result.next();
				uris.add((IRI) bindings.getValue("Subject"));
				assertNotNull(bindings.getValue("Score"));

				assertFalse(result.hasNext());

				assertTrue(uris.contains(SUBJECT_1));
				assertTrue(uris.contains(SUBJECT_2));
				assertTrue(uris.contains(SUBJECT_3));
			}
		}
	}

	@Test
	public void testComplexQueryOne() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Resource ?Matching ?Score ");
		buffer.append("WHERE { ?Resource <" + PREDICATE_3 + "> ?Matching. ");
		buffer.append(" ?Matching <" + MATCHES + "> [ ");
		buffer.append(" <" + QUERY + "> \"one\"; ");
		buffer.append(" <" + SCORE + "> ?Score ]. } ");
		String q = buffer.toString();

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			TupleQuery query = connection.prepareTupleQuery(q);
			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				List<String> results = new ArrayList<>();
				BindingSet bindings;

				assertTrue(result.hasNext());
				bindings = result.next();
				results.add("<" + (IRI) bindings.getValue("Resource") + ">, " + "<"
						+ (IRI) bindings.getValue("Matching") + ">");
				assertNotNull(bindings.getValue("Score"));

				assertTrue(result.hasNext());
				bindings = result.next();
				results.add("<" + (IRI) bindings.getValue("Resource") + ">, " + "<"
						+ (IRI) bindings.getValue("Matching") + ">");
				assertNotNull(bindings.getValue("Score"));

				assertFalse(result.hasNext());

				assertTrue(results.contains("<" + SUBJECT_3 + ">, <" + SUBJECT_1 + ">"));
				assertTrue(results.contains("<" + SUBJECT_3 + ">, <" + SUBJECT_2 + ">"));
			}
		}
	}

	@Test
	public void testComplexQueryTwo() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Resource ?Matching ?Score ");
		buffer.append("WHERE { ?Resource <" + PREDICATE_3 + "> ?Matching. ");
		buffer.append(" ?Matching <" + MATCHES + "> [ ");
		buffer.append(" <" + QUERY + "> \"two\"; ");
		buffer.append(" <" + SCORE + "> ?Score ]. } ");

		String q = buffer.toString();

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			TupleQuery query = connection.prepareTupleQuery(q);
			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				assertTrue(result.hasNext());
				BindingSet bindings = result.next();
				assertEquals(SUBJECT_3, (IRI) bindings.getValue("Resource"));
				assertEquals(SUBJECT_1, (IRI) bindings.getValue("Matching"));
				assertNotNull(bindings.getValue("Score"));

				assertFalse(result.hasNext());

			}
		}
	}

	@Test
	public void testMultipleLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		String[] queries = new String[] {
				"SELECT ?Resource1 ?Resource2 ?R1Score ?R2Score \n"
						+ "WHERE {\n"
						+ "  ?Resource1 <" + PREDICATE_3 + "> ?Resource2 ; \n"
						+ "             <" + MATCHES + "> [ \n"
						+ "                                  <" + QUERY + "> \"one\"; \n"
						+ "                                  <" + SCORE + "> ?R1Score ]. \n"
						+ "  ?Resource2 <" + MATCHES + "> [ \n"
						+ "                                  <" + QUERY + "> \"one\"; \n"
						+ "                                  <" + SCORE + "> ?R2Score ]. } ",

				"SELECT ?Resource1 ?Resource3 ?R1Score ?R3Score \n"
						+ "WHERE {\n"
						+ "  ?Resource2 ?p21 ?Resource1 . \n"
						+ "  ?Resource2 ?p23 ?Resource3 . \n"
						+ "  ?Resource1 <" + MATCHES + "> [ \n"
						+ "                                 <" + QUERY + "> \"one\"; \n"
						+ "                                 <" + SCORE + "> ?R1Score ]. \n"
						+ "  ?Resource3 <" + MATCHES + "> [ \n"
						+ "                                 <" + QUERY + "> \"one\"; \n"
						+ "                                 <" + SCORE + "> ?R3Score ] ."
						+ " FILTER(?Resource1 != ?Resource3)\n}",

				"SELECT ?Resource1 ?Resource3 ?R1Score ?R3Score \n"
						+ "WHERE { \n"
						+ "  ?Resource2 ?p21 ?Resource1; \n"
						+ "             ?p23 ?Resource3 . \n"
						+ "  ?Resource1 <" + MATCHES + "> [ \n"
						+ "                           <" + QUERY + "> \"one\"; \n"
						+ "                           <" + PROPERTY + "> <" + PREDICATE_1 + ">; \n"
						+ "                           <" + SCORE + "> ?R1Score ].\n"
						+ "  ?Resource3 <" + MATCHES + "> [ \n"
						+ "                           <" + QUERY + "> \"two\"; \n"
						+ "                           <" + PROPERTY + "> <" + PREDICATE_2 + ">; \n"
						+ "                           <" + SCORE + "> ?R3Score ].}",
				"SELECT ?Resource1 ?Resource2 ?R1Score ?R2Score \n"
						+ "WHERE { \n"
						+ "   ?Resource1 <" + MATCHES + "> [ \n"
						+ "                            <" + QUERY + "> \"one\"; \n"
						+ "                            <" + PROPERTY + "> <" + PREDICATE_1 + ">; \n"
						+ "                            <" + SCORE + "> ?R1Score ].\n"
						+ "   ?Resource2 <" + MATCHES + "> [ \n"
						+ "                             <" + QUERY + "> \"one\"; \n"
						+ "                             <" + PROPERTY + "> <" + PREDICATE_2 + ">; \n"
						+ "                             <" + SCORE + "> ?R2Score ].}" };

		ArrayList<List<Map<String, String>>> results = new ArrayList<>();
		ArrayList<Map<String, String>> resultSet;
		Map<String, String> result;

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_3.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_1.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_3.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_2.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_2.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource3", SUBJECT_1.stringValue());
		result.put("R3Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_1.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_3.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource1", SUBJECT_2.stringValue());
		result.put("R1Score", null); // null means: ignore the value
		result.put("Resource2", SUBJECT_3.stringValue());
		result.put("R2Score", null); // null means: ignore the value
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		evaluate(queries, results);
	}

	private void evaluate(String[] queries, ArrayList<List<Map<String, String>>> expectedResults)
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			for (int queryID = 0; queryID < queries.length; queryID++) {
				String sparql = queries[queryID];
				List<Map<String, String>> expectedResultSet = expectedResults.get(queryID);

				// fire the query
				TupleQuery query = connection.prepareTupleQuery(sparql);
				try (TupleQueryResult tqr = query.evaluate()) {

					// check the results
					int actualResults = 0;
					Set<Integer> matched = new HashSet<>();
					while (tqr.hasNext()) {
						BindingSet bs = tqr.next();
						actualResults++;

						boolean matches;
						for (int resultSetID = 0; resultSetID < expectedResultSet.size(); resultSetID++) {
							// ignore results that matched before
							if (matched.contains(resultSetID)) {
								continue;
							}

							// assume it matches
							matches = true;

							// get the result we compare with now
							Map<String, String> expectedResult = new HashMap<>(expectedResultSet.get(resultSetID));

							// get all var names
							Collection<String> vars = new ArrayList<>(expectedResult.keySet());

							// check if all actual results are expected
							for (String var : vars) {
								String expectedVal = expectedResult.get(var);
								Value actualVal = bs.getValue(var);

								if (expectedVal == null) {
									// don't care about the actual value, as long as there is
									// one
									if (actualVal == null) {
										matches = false;
										break;
									}
								} else {
									// compare the values
									if ((actualVal == null) || (expectedVal.compareTo(actualVal.stringValue()) != 0)) {
										matches = false;
										break;
									}
								}

								// remove the matched result so that we do not match it twice
								expectedResult.remove(var);
							}

							// check if expected results were existing
							if (!expectedResult.isEmpty()) {
								matches = false;
							}

							if (matches) {
								matched.add(resultSetID);
								break;
							}
						}
					}

					// the number of matched expected results must be equal to the number
					// of actual results
					assertEquals(expectedResultSet.size(), matched.size(),
							"How many expected results were retrieved for query #" + queryID + "?");
					assertEquals(expectedResultSet.size(), actualResults,
							"How many actual results were retrieved for query #" + queryID + "?");
				}
			}
		}
	}

	@Test
	public void testPredicateLuceneQueries()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		String[] queries = new String[] {
				"SELECT ?Resource ?Score ?Snippet \n"
						+ "WHERE { \n"
						+ "  ?Resource <" + MATCHES + "> [ \n"
						+ "                            <" + QUERY + "> \"one\"; \n"
						+ "                            <" + SCORE + "> ?Score; \n"
						+ "                            <" + SNIPPET + "> ?Snippet ].}",
				"SELECT ?Resource ?Score ?Snippet \n"
						+ "WHERE { \n"
						+ "  ?Resource <" + MATCHES + "> [ \n"
						+ "                            <" + QUERY + "> \"five\"; \n"
						+ "                            <" + SCORE + "> ?Score; \n"
						+ "                            <" + SNIPPET + "> ?Snippet ].}" };

		ArrayList<List<Map<String, String>>> results = new ArrayList<>();
		ArrayList<Map<String, String>> resultSet;
		Map<String, String> result;

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource", SUBJECT_1.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource", SUBJECT_2.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// another possible result
		result = new HashMap<>();
		result.put("Resource", SUBJECT_3.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>one</B>");
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		// create a new result set
		resultSet = new ArrayList<>();

		// one possible result
		result = new HashMap<>();
		result.put("Resource", SUBJECT_1.stringValue());
		result.put("Score", null); // null means: ignore the value
		result.put("Snippet", "<B>five</B>");
		resultSet.add(result);

		// add the results of for the first query
		results.add(resultSet);

		evaluate(queries, results);
	}

	@Test
	public void testSnippetQueries() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		// search for the term "one", but only in predicate 1
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Resource ?Score \n");
		buffer.append("WHERE {\n");
		buffer.append("  ?Resource <" + MATCHES + "> [\n ");
		buffer.append("    <" + QUERY + "> \"one\"; ");
		buffer.append("    <" + PROPERTY + "> <" + PREDICATE_1 + ">; ");
		buffer.append("    <" + SCORE + "> ?Score ].} ");
		String q = buffer.toString();

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire the query
			TupleQuery query = connection.prepareTupleQuery(q);
			try (TupleQueryResult result = query.evaluate()) {
				// check the results
				BindingSet bindings;

				// the first result is subject 1 and has a score
				int results = 0;
				Set<IRI> expectedSubject = new HashSet<>();
				expectedSubject.add(SUBJECT_1);
				expectedSubject.add(SUBJECT_2);
				while (result.hasNext()) {
					results++;
					bindings = result.next();

					// the resource should be among the set of expected subjects, if so,
					// remove it from the set
					assertTrue(expectedSubject.remove(bindings.getValue("Resource")));

					// there should be a score
					assertNotNull(bindings.getValue("Score"));
				}

				// there should have been only 2 results
				assertEquals(2, results);
			}
		}
	}

	/**
	 * Test if the snippets do not accidentially come from the "text" field while we actually expect them to come from
	 * the predicate field.
	 */
	@Test
	public void testSnippetLimitedToPredicate()
			throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// more test-data
		try (RepositoryConnection myconnection = repository.getConnection()) {
			myconnection.begin();
			// we use the string 'charly' as test-case. the snippets should contain
			// "come" and "unicorn"
			// and 'poor' should not be returned if we limit on predicate1
			// and watch http://www.youtube.com/watch?v=Q5im0Ssyyus like 25mio others
			myconnection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("come charly lets go to candy mountain"));
			myconnection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("but the unicorn charly said to goaway"));
			myconnection.add(SUBJECT_1, PREDICATE_2, vf.createLiteral("there was poor charly without a kidney"));
			myconnection.commit();
		}

		{
			// prepare the query
			// search for the term "charly", but only in predicate 1
			StringBuilder buffer = new StringBuilder();
			buffer.append("SELECT ?Resource ?Score ?Snippet \n");
			buffer.append("WHERE { \n");
			buffer.append("  ?Resource <" + MATCHES + "> [\n ");
			buffer.append("    <" + QUERY + "> \"charly\"; ");
			buffer.append("    <" + PROPERTY + "> <" + PREDICATE_1 + ">; ");
			buffer.append("    <" + SNIPPET + "> ?Snippet; ");
			buffer.append("    <" + SCORE + "> ?Score ]. } ");
			String q = buffer.toString();

			// fire the query
			try (RepositoryConnection connection = repository.getConnection()) {
				TupleQuery query = connection.prepareTupleQuery(q);
				try (TupleQueryResult result = query.evaluate()) {

					// check the results
					BindingSet bindings;

					// the first result is subject 1 and has a score
					int results = 0;
					Set<String> expectedSnippetPart = new HashSet<>();
					expectedSnippetPart.add("come");
					expectedSnippetPart.add("unicorn");
					String notexpected = "poor";
					while (result.hasNext()) {
						results++;
						bindings = result.next();

						// the resource should be among the set of expected subjects, if so,
						// remove it from the set
						String snippet = ((Literal) bindings.getValue("Snippet")).stringValue();
						boolean foundexpected = false;
						for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
							String expected = i.next();
							if (snippet.contains(expected)) {
								foundexpected = true;
								i.remove();
							}
						}
						if (snippet.contains(notexpected)) {
							fail("snippet '" + snippet + "' contained value '" + notexpected + "' from predicate "
									+ PREDICATE_2);
						}
						if (!foundexpected) {
							fail("did not find any of the expected strings " + expectedSnippetPart + " in the snippet "
									+ snippet);
						}

						// there should be a score
						assertNotNull(bindings.getValue("Score"));
					}

					// we found all
					assertTrue(expectedSnippetPart.isEmpty(),
							"These were expected but not found: " + expectedSnippetPart);

					assertEquals(2, results, "there should have been 2 results");
				}
			}
		}
		/**
		 * DO THE SAME, BUT WIHTOUT PROPERTY RESTRICTION, JUST TO CHECK
		 */
		{
			// prepare the query
			// search for the term "charly" in all predicates
			StringBuilder buffer = new StringBuilder();
			buffer.append("SELECT ?Resource ?Score ?Snippet \n");
			buffer.append("WHERE { \n");
			buffer.append("  ?Resource <" + MATCHES + "> [\n ");
			buffer.append("    <" + QUERY + "> \"charly\"; ");
			buffer.append("    <" + SNIPPET + "> ?Snippet; ");
			buffer.append("    <" + SCORE + "> ?Score ].} ");
			String q = buffer.toString();

			try (RepositoryConnection connection = repository.getConnection()) {
				// fire the query
				TupleQuery query = connection.prepareTupleQuery(q);
				try (TupleQueryResult result = query.evaluate()) {

					// check the results
					BindingSet bindings;

					// the first result is subject 1 and has a score
					int results = 0;
					Set<String> expectedSnippetPart = new HashSet<>();
					expectedSnippetPart.add("come");
					expectedSnippetPart.add("unicorn");
					expectedSnippetPart.add("poor");

					while (result.hasNext()) {
						results++;
						bindings = result.next();

						// the resource should be among the set of expected subjects, if so,
						// remove it from the set
						String snippet = ((Literal) bindings.getValue("Snippet")).stringValue();
						boolean foundexpected = false;
						for (Iterator<String> i = expectedSnippetPart.iterator(); i.hasNext();) {
							String expected = i.next();
							if (snippet.contains(expected)) {
								foundexpected = true;
								i.remove();
							}
						}
						if (!foundexpected) {
							fail("did not find any of the expected strings " + expectedSnippetPart + " in the snippet "
									+ snippet);
						}

						// there should be a score
						assertNotNull(bindings.getValue("Score"));
					}

					// we found all
					assertTrue(expectedSnippetPart.isEmpty(),
							"These were expected but not found: " + expectedSnippetPart);

					assertEquals(3, results, "there should have been 3 results");

				}
			}
		}
	}

	@Test
	public void testGraphQuery() throws QueryEvaluationException, MalformedQueryException, RepositoryException {
		IRI score = vf.createIRI(LuceneSailSchema.NAMESPACE + "score");
		StringBuilder query = new StringBuilder();

		// here we would expect two links from SUBJECT3 to SUBJECT1 and SUBJECT2
		// and one link from SUBJECT3 to its score
		query.append("PREFIX lucenesail: <" + LuceneSailSchema.NAMESPACE + "> \n");
		query.append("CONSTRUCT { \n");
		query.append("    ?r <" + PREDICATE_3 + "> ?r2 ;\n");
		query.append("       <" + score + "> ?s } \n");
		query.append("WHERE { \n");
		query.append("    ?r lucenesail:matches ?match. ?match lucenesail:query \"four\"; \n");
		query.append("                                         lucenesail:score ?s. \n");
		query.append("    ?r <" + PREDICATE_3 + "> ?r2. }\n");

		int r = 0;
		int n = 0;

		try (RepositoryConnection connection = repository.getConnection()) {
			GraphQuery gq = connection.prepareGraphQuery(query.toString());
			try (GraphQueryResult result = gq.evaluate()) {
				while (result.hasNext()) {
					Statement statement = result.next();
					n++;

					if (statement.getSubject().equals(SUBJECT_3) && statement.getPredicate().equals(PREDICATE_3)
							&& statement.getObject().equals(SUBJECT_1)) {
						r |= 1;
						continue;
					}
					if (statement.getSubject().equals(SUBJECT_3) && statement.getPredicate().equals(PREDICATE_3)
							&& statement.getObject().equals(SUBJECT_2)) {
						r |= 2;
						continue;
					}
					if (statement.getSubject().equals(SUBJECT_3) && statement.getPredicate().equals(score)) {
						r |= 4;
						continue;
					}
				}
				assertEquals(3, n);
				assertEquals(7, r);
			}
		}
	}

	@Test
	public void testQueryWithSpecifiedSubject()
			throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query with the subject pre-specified
			TupleQuery query = connection.prepareTupleQuery(QUERY_STRING);
			query.setBinding("Subject", SUBJECT_1);
			query.setBinding("Query", vf.createLiteral("one"));
			TupleQueryResult result = query.evaluate();

			// check that this subject and only this subject is returned
			assertTrue(result.hasNext());
			BindingSet bindings = result.next();
			assertEquals(SUBJECT_1, (IRI) bindings.getValue("Subject"));
			assertNotNull(bindings.getValue("Score"));
			assertFalse(result.hasNext());
		}
	}

	@Test
	public void testUnionQuery() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String queryStr = "";
		queryStr += "PREFIX search: <http://www.openrdf.org/contrib/lucenesail#> ";
		queryStr += "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> ";
		queryStr += "SELECT DISTINCT ?result { ";
		queryStr += "{ ?result search:matches ?match1 . ";
		queryStr += "  ?match1 search:query 'one' ; ";
		queryStr += "          search:property <urn:predicate1> . }";
		queryStr += " UNION ";
		queryStr += "{ ?result search:matches ?match2 . ";
		queryStr += "  ?match2 search:query 'one' ; ";
		queryStr += "          search:property <urn:predicate2> . } ";
		queryStr += "} ";

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query with the subject pre-specified
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("result", SUBJECT_1);
			try (TupleQueryResult result = query.evaluate()) {
				// check that this subject and only this subject is returned
				BindingSet bs = result.next();
				assertEquals(SUBJECT_1, bs.getValue("result"));
			}
		}
	}

	@Test
	public void testContextHandling() {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(SUBJECT_4, PREDICATE_1, vf.createLiteral("sfourponecone"), CONTEXT_1);
			connection.add(SUBJECT_4, PREDICATE_2, vf.createLiteral("sfourptwocone"), CONTEXT_1);
			connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponecone"), CONTEXT_1);
			connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponectwo"), CONTEXT_2);
			connection.add(SUBJECT_5, PREDICATE_2, vf.createLiteral("sfiveptwoctwo"), CONTEXT_2);
			connection.commit();

			// test querying
			assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
			assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
			assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);

			// blind test to see if this method works:
			assertNoQueryResult("johannesgrenzfurthner");

			// remove a context
			connection.clear(CONTEXT_1);
			connection.commit();
			assertNoQueryResult("sfourponecone");
			assertNoQueryResult("sfourptwocone");
			assertNoQueryResult("sfiveponecone");
			assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
		}
	}

	@Test
	public void testConcurrentReadingAndWriting() {

		try (RepositoryConnection connection = repository.getConnection()) {
			connection.begin();
			connection.add(SUBJECT_1, PREDICATE_1, vf.createLiteral("sfourponecone"), CONTEXT_1);
			connection.add(SUBJECT_2, PREDICATE_1, vf.createLiteral("sfourponecone"), CONTEXT_1);
			connection.commit();

			{
				String queryString = "SELECT ?Resource " + "WHERE { ?Resource <" + MATCHES + "> [ " + " <" + QUERY
						+ "> \"sfourponecone\"].} ";
				TupleQuery query = connection.prepareTupleQuery(queryString);
				TupleQueryResult result = query.evaluate();

				// check the results
				assertTrue(result.hasNext());
				@SuppressWarnings("unused")
				BindingSet bindings = result.next();

				connection.add(SUBJECT_3, PREDICATE_1, vf.createLiteral("sfourponecone"), CONTEXT_1);

				assertTrue(result.hasNext());
				bindings = result.next();
				result.close();
				connection.commit();
			}
			{
				String queryString = "SELECT ?Resource " + "WHERE { ?Resource <" + MATCHES + "> [ " + " <" + QUERY
						+ "> \"sfourponecone\"].} ";
				TupleQuery query = connection.prepareTupleQuery(queryString);
				TupleQueryResult result = query.evaluate();

				// check the results
				assertTrue(result.hasNext());
				@SuppressWarnings("unused")
				BindingSet bindings = result.next();

				connection.add(SUBJECT_3, PREDICATE_1, vf.createLiteral("blubbb"), CONTEXT_1);
				connection.commit();

				assertTrue(result.hasNext());
				bindings = result.next();
				result.close();
			}
		}
	}

	/**
	 * we experienced problems with the NULL context and lucenesail in August 2008
	 *
	 */
	@Test
	public void testNullContextHandling() {
		try (RepositoryConnection connection = repository.getConnection()) {
			connection.add(SUBJECT_4, PREDICATE_1, vf.createLiteral("sfourponecone"));
			connection.add(SUBJECT_4, PREDICATE_2, vf.createLiteral("sfourptwocone"));
			connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponecone"));
			connection.add(SUBJECT_5, PREDICATE_1, vf.createLiteral("sfiveponectwo"), CONTEXT_2);
			connection.add(SUBJECT_5, PREDICATE_2, vf.createLiteral("sfiveptwoctwo"), CONTEXT_2);
			connection.commit();

			// test querying
			assertQueryResult("sfourponecone", PREDICATE_1, SUBJECT_4);
			assertQueryResult("sfourptwocone", PREDICATE_2, SUBJECT_4);
			assertQueryResult("sfiveponecone", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
			// blind test to see if this method works:
			assertNoQueryResult("johannesgrenzfurthner");
			// remove a context
			connection.clear((Resource) null);
			connection.commit();
			assertNoQueryResult("sfourponecone");
			assertNoQueryResult("sfourptwocone");
			assertNoQueryResult("sfiveponecone");
			assertQueryResult("sfiveponectwo", PREDICATE_1, SUBJECT_5);
			assertQueryResult("sfiveptwoctwo", PREDICATE_2, SUBJECT_5);
		}
	}

	@Test
	public void testFuzzyQuery() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		// prepare the query
		// search for the term "one" with 80% fuzzyness
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Resource ?Score \n");
		buffer.append("WHERE { \n");
		buffer.append("  ?Resource <" + MATCHES + "> [\n");
		buffer.append("    <" + QUERY + "> \"one~0.8\"; ");
		buffer.append("    <" + SCORE + "> ?Score ].} ");
		String q = buffer.toString();

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire the query
			TupleQuery query = connection.prepareTupleQuery(q);
			try (TupleQueryResult result = query.evaluate()) {

				// check the results
				BindingSet bindings;

				// the first result is subject 1 and has a score
				int results = 0;
				Set<IRI> expectedSubject = new HashSet<>();
				expectedSubject.add(SUBJECT_1);
				expectedSubject.add(SUBJECT_2);
				expectedSubject.add(SUBJECT_3);
				while (result.hasNext()) {
					results++;
					bindings = result.next();

					// the resource should be among the set of expected subjects, if so,
					// remove it from the set
					assertTrue(expectedSubject.remove((IRI) bindings.getValue("Resource")));

					// there should be a score
					assertNotNull(bindings.getValue("Score"));
				}

				// there should have been 3 results
				assertEquals(3, results);

			}
		}
	}

	@Test
	public void testReindexing() {
		sail.reindex();
		testComplexQueryTwo();
	}

	@Test
	public void testPropertyVar() throws MalformedQueryException, RepositoryException, QueryEvaluationException {
		StringBuilder buffer = new StringBuilder();
		buffer.append("SELECT ?Resource ?Property \n");
		buffer.append("WHERE {\n");
		buffer.append("  ?Resource <" + MATCHES + "> [\n ");
		buffer.append("    <" + QUERY + "> \"one\";\n ");
		buffer.append("    <" + PROPERTY + "> ?Property ]. } ");
		String q = buffer.toString();

		try (RepositoryConnection connection = repository.getConnection()) {
			// fire the query
			TupleQuery query = connection.prepareTupleQuery(q);
			try (TupleQueryResult result = query.evaluate()) {

				int results = 0;
				Map<IRI, IRI> expectedSubject = new HashMap<>();
				expectedSubject.put(SUBJECT_1, PREDICATE_1);
				expectedSubject.put(SUBJECT_2, PREDICATE_1);
				expectedSubject.put(SUBJECT_3, PREDICATE_2);
				while (result.hasNext()) {
					results++;
					BindingSet bindings = result.next();

					// the resource should be among the set of expected subjects, if so,
					// remove it from the set
					Value subject = bindings.getValue("Resource");
					IRI expectedProperty = expectedSubject.remove(subject);
					assertEquals(expectedProperty, bindings.getValue("Property"), "For subject " + subject);
				}

				// there should have been 3 results
				assertEquals(3, results);
			}
		}
	}

	@Test
	public void testMultithreadedAdd() throws InterruptedException {
		int numThreads = 3;
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(numThreads);
		final Set<Throwable> exceptions = ConcurrentHashMap.newKeySet();
		for (int i = 0; i < numThreads; i++) {
			new Thread(new Runnable() {

				private final long iterationCount = 10 + Math.round(random.nextDouble() * 100);

				@Override
				public void run() {
					try (RepositoryConnection con = repository.getConnection()) {
						startLatch.await();
						for (long i = 0; i < iterationCount; i++) {
							con.add(vf.createIRI("ex:" + i), vf.createIRI("ex:prop" + i % 3), vf.createLiteral(i));
						}
					} catch (Throwable e) {
						exceptions.add(e);
						throw new AssertionError(e);
					} finally {
						endLatch.countDown();
					}
				}
			}).start();
		}
		startLatch.countDown();
		endLatch.await();
		for (Throwable e : exceptions) {
			e.printStackTrace(System.err);
		}
		assertEquals(0, exceptions.size(), "Exceptions occurred during testMultithreadedAdd, see stacktraces above");
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	public void testDefaultNumDocsResult(int numDoc) {
		createTestSail(lc -> lc.setParameter(LuceneSail.DEFAULT_NUM_DOCS_KEY, String.valueOf(numDoc)));
		Repositories.consumeNoTransaction(repository, conn -> {
			try (TupleQueryResult res = conn.prepareTupleQuery(
					"SELECT ?Resource {\n"
							+ "  ?Resource <" + MATCHES + "> [\n "
							+ "    <" + QUERY + "> \"one\"\n "
							+ "  ]. } "
			).evaluate()) {
				for (int k = 0; k < numDoc; k++) {
					assertTrue(res.hasNext(), "missing result #" + k);
					res.next();
				}
				if (res.hasNext()) {
					StringBuilder b = new StringBuilder();
					int r = 0;
					do {
						b.append("\n#").append(r++).append(res.next());
					} while (res.hasNext());
					fail("can't have more than " + numDoc + " result(s)" + b);
				}
			}
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	public void testMaxNumDocsResult(int numDoc) {
		createTestSail(lc -> lc.setParameter(LuceneSail.MAX_DOCUMENTS_KEY, String.valueOf(numDoc)));
		Repositories.consumeNoTransaction(repository, conn -> {
			try (TupleQueryResult res = conn.prepareTupleQuery(
					"SELECT ?Resource {\n"
							+ "  ?Resource <" + MATCHES + "> [\n "
							+ "    <" + QUERY + "> \"one\";\n "
							+ "    <" + NUM_DOCS + "> 3;\n "
							+ "  ]. } "
			).evaluate()) {
				for (int k = 0; k < numDoc; k++) {
					assertTrue(res.hasNext(), "missing result #" + k);
					res.next();
				}
				if (res.hasNext()) {
					StringBuilder b = new StringBuilder();
					int r = 0;
					do {
						b.append("\n#").append(r++).append(res.next());
					} while (res.hasNext());
					fail("can't have more than " + numDoc + " result(s)" + b);
				}
			}
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3 })
	public void testNumDocsResult(int numDoc) {
		Repositories.consumeNoTransaction(repository, conn -> {
			try (TupleQueryResult res = conn.prepareTupleQuery(
					"SELECT ?Resource {\n"
							+ "  ?Resource <" + MATCHES + "> [\n "
							+ "    <" + QUERY + "> \"one\";\n "
							+ "    <" + NUM_DOCS + "> " + numDoc + ";\n "
							+ "  ]. } "
			).evaluate()) {
				for (int k = 0; k < numDoc; k++) {
					assertTrue(res.hasNext(), "missing result #" + k);
					res.next();
				}
				if (res.hasNext()) {
					StringBuilder b = new StringBuilder();
					int r = 0;
					do {
						b.append("\n#").append(r++).append(res.next());
					} while (res.hasNext());
					fail("can't have more than " + numDoc + " result(s)" + b);
				}
			}
		});
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -2, -3 })
	public void testNumDocsResultNegative(int numDocs) {
		// assert that negative values cause an assertion error
		assertThrows(AssertionError.class, () -> {
			Repositories.consumeNoTransaction(repository, conn -> {
				try (TupleQueryResult res = conn.prepareTupleQuery(
						"SELECT ?Resource {\n"
								+ "  ?Resource <" + MATCHES + "> [\n "
								+ "    <" + QUERY + "> \"one\";\n "
								+ "    <" + NUM_DOCS + "> " + numDocs + ";\n "
								+ "  ]. } "
				).evaluate()) {
					assertFalse(res.hasNext());
				}
			});
		});
	}

	protected void assertQueryResult(String literal, IRI predicate, Resource resultUri) {
		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			String queryString = "SELECT ?Resource WHERE { ?Resource <" + MATCHES + "> [ " + " <" + QUERY + "> \""
					+ literal + "\" ]. } ";
			TupleQuery query = connection.prepareTupleQuery(queryString);
			try (TupleQueryResult result = query.evaluate()) {
				// check the result
				assertTrue(result.hasNext(),
						"query for literal '" + literal + " did not return any results, expected was " + resultUri);
				BindingSet bindings = result.next();
				assertEquals(resultUri, bindings.getValue("Resource"),
						"query for literal '" + literal + " did not return the expected resource");
				assertFalse(result.hasNext());
			}
		}
	}

	protected void assertNoQueryResult(String literal) {
		try (RepositoryConnection connection = repository.getConnection()) {
			// fire a query for all subjects with a given term
			String queryString = "SELECT ?Resource WHERE { ?Resource <" + MATCHES + "> [ " + " <" + QUERY + "> \""
					+ literal + "\" ]. } ";
			TupleQuery query = connection.prepareTupleQuery(queryString);
			try (TupleQueryResult result = query.evaluate()) {

				// check the result
				assertFalse(result.hasNext(),
						"query for literal '" + literal + " did return results, which was not expected.");
			}
		}
	}
}
