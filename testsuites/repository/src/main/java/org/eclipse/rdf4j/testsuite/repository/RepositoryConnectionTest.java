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
package org.eclipse.rdf4j.testsuite.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Namespaces;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class RepositoryConnectionTest {
	@BeforeAll
	public static void setUpClass() {
		// Turn off debugging for this test, as the cleanup processes are working correctly,
		// but they debug a lot of information in testOrderByQueriesAreInterrupable
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	// FIXME: Cannot use EnumSource because this method is "overridden"
	public static IsolationLevel[] parameters() {
		return IsolationLevels.values();
	}

	private static final String SPARQL_DEL_ALL = "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o }";

	private static final String URN_TEST_O1 = "urn:test:o1";

	private static final String URN_TEST_S1 = "urn:test:s1";

	private static final String URN_TEST_P2 = "urn:test:p2";

	private static final String URN_TEST_P1 = "urn:test:p1";

	private static final String URN_PRED = "urn:pred";

	private static final String RDF_PREFIX = "rdf";

	private static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";

	private static final String RDFS_PREFIX = "rdfs";

	private static final String EXAMPLE_NS = "http://example.org/";

	private static final String EXAMPLE = "example";

	private static final String ASK = "ASK ";

	private static final String PREFIX_FOAF = "PREFIX foaf: <";

	private static final String PERSON = "person";

	private static final String UNEXPECTED_TYPE = "unexpected query object type: ";

	private static final String UNSUPPORTED_OP = "unsupported operation: ";

	private static final String NEWLY_ADDED = "Repository should contain newly added statement";

	private static final String MBOX = "mbox";

	private static final String NAME = "name";

	protected static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	public static final String TEST_DIR_PREFIX = "/testcases/";

	protected Repository testRepository;

	protected RepositoryConnection testCon;

	protected RepositoryConnection testCon2;

	protected ValueFactory vf;

	protected Resource bob;

	protected Resource alice;

	protected Resource alexander;

	protected IRI name;

	protected IRI mbox;

	protected final IRI publisher = DC.PUBLISHER;

	protected IRI unknownContext;

	protected IRI context1;

	protected IRI context2;

	protected Literal nameAlice;

	protected Literal nameBob;

	protected Literal mboxAlice;

	protected Literal mboxBob;

	protected Literal Александър;

	@BeforeEach
	public void setUp(@TempDir File dataDir) throws Exception {
		testRepository = createRepository(dataDir);

		testCon = testRepository.getConnection();
		testCon.begin();
		testCon.clear();
		testCon.clearNamespaces();
		testCon.commit();
	}

	protected void setupTest(IsolationLevel level) {
		testCon.setIsolationLevel(level);

		testCon2 = testRepository.getConnection();
		testCon2.setIsolationLevel(level);

		vf = testRepository.getValueFactory();

		// Initialize values
		bob = vf.createBNode();
		alice = vf.createBNode();
		alexander = vf.createBNode();

		name = vf.createIRI(FOAF_NS + NAME);
		mbox = vf.createIRI(FOAF_NS + MBOX);

		nameAlice = vf.createLiteral("Alice");
		nameBob = vf.createLiteral("Bob");

		mboxAlice = vf.createLiteral("alice@example.org");
		mboxBob = vf.createLiteral("bob@example.org");

		Александър = vf.createLiteral("Александър");

		unknownContext = vf.createIRI("urn:unknownContext");

		context1 = vf.createIRI("urn:x-local:graph1");
		context2 = vf.createIRI("urn:x-local:graph2");
	}

	@AfterEach
	public void tearDown() {
		try {
			testCon2.close();
		} finally {
			try {
				testCon.close();
			} finally {
				testRepository.shutDown();
			}
		}
	}

	/**
	 * Gets an (uninitialized) instance of the repository that should be tested.
	 *
	 * @return an uninitialized repository.
	 */
	protected abstract Repository createRepository(File dataDir) throws Exception;

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddStatement(IsolationLevel level, @TempDir File dataDir) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false), NEWLY_ADDED);

		Statement statement = vf.createStatement(alice, name, nameAlice);
		testCon.add(statement);

		assertTrue(testCon.hasStatement(statement, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false), NEWLY_ADDED);

		Repository tempRep = createRepository(dataDir);
		try (RepositoryConnection con = tempRep.getConnection()) {

			con.add(testCon.getStatements(null, null, null, false));

			assertTrue(con.hasStatement(bob, name, nameBob, false),
					"Temp Repository should contain newly added statement");
		} finally {
			tempRep.shutDown();
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddStatementWithContext(IsolationLevel level) {
		setupTest(level);

		Statement statement = vf.createStatement(alice, name, nameAlice, context1);
		testCon.add(statement);

		assertTrue(testCon.hasStatement(statement, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(alice, name, nameAlice, false, context1), NEWLY_ADDED);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddLiteralWithNewline(IsolationLevel level) {
		setupTest(level);

		Literal test = vf.createLiteral("this is a test\n");
		testCon.add(bob, RDFS.LABEL, test);

		assertTrue(testCon.hasStatement(bob, RDFS.LABEL, test, false), NEWLY_ADDED);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testTransactionIsolation(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.READ_UNCOMMITTED.isCompatibleWith(level)) {
			return;
		}
		testCon.begin();
		testCon.add(bob, name, nameBob);
		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isTrue();
		assertThat(testCon2.hasStatement(bob, name, nameBob, false)).isFalse();
		testCon.commit();
		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isTrue();
		assertThat(testCon2.hasStatement(bob, name, nameBob, false)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddReader(IsolationLevel level) throws Exception {
		setupTest(level);

		try (Reader defaultGraph = new InputStreamReader(
				RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl"),
				StandardCharsets.UTF_8)) {
			testCon.add(defaultGraph, "", RDFFormat.TURTLE);
		}
		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);

		// add file graph1.ttl to context1
		try (InputStream graph1Stream = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "graph1.ttl");
				Reader graph1 = new InputStreamReader(graph1Stream, StandardCharsets.UTF_8)) {
			testCon.add(graph1, "", RDFFormat.TURTLE, context1);
		}

		// add file graph2.ttl to context2
		try (InputStream graph2Stream = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "graph2.ttl");
				Reader graph2 = new InputStreamReader(graph2Stream, StandardCharsets.UTF_8)) {
			testCon.add(graph2, "", RDFFormat.TURTLE, context2);
		}
		assertTrue(testCon.hasStatement(null, name, nameAlice, false), "alice should be known in the store");
		assertFalse(testCon.hasStatement(null, name, nameAlice, false, context1),
				"alice should not be known in context1");
		assertTrue(testCon.hasStatement(null, name, nameAlice, false, context2), "alice should be known in context2");
		assertTrue(testCon.hasStatement(null, name, nameBob, false), "bob should be known in the store");
		assertFalse(testCon.hasStatement(null, name, nameBob, false, context2), "bob should not be known in context2");
		assertTrue(testCon.hasStatement(null, name, nameBob, false, context1), "bib should be known in context1");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddInputStream(IsolationLevel level) throws Exception {
		setupTest(level);

		// add file default-graph.ttl to repository, no context
		try (InputStream defaultGraph = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl")) {
			testCon.add(defaultGraph, "", RDFFormat.TURTLE);
		}
		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);

		// add file graph1.ttl to context1
		try (InputStream graph1 = RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "graph1.ttl")) {
			testCon.add(graph1, "", RDFFormat.TURTLE, context1);
		}

		// add file graph2.ttl to context2
		try (InputStream graph2 = RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "graph2.ttl")) {
			testCon.add(graph2, "", RDFFormat.TURTLE, context2);
		}
		assertTrue(testCon.hasStatement(null, name, nameAlice, false), "alice should be known in the store");
		assertFalse(testCon.hasStatement(null, name, nameAlice, false, context1),
				"alice should not be known in context1");
		assertTrue(testCon.hasStatement(null, name, nameAlice, false, context2), "alice should be known in context2");
		assertTrue(testCon.hasStatement(null, name, nameBob, false), "bob should be known in the store");
		assertFalse(testCon.hasStatement(null, name, nameBob, false, context2), "bob should not be known in context2");
		assertTrue(testCon.hasStatement(null, name, nameBob, false, context1), "bib should be known in context1");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddInputStreamInTxn(IsolationLevel level) throws Exception {
		setupTest(level);

		// add file default-graph.ttl to repository, no context
		try (InputStream defaultGraph = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl")) {
			testCon.begin();
			testCon.add(defaultGraph, "", RDFFormat.TURTLE);
			testCon.commit();
		}
		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddReaderInTxn(IsolationLevel level) throws Exception {
		setupTest(level);

		// add file default-graph.ttl to repository, no context

		try (InputStream defaultGraph = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl");
				InputStreamReader reader = new InputStreamReader(defaultGraph)) {
			testCon.begin();
			testCon.add(reader, "", RDFFormat.TURTLE);
			testCon.commit();
		}
		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddGzipInputStream(IsolationLevel level) throws Exception {
		setupTest(level);

		// add file default-graph.ttl to repository, no context
		try (InputStream defaultGraph = RepositoryConnectionTest.class
				.getResourceAsStream(TEST_DIR_PREFIX + "default-graph.ttl.gz")) {
			testCon.add(defaultGraph, "", RDFFormat.TURTLE);
		}

		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddZipFile(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "graphs.zip"), "",
				RDFFormat.TURTLE);
		assertTrue(testCon.hasStatement(null, publisher, nameBob, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, publisher, nameAlice, false), NEWLY_ADDED);
		assertTrue(testCon.hasStatement(null, name, nameAlice, false), "alice should be known in the store");
		assertTrue(testCon.hasStatement(null, name, nameBob, false), "bob should be known in the store");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddMalformedLiteralsDefaultConfig(IsolationLevel level) throws Exception {
		setupTest(level);

		try {
			testCon.add(RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "malformed-literals.ttl"),
					"", RDFFormat.TURTLE);
			fail("upload of malformed literals should fail with error in default configuration");
		} catch (RDFParseException e) {
			// ignore, as expected
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddMalformedLiteralsStrictConfig(IsolationLevel level) throws Exception {
		setupTest(level);

		Set<RioSetting<?>> empty = Collections.emptySet();
		testCon.getParserConfig().setNonFatalErrors(empty);

		try {
			testCon.add(RepositoryConnectionTest.class.getResourceAsStream(TEST_DIR_PREFIX + "malformed-literals.ttl"),
					"", RDFFormat.TURTLE);
			fail("upload of malformed literals should fail with error in strict configuration");

		} catch (RDFParseException e) {
			// ingnore, as expected.
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAutoCommit(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(alice, name, nameAlice);

		assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
				"Uncommitted update should be visible to own connection");

		testCon.commit();

		assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
				"Repository should contain statement after commit");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRollback(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		testCon.begin();
		testCon.add(alice, name, nameAlice);

		assertTrue(testCon.hasStatement(alice, name, nameAlice, false),
				"Uncommitted updates should be visible to own connection");

		testCon.rollback();

		assertFalse(testCon.hasStatement(alice, name, nameAlice, false),
				"Repository should not contain statement after rollback");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSimpleTupleQuery(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" SELECT ?name ?mbox" +
				" WHERE { [] foaf:name ?name;" +
				"            foaf:mbox ?mbox. }";

		try (TupleQueryResult result = testCon.prepareTupleQuery(queryBuilder).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertThat(solution.hasBinding(NAME)).isTrue();
				assertThat(solution.hasBinding(MBOX)).isTrue();
				Value nameResult = solution.getValue(NAME);
				Value mboxResult = solution.getValue(MBOX);
				assertThat(nameResult).isIn(nameAlice, nameBob);
				assertThat(mboxResult).isIn(mboxAlice, mboxBob);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testPrepareSPARQLQuery(IsolationLevel level) {
		setupTest(level);

		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name ?y . }");

		try {
			testCon.prepareQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		} catch (UnsupportedOperationException e) {
			fail(UNSUPPORTED_OP + e.getMessage());
		} catch (ClassCastException e) {
			fail(UNEXPECTED_TYPE + e.getMessage());
		}

		queryBuilder = new StringBuilder();
		queryBuilder.append(" BASE <http://base.uri>");
		queryBuilder.append(" PREFIX foaf: <" + FOAF_NS + ">");
		queryBuilder.append(" PREFIX ex: <http://example.org/>");
		queryBuilder.append(" PREFIX : <http://example.org/foo#>");
		queryBuilder.append(" SELECT ?person");
		queryBuilder.append(" WHERE { ?person foaf:name ?y . }");

		try {
			testCon.prepareQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		} catch (UnsupportedOperationException e) {
			fail(UNSUPPORTED_OP + e.getMessage());
		} catch (ClassCastException e) {
			fail(UNEXPECTED_TYPE + e.getMessage());
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSimpleTupleQueryUnicode(IsolationLevel level) {
		setupTest(level);

		testCon.add(alexander, name, Александър);
		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" SELECT ?person" +
				" WHERE { ?person foaf:name \"" + Александър.getLabel() + "\". }";

		try (TupleQueryResult result = testCon.prepareTupleQuery(queryBuilder).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertThat(solution.hasBinding(PERSON)).isTrue();
				assertThat(solution.getValue(PERSON)).isEqualTo(alexander);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testPreparedTupleQuery(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" SELECT ?name ?mbox \n" +
				" WHERE { [] foaf:name ?name; \n" +
				"            foaf:mbox ?mbox . }";
		TupleQuery query = testCon.prepareTupleQuery(queryBuilder);
		query.setBinding(NAME, nameBob);

		try (TupleQueryResult result = query.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertThat(solution.hasBinding(NAME)).isTrue();
				assertThat(solution.hasBinding(MBOX)).isTrue();
				Value nameResult = solution.getValue(NAME);
				Value mboxResult = solution.getValue(MBOX);
				assertEquals(nameBob, nameResult, "unexpected value for name: " + nameResult);
				assertEquals(mboxBob, mboxResult, "unexpected value for mbox: " + mboxResult);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testPreparedTupleQueryUnicode(IsolationLevel level) {
		setupTest(level);

		testCon.add(alexander, name, Александър);

		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" SELECT ?person \n" +
				" WHERE { ?person foaf:name ?name . }";

		TupleQuery query = testCon.prepareTupleQuery(queryBuilder);
		query.setBinding(NAME, Александър);

		try (TupleQueryResult result = query.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();

			while (result.hasNext()) {
				BindingSet solution = result.next();
				assertThat(solution.hasBinding(PERSON)).isTrue();
				assertThat(solution.getValue(PERSON)).isEqualTo(alexander);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSimpleGraphQuery(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" CONSTRUCT\n" +
				" WHERE { [] foaf:name ?name;\n" +
				"            foaf:mbox ?mbox.}";

		try (GraphQueryResult result = testCon.prepareGraphQuery(queryBuilder).evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();

			while (result.hasNext()) {
				Statement st = result.next();
				if (name.equals(st.getPredicate())) {
					assertThat(st.getObject()).isIn(nameAlice, nameBob);
				} else {
					assertThat(st.getPredicate()).isEqualTo(mbox);
					assertThat(st.getObject()).isIn(mboxAlice, mboxBob);
				}
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testPreparedGraphQuery(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		testCon.commit();
		String queryBuilder = " PREFIX foaf: <" + FOAF_NS + "> \n" +
				" CONSTRUCT\n" +
				" WHERE { [] foaf:name ?name;\n" +
				"            foaf:mbox ?mbox.}";
		GraphQuery query = testCon.prepareGraphQuery(queryBuilder);
		query.setBinding(NAME, nameBob);

		try (GraphQueryResult result = query.evaluate()) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isTrue();
			while (result.hasNext()) {
				Statement st = result.next();
				IRI predicate = st.getPredicate();
				assertThat(predicate).isIn(name, mbox);
				Value object = st.getObject();
				if (name.equals(predicate)) {
					assertEquals(nameBob, object, "unexpected value for name: " + object);
				} else {
					assertThat(predicate).isEqualTo(mbox);
					assertEquals(mboxBob, object, "unexpected value for mbox: " + object);
				}
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSimpleBooleanQuery(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		String queryBuilder = PREFIX_FOAF + FOAF_NS + "> " +
				ASK +
				"{ ?p foaf:name ?name }";

		boolean exists = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder).evaluate();

		assertThat(exists).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testPreparedBooleanQuery(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);

		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);

		String queryBuilder = PREFIX_FOAF + FOAF_NS + "> " +
				ASK +
				"{ ?p foaf:name ?name }";

		BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder);
		query.setBinding(NAME, nameBob);

		assertThat(query.evaluate()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testDataset(IsolationLevel level) {
		setupTest(level);

		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append(PREFIX_FOAF + FOAF_NS + "> ");
		queryBuilder.append(ASK);
		queryBuilder.append("{ ?p foaf:name ?name }");
		BooleanQuery query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding(NAME, nameBob);
		assertThat(query.evaluate()).isTrue();
		SimpleDataset dataset = new SimpleDataset();

		// default graph: {context1}
		dataset.addDefaultGraph(context1);
		query.setDataset(dataset);
		assertThat(query.evaluate()).isTrue();

		// default graph: {context1, context2}
		dataset.addDefaultGraph(context2);
		query.setDataset(dataset);
		assertThat(query.evaluate()).isTrue();

		// default graph: {context2}
		dataset.removeDefaultGraph(context1);
		query.setDataset(dataset);
		assertThat(query.evaluate()).isFalse();
		queryBuilder.setLength(0);
		queryBuilder.append(PREFIX_FOAF + FOAF_NS + "> ");
		queryBuilder.append(ASK);
		queryBuilder.append("{ GRAPH ?g { ?p foaf:name ?name } }");
		query = testCon.prepareBooleanQuery(QueryLanguage.SPARQL, queryBuilder.toString());
		query.setBinding(NAME, nameBob);

		// default graph: {context2}; named graph: {}
		query.setDataset(dataset);
		assertThat(query.evaluate()).isFalse();

		// default graph: {context1, context2}; named graph: {context2}
		dataset.addDefaultGraph(context1);
		dataset.addNamedGraph(context2);
		query.setDataset(dataset);
		assertThat(query.evaluate()).isFalse();

		// default graph: {context1, context2}; named graph: {context1,
		// context2}
		dataset.addNamedGraph(context1);
		query.setDataset(dataset);
		assertThat(query.evaluate()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatements(IsolationLevel level) {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false), "Repository should contain statement");

		try (RepositoryResult<Statement> result = testCon.getStatements(null, name, null, false)) {
			assertNotNull(result, "Iterator should not be null");
			assertTrue(result.hasNext(), "Iterator should not be empty");

			while (result.hasNext()) {
				Statement st = result.next();
				assertNull(st.getContext(), "Statement should not be in a context ");
				assertEquals(st.getPredicate(), name, "Statement predicate should be equal to name ");
			}
		}

		List<Statement> list = Iterations.addAll(testCon.getStatements(null, name, null, false), new ArrayList<>());

		assertNotNull(list, "List should not be null");
		assertFalse(list.isEmpty(), "List should not be empty");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatementsIterable(IsolationLevel level) {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		assertTrue(testCon.hasStatement(bob, name, nameBob, false), "Repository should contain statement");

		try (RepositoryResult<Statement> result = testCon.getStatements(null, name, null, false)) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat((Iterable<?>) result).isNotEmpty();

			for (Statement st : result) {
				assertThat(st.getContext()).isNull();
				assertThat(st.getPredicate()).isEqualTo(name);
			}

			assertThat((Iterable<?>) result).isEmpty();
			assertThat(result.isClosed()).isTrue();
		}

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatementsMalformedTypedLiteral(IsolationLevel level) {
		setupTest(level);

		Literal invalidIntegerLiteral = vf.createLiteral("the number four", XSD.INTEGER);
		try {
			IRI pred = vf.createIRI(URN_PRED);
			testCon.add(bob, pred, invalidIntegerLiteral);

			try (RepositoryResult<Statement> statements = testCon.getStatements(bob, pred, null, true)) {
				assertNotNull(statements);
				assertTrue(statements.hasNext());
				Statement st = statements.next();
				assertTrue(st.getObject() instanceof Literal);
				assertEquals(st.getObject(), invalidIntegerLiteral);
			}
		} catch (RepositoryException e) {
			// shouldn't happen
			fail(e.getMessage());
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatementsMalformedLanguageLiteral(IsolationLevel level) {
		setupTest(level);

		Literal invalidLanguageLiteral = vf.createLiteral("the number four", "en_us");
		try {
			IRI pred = vf.createIRI(URN_PRED);
			testCon.add(bob, pred, invalidLanguageLiteral);

			try (RepositoryResult<Statement> statements = testCon.getStatements(bob, pred, null, true)) {
				assertNotNull(statements);
				assertTrue(statements.hasNext());
				Statement st = statements.next();
				assertTrue(st.getObject() instanceof Literal);
				assertEquals(st.getObject(), invalidLanguageLiteral);
			}
		} catch (RepositoryException e) {
			e.printStackTrace();
			// shouldn't happen
			fail(e.getMessage());
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatementsInSingleContext(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.commit();
		assertTrue(testCon.hasStatement(bob, name, nameBob, false), "Repository should contain statement");
		assertTrue(testCon.hasStatement(bob, name, nameBob, false, context1),
				"Repository should contain statement in context1");
		assertFalse(testCon.hasStatement(bob, name, nameBob, false, context2),
				"Repository should not contain statement in context2");

		// Check handling of getStatements without context IDs
		try (RepositoryResult<Statement> result = testCon.getStatements(bob, name, null, false)) {
			while (result.hasNext()) {
				Statement st = result.next();
				assertThat(st.getSubject()).isEqualTo(bob);
				assertThat(st.getPredicate()).isEqualTo(name);
				assertThat(st.getObject()).isEqualTo(nameBob);
				assertThat(st.getContext()).isEqualTo(context1);
			}
		}

		// Check handling of getStatements with a known context ID
		try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, context1)) {
			while (result.hasNext()) {
				Statement st = result.next();
				assertThat(st.getContext()).isEqualTo(context1);
			}
		}

		// Check handling of getStatements with an unknown context ID
		try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false, unknownContext)) {
			assertThat((Iterable<?>) result).isNotNull();
			assertThat(result.hasNext()).isFalse();
		}

		try (RepositoryResult<Statement> result = testCon.getStatements(null, name, null, false, context1)) {
			List<Statement> list = Iterations.addAll(result, new ArrayList<>());
			assertNotNull(list, "List should not be null");
			assertFalse(list.isEmpty(), "List should not be empty");
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetStatementsInMultipleContexts(IsolationLevel level) {
		setupTest(level);

		testCon.clear();

		testCon.begin();
		testCon.add(alice, name, nameAlice, context2);
		testCon.add(alice, mbox, mboxAlice, context2);
		testCon.add(context2, publisher, nameAlice);
		testCon.commit();

		// get statements with either no context or context2
		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, null, context2)) {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				assertThat(st.getContext()).isIn(null, context2);
			}

			assertEquals(3, count, "there should be three statements");
		}

		// get all statements with context1 or context2. Note that context1 and
		// context2 are both known
		// in the store because they have been created through the store's own
		// value vf.
		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, context1, context2)) {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2
				assertThat(st.getContext()).isEqualTo(context2);
			}
			assertEquals(2, count, "there should be two statements");
		}

		// get all statements with unknownContext or context2.
		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, unknownContext,
				context2)) {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2
				assertThat(st.getContext()).isEqualTo(context2);
			}
			assertEquals(2, count, "there should be two statements");
		}

		// add statements to context1
		testCon.begin();
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, mbox, mboxBob, context1);
		testCon.add(context1, publisher, nameBob);
		testCon.commit();

		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, context1)) {
			assertThat((Iterable<?>) iter).isNotNull();
			assertThat(iter.hasNext()).isTrue();
		}

		// get statements with either no context or context2
		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, null, context2)) {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				// we should have _only_ statements from context2, or without
				// context
				assertThat(st.getContext()).isIn(null, context2);
			}
			assertEquals(4, count, "there should be four statements");
		}

		// get all statements with context1 or context2
		try (RepositoryResult<Statement> iter = testCon.getStatements(null, null, null, false, context1, context2)) {
			int count = 0;
			while (iter.hasNext()) {
				count++;
				Statement st = iter.next();
				assertThat(st.getContext()).isIn(context1, context2);
			}
			assertEquals(4, count, "there should be four statements");
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testDuplicateFilter(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(bob, name, nameBob);
		testCon.add(bob, name, nameBob, context1);
		testCon.add(bob, name, nameBob, context2);
		testCon.commit();

		try (RepositoryResult<Statement> result = testCon.getStatements(bob, name, null, true)) {
			result.enableDuplicateFilter();
			int count = 0;
			while (result.hasNext()) {
				result.next();
				count++;
			}
			// TODO now that statement.equals includes context, the above three statements are considered distinct.
			// This duplicate filter test has become meaningless since it is _expected_ that nothing gets filtered out.
			// We should look into reimplementing/renaming the enableDuplicateFilter to ignore context.
			assertThat(count).isEqualTo(3);
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRemoveStatements(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(bob, name, nameBob);
		testCon.add(alice, name, nameAlice);
		testCon.commit();

		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isTrue();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isTrue();

		testCon.remove(bob, name, nameBob);

		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isFalse();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isTrue();

		testCon.remove(alice, null, null);
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isFalse();
		assertThat(testCon.isEmpty()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRemoveStatementWithContext(IsolationLevel level) {
		setupTest(level);

		Statement statement = vf.createStatement(alice, name, nameAlice, context1);
		testCon.add(statement);

		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isTrue();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false, context1)).isTrue();

		testCon.remove(alice, name, nameAlice, context1);

		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isFalse();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false, context1)).isFalse();

	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRemoveStatementCollection(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(alice, name, nameAlice);
		testCon.add(bob, name, nameBob);
		testCon.commit();

		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isTrue();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isTrue();

		try (RepositoryResult<Statement> result = testCon.getStatements(null, null, null, false)) {
			Collection<Statement> c = Iterations.addAll(result, new ArrayList<>());

			testCon.remove(c);

			assertThat(testCon.hasStatement(bob, name, nameBob, false)).isFalse();
			assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isFalse();
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRemoveStatementIteration(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(alice, name, nameAlice);
		testCon.add(bob, name, nameBob);
		testCon.commit();

		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isTrue();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isTrue();

		try (CloseableIteration<? extends Statement> iter = testCon.getStatements(null, null, null,
				false)) {
			testCon.remove(iter);
		}

		assertThat(testCon.hasStatement(bob, name, nameBob, false)).isFalse();
		assertThat(testCon.hasStatement(alice, name, nameAlice, false)).isFalse();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetNamespace(IsolationLevel level) {
		setupTest(level);

		setupNamespaces();
		assertThat(testCon.getNamespace(EXAMPLE)).isEqualTo(EXAMPLE_NS);
		assertThat(testCon.getNamespace(RDFS_PREFIX)).isEqualTo(RDFS_NS);
		assertThat(testCon.getNamespace(RDF_PREFIX)).isEqualTo("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		assertThat(testCon.getNamespace("undefined")).isNull();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetNamespaces(IsolationLevel level) {
		setupTest(level);

		setupNamespaces();
		Map<String, String> map = Namespaces.asMap(Iterations.asSet(testCon.getNamespaces()));
		assertThat(map.size()).isEqualTo(3);
		assertThat(map.keySet()).contains(EXAMPLE, RDFS_PREFIX, RDF_PREFIX);
		assertThat(map.get(EXAMPLE)).isEqualTo(EXAMPLE_NS);
		assertThat(map.get(RDFS_PREFIX)).isEqualTo(RDFS_NS);
		assertThat(map.get(RDF_PREFIX)).isEqualTo("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testImportNamespacesFromIterable(IsolationLevel level) {
		setupTest(level);

		Model nsAwareModel = new LinkedHashModel();
		nsAwareModel.setNamespace(RDFS_PREFIX, RDFS_NS);
		nsAwareModel.setNamespace(EXAMPLE, EXAMPLE_NS);

		testCon.add(nsAwareModel);
		assertThat(testCon.getNamespace(RDFS_PREFIX)).isEqualTo(RDFS_NS);
		assertThat(testCon.getNamespace(EXAMPLE)).isEqualTo(EXAMPLE_NS);

		// Test that existing namespaces are not overwritten
		nsAwareModel.setNamespace(EXAMPLE, "http://something.else/");
		testCon.add(nsAwareModel);
		assertThat(testCon.getNamespace(EXAMPLE)).isEqualTo(EXAMPLE_NS);
	}

	private void setupNamespaces() throws RDFParseException, RepositoryException {
		testCon.setNamespace(EXAMPLE, EXAMPLE_NS);
		testCon.setNamespace(RDF_PREFIX, "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		testCon.setNamespace(RDFS_PREFIX, RDFS_NS);

		// Translated from earlier RDF document. Is this line even necessary?
		testCon.add(vf.createIRI(EXAMPLE_NS, "Main"), vf.createIRI(RDFS_NS, "label"), vf.createLiteral("Main Node"));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testClear(IsolationLevel level) {
		setupTest(level);

		testCon.add(bob, name, nameBob);
		assertThat(testCon.hasStatement(null, name, nameBob, false)).isTrue();
		testCon.clear();
		assertThat(testCon.hasStatement(null, name, nameBob, false)).isFalse();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRecoverFromParseError(IsolationLevel level) throws RepositoryException, IOException {
		setupTest(level);

		String invalidData = "bad";
		String validData = "@prefix foo: <http://example.org/foo#>.\nfoo:a foo:b foo:c.";

		try {
			testCon.add(new StringReader(invalidData), "", RDFFormat.TURTLE);
			fail("Invalid data should result in an exception");
		} catch (RDFParseException e) {
			// Expected behaviour
		}

		try {
			testCon.add(new StringReader(validData), "", RDFFormat.TURTLE);
		} catch (RDFParseException e) {
			fail("Valid data should not result in an exception");
		}

		assertEquals(1, testCon.size(), "Repository contains incorrect number of statements");
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testStatementSerialization(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		Statement st;

		try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, true)) {
			st = statements.next();
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(st);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		Statement deserialized = (Statement) in.readObject();
		in.close();

		assertThat(deserialized).isEqualTo(st);

		assertThat(testCon.hasStatement(st, true)).isTrue();
		assertThat(testCon.hasStatement(deserialized, true)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testBNodeSerialization(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		Statement st;
		try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
			st = statements.next();
		}

		BNode bnode = (BNode) st.getSubject();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(bnode);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		BNode deserializedBNode = (BNode) in.readObject();
		in.close();

		assertThat(deserializedBNode).isEqualTo(bnode);

		assertThat(testCon.hasStatement(bnode, name, nameBob, true)).isTrue();
		assertThat(testCon.hasStatement(deserializedBNode, name, nameBob, true)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testURISerialization(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		Statement st;
		try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
			st = statements.next();
		}

		IRI uri = st.getPredicate();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(uri);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		IRI deserializedURI = (IRI) in.readObject();
		in.close();

		assertThat(deserializedURI).isEqualTo(uri);

		assertThat(testCon.hasStatement(bob, uri, nameBob, true)).isTrue();
		assertThat(testCon.hasStatement(bob, deserializedURI, nameBob, true)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testLiteralSerialization(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);

		Statement st;
		try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, false)) {
			st = statements.next();
		}

		Literal literal = (Literal) st.getObject();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(literal);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		Literal deserialized = (Literal) in.readObject();
		in.close();

		assertThat(deserialized).isEqualTo(literal);

		assertThat(testCon.hasStatement(bob, name, literal, true)).isTrue();
		assertThat(testCon.hasStatement(bob, name, deserialized, true)).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGraphSerialization(IsolationLevel level) throws Exception {
		setupTest(level);

		testCon.add(bob, name, nameBob);
		testCon.add(alice, name, nameAlice);

		try (RepositoryResult<Statement> statements = testCon.getStatements(null, null, null, true)) {
			Model graph = Iterations.addAll(statements, new LinkedHashModel());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(graph);
			out.close();

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream in = new ObjectInputStream(bais);
			Model deserializedGraph = (Model) in.readObject();
			in.close();

			assertThat(deserializedGraph.isEmpty()).isFalse();
			assertThat(deserializedGraph).hasSameSizeAs(graph);
			for (Statement st : deserializedGraph) {
				assertThat(graph).contains(st);
				assertThat(testCon.hasStatement(st, true)).isTrue();
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testEmptyRollback(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		assertThat(testCon.isEmpty()).isTrue();
		assertThat(testCon2.isEmpty()).isTrue();
		testCon.begin();
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.isEmpty()).isFalse();
		assertThat(testCon2.isEmpty()).isTrue();
		testCon.rollback();
		assertThat(testCon.isEmpty()).isTrue();
		assertThat(testCon2.isEmpty()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testEmptyCommit(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		assertThat(testCon.isEmpty()).isTrue();
		assertThat(testCon2.isEmpty()).isTrue();
		testCon.begin();
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.isEmpty()).isFalse();
		assertThat(testCon2.isEmpty()).isTrue();
		testCon.commit();
		assertThat(testCon.isEmpty()).isFalse();
		assertThat(testCon2.isEmpty()).isFalse();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testOpen(IsolationLevel level) {
		setupTest(level);

		assertThat(testCon.isOpen()).isTrue();
		assertThat(testCon2.isOpen()).isTrue();
		testCon.close();
		assertThat(testCon.isOpen()).isFalse();
		assertThat(testCon2.isOpen()).isTrue();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSizeRollback(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		assertThat(testCon.size()).isEqualTo(0L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.begin();
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.size()).isEqualTo(1L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.size()).isEqualTo(2L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.rollback();
		assertThat(testCon.size()).isEqualTo(0L);
		assertThat(testCon2.size()).isEqualTo(0L);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSizeCommit(IsolationLevel level) {
		setupTest(level);

		if (IsolationLevels.NONE.isCompatibleWith(level)) {
			return;
		}
		assertThat(testCon.size()).isEqualTo(0L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.begin();
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.size()).isEqualTo(1L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.add(vf.createBNode(), vf.createIRI(URN_PRED), vf.createBNode());
		assertThat(testCon.size()).isEqualTo(2L);
		assertThat(testCon2.size()).isEqualTo(0L);
		testCon.commit();
		assertThat(testCon.size()).isEqualTo(2L);
		assertThat(testCon2.size()).isEqualTo(2L);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSizeDuplicateStatement(IsolationLevel level) {
		setupTest(level);

		testCon.begin();
		testCon.add(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		testCon.commit();
		testCon.begin();
		testCon.add(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		assertEquals(1, testCon.size(), "Statement should appear once");
		testCon.commit();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddRemove(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		final Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.begin();
		testCon.add(stmt);
		testCon.remove(stmt);
		testCon.commit();

		testCon.exportStatements(null, null, null, false, new AbstractRDFHandler() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				assertThat(st).isNotEqualTo(stmt);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddDelete(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		final Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.begin();
		testCon.add(stmt);
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"DELETE DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.commit();

		testCon.exportStatements(null, null, null, false, new AbstractRDFHandler() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				assertThat(st).isNotEqualTo(stmt);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public final void testInsertRemove(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		final Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.begin();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"INSERT DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.remove(stmt);
		testCon.commit();

		testCon.exportStatements(null, null, null, false, new AbstractRDFHandler() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				assertThat(st).isNotEqualTo(stmt);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testInsertDelete(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		final Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.begin();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"INSERT DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"DELETE DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.commit();

		testCon.exportStatements(null, null, null, false, new AbstractRDFHandler() {

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				assertThat(st).isNotEqualTo(stmt);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddRemoveAdd(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.add(stmt);
		testCon.begin();
		testCon.remove(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1));
		testCon.add(stmt);
		testCon.commit();
		assertFalse(testCon.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddDeleteAdd(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.add(stmt);
		testCon.begin();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"DELETE DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.add(stmt);
		testCon.commit();
		assertFalse(testCon.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddRemoveInsert(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		Statement stmt = vf.createStatement(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1),
				vf.createIRI(URN_TEST_O1));
		testCon.add(stmt);
		testCon.begin();
		testCon.remove(stmt);
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"INSERT DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.commit();
		assertFalse(testCon.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testAddDeleteInsert(IsolationLevel level) throws RDF4JException {
		setupTest(level);

		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1));
		testCon.begin();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"DELETE DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.prepareUpdate(QueryLanguage.SPARQL,
				"INSERT DATA {<" + URN_TEST_S1 + "> <" + URN_TEST_P1 + "> <" + URN_TEST_O1 + ">}").execute();
		testCon.commit();
		assertFalse(testCon.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testQueryInTransaction(IsolationLevel level) {
		setupTest(level);

		testCon.add(bob, RDF.TYPE, FOAF.PERSON);

		testCon.begin();
		String query = "SELECT * where {?x a ?y }";
		try (TupleQueryResult result = testCon.prepareTupleQuery(QueryLanguage.SPARQL, query).evaluate()) {
			// test verifies that query as part of transaction executes and returns
			// a result
			assertNotNull(result);
			assertTrue(result.hasNext());
			testCon.commit();
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateInTransaction(IsolationLevel level) {
		setupTest(level);

		testCon.add(bob, RDF.TYPE, FOAF.PERSON);

		testCon.begin();
		String query = "INSERT { ?x rdfs:label \"Bob\" } where {?x a ?y }";
		testCon.prepareUpdate(QueryLanguage.SPARQL, query).execute();

		// test verifies that update as part of transaction executes and returns
		// a result
		assertTrue(testCon.hasStatement(bob, RDFS.LABEL, vf.createLiteral("Bob"), true));
		testCon.commit();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testInferredStatementCount(IsolationLevel level) {
		setupTest(level);

		assertThat(testCon.isEmpty()).isTrue();
		long inferred = getTotalStatementCount(testCon);

		IRI root = vf.createIRI("urn:root");

		testCon.add(root, RDF.TYPE, RDF.LIST);
		testCon.remove(root, RDF.TYPE, RDF.LIST);

		assertThat(testCon.isEmpty()).isTrue();
		assertThat(getTotalStatementCount(testCon)).isEqualTo(inferred);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testGetContextIDs(IsolationLevel level) {
		setupTest(level);

		assertThat(Iterations.asList(testCon.getContextIDs())).isEmpty();

		// load data
		testCon.add(bob, name, nameBob, context1);
		assertThat(Iterations.asList(testCon.getContextIDs())).isEqualTo(List.of((Resource) context1));

		testCon.remove(bob, name, nameBob, context1);
		assertThat(Iterations.asList(testCon.getContextIDs())).isEmpty();

		testCon.add(bob, name, nameBob, context2);
		assertThat(Iterations.asList(testCon.getContextIDs())).isEqualTo(List.of((Resource) context2));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testXmlCalendarZ(IsolationLevel level) throws Exception {
		setupTest(level);

		String NS = "http://example.org/rdf/";
		int OFFSET = TimeZone.getDefault()
				.getOffset(new GregorianCalendar(2007 - 1900, Calendar.NOVEMBER, 6).getTimeInMillis()) / 1000 / 60;
		String SELECT_BY_DATE = "SELECT ?s ?d WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> ?d . FILTER (?d <= ?date) }";
		DatatypeFactory data = DatatypeFactory.newInstance();
		for (int i = 1; i < 5; i++) {
			IRI uri = vf.createIRI(NS, "date" + i);
			XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2000);
			xcal.setMonth(11);
			xcal.setDay(i * 2);
			testCon.add(uri, RDF.VALUE, vf.createLiteral(xcal));
			IRI uriz = vf.createIRI(NS, "dateZ" + i);
			xcal = data.newXMLGregorianCalendar();
			xcal.setYear(2007);
			xcal.setMonth(11);
			xcal.setDay(i * 2);
			xcal.setTimezone(OFFSET);
			testCon.add(uriz, RDF.VALUE, vf.createLiteral(xcal));
		}
		XMLGregorianCalendar xcal = data.newXMLGregorianCalendar();
		xcal.setYear(2007);
		xcal.setMonth(11);
		xcal.setDay(6);
		xcal.setTimezone(OFFSET);
		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, SELECT_BY_DATE);
		query.setBinding("date", vf.createLiteral(xcal));
		List<BindingSet> list;
		try (TupleQueryResult result = query.evaluate()) {
			list = new ArrayList<>();
			while (result.hasNext()) {
				list.add(result.next());
			}
		}
		assertThat(list).hasSize(7);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testOptionalFilter(IsolationLevel level) {
		setupTest(level);

		String optional = "{ ?s :p1 ?v1 OPTIONAL {?s :p2 ?v2 FILTER(?v1<3) } }";
		IRI s = vf.createIRI("urn:test:s");
		IRI p1 = vf.createIRI(URN_TEST_P1);
		IRI p2 = vf.createIRI(URN_TEST_P2);
		Value v1 = vf.createLiteral(1);
		Value v2 = vf.createLiteral(2);
		Value v3 = vf.createLiteral(3);
		testCon.add(s, p1, v1);
		testCon.add(s, p2, v2);
		testCon.add(s, p1, v3);
		String qry = "PREFIX :<urn:test:> SELECT ?s ?v1 ?v2 WHERE " + optional;
		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
		try (TupleQueryResult result = query.evaluate()) {
			Set<List<Value>> set = new HashSet<>();
			while (result.hasNext()) {
				BindingSet bindings = result.next();
				set.add(Arrays.asList(bindings.getValue("v1"), bindings.getValue("v2")));
			}
			assertThat(set).contains(Arrays.asList(v1, v2));
			assertThat(set).contains(Arrays.asList(v3, null));
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testOrPredicate(IsolationLevel level) {
		setupTest(level);

		String union = "{ :s ?p :o FILTER (?p = :p1 || ?p = :p2) }";
		IRI s = vf.createIRI("urn:test:s");
		IRI p1 = vf.createIRI(URN_TEST_P1);
		IRI p2 = vf.createIRI(URN_TEST_P2);
		IRI o = vf.createIRI("urn:test:o");
		testCon.add(s, p1, o);
		testCon.add(s, p2, o);
		String qry = "PREFIX :<urn:test:> SELECT ?p WHERE " + union;
		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, qry);
		try (TupleQueryResult result = query.evaluate()) {
			List<Value> list = new ArrayList<>();
			while (result.hasNext()) {
				BindingSet bindings = result.next();
				list.add(bindings.getValue("p"));
			}
			assertThat(list).contains(p1);
			assertThat(list).contains(p2);
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSES713(IsolationLevel level) {
		setupTest(level);

		String queryString = "SELECT * { ?sub ?pred ?obj . FILTER ( 'not a number' + 1 = ?obj )}";

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		try (TupleQueryResult tqr = query.evaluate()) {
			assertFalse(tqr.hasNext(), "Query should not return any results");
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testSES2172ChineseChars(IsolationLevel level) {
		setupTest(level);

		String updateString = "INSERT DATA { <urn:subject1> rdfs:label \"\\u8BBE\\u5907\". }";

		Update update = testCon.prepareUpdate(QueryLanguage.SPARQL, updateString);
		update.execute();

		assertFalse(testCon.isEmpty());

		String queryString = "SELECT ?o WHERE { <urn:subject1> rdfs:label ?o . }";

		TupleQuery query = testCon.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
		try (TupleQueryResult result = query.evaluate()) {
			assertNotNull(result);

			final String expected = "设备";
			while (result.hasNext()) {
				Value o = result.next().getValue("o");

				assertEquals(expected, o.stringValue());
			}
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testQueryDefaultGraph(IsolationLevel level) {
		setupTest(level);

		IRI graph = vf.createIRI("urn:test:default");
		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1));
		assertThat(size(graph)).isEqualTo(0);
		testCon.add(vf.createIRI("urn:test:s2"), vf.createIRI(URN_TEST_P2), vf.createIRI("urn:test:o2"), graph);
		assertThat(size(graph)).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testQueryBaseURI(IsolationLevel level) {
		setupTest(level);

		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1));
		try (TupleQueryResult rs = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { <> ?p ?o }", URN_TEST_S1)
				.evaluate()) {
			assertThat(rs.hasNext()).isTrue();
		}
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testUpdateBaseURI(IsolationLevel level) {
		setupTest(level);

		testCon.prepareUpdate(QueryLanguage.SPARQL, "INSERT DATA { <> a <> }", URN_TEST_S1).execute();
		assertThat(testCon.size()).isEqualTo(1L);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testDeleteDefaultGraph(IsolationLevel level) {
		setupTest(level);

		IRI g1 = vf.createIRI("urn:test:g1");
		IRI g2 = vf.createIRI("urn:test:g2");
		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1), g1);
		testCon.add(vf.createIRI("urn:test:s2"), vf.createIRI(URN_TEST_P2), vf.createIRI("urn:test:o2"), g2);
		Update up = testCon.prepareUpdate(QueryLanguage.SPARQL, SPARQL_DEL_ALL);
		SimpleDataset ds = new SimpleDataset();
		ds.addDefaultGraph(g1);
		ds.addDefaultRemoveGraph(g1);
		up.setDataset(ds);
		up.execute();
		assertThat(size(g1)).isEqualTo(0);
		assertThat(size(g2)).isEqualTo(1);
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testRemoveStatementsFromContextSingleTransaction(IsolationLevel level) throws Exception {
		setupTest(level);

		IRI g1 = vf.createIRI("urn:test:g1");
		IRI g2 = vf.createIRI("urn:test:g2");
		testCon.begin();
		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1), g1);
		testCon.add(vf.createIRI("urn:test:s2"), vf.createIRI(URN_TEST_P2), vf.createIRI("urn:test:o2"), g2);
		testCon.commit();

		testCon.begin();
		testCon.remove(((Resource) null), null, null, g1);
		try (Stream<Statement> stream = testCon.getStatements(null, null, null, false).stream()) {
			List<Statement> collect = stream.collect(Collectors.toList());
			assertEquals(1, collect.size());
		}
		testCon.commit();
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testClearStatementsFromContextSingleTransaction(IsolationLevel level) throws Exception {
		setupTest(level);

		IRI g1 = vf.createIRI("urn:test:g1");
		IRI g2 = vf.createIRI("urn:test:g2");
		testCon.begin();
		testCon.add(vf.createIRI(URN_TEST_S1), vf.createIRI(URN_TEST_P1), vf.createIRI(URN_TEST_O1), g1);
		testCon.add(vf.createIRI("urn:test:s2"), vf.createIRI(URN_TEST_P2), vf.createIRI("urn:test:o2"), g2);
		testCon.commit();

		testCon.begin();
		testCon.clear(g1);

		try (Stream<Statement> stream = testCon.getStatements(null, null, null, false).stream()) {
			List<Statement> collect = stream.collect(Collectors.toList());
			assertEquals(1, collect.size());
		}
		testCon.commit();
	}

	private long size(IRI defaultGraph) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		TupleQuery qry = testCon.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT * { ?s ?p ?o }");
		SimpleDataset dataset = new SimpleDataset();
		dataset.addDefaultGraph(defaultGraph);
		qry.setDataset(dataset);

		long size;
		try (TupleQueryResult result = qry.evaluate()) {
			size = result.stream().count();
		}

		try (Stream<Statement> stream = testCon.getStatements(null, null, null, defaultGraph).stream()) {
			assertEquals(size, stream.count());
		}

		return size;

	}

	private long getTotalStatementCount(RepositoryConnection connection) throws RepositoryException {
		try (CloseableIteration<? extends Statement> iter = connection.getStatements(null, null,
				null, true)) {
			return iter.stream().count();
		}
	}

}
