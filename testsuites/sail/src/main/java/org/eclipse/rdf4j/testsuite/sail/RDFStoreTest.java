/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sail;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * A JUnit test for testing Sail implementations that store RDF data. This is purely a test for data storage and
 * retrieval which assumes that no inferencing or whatsoever is performed. This is an abstract class that should be
 * extended for specific Sail implementations.
 */
public abstract class RDFStoreTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	/**
	 * Timeout all individual tests after 1 minute.
	 */
	@Rule
	public Timeout to = new Timeout(60, TimeUnit.SECONDS);

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String EXAMPLE_NS = "http://example.org/";

	private static final String PAINTER = "Painter";

	private static final String PAINTS = "paints";

	private static final String PAINTING = "Painting";

	private static final String PICASSO = "picasso";

	private static final String REMBRANDT = "rembrandt";

	private static final String GUERNICA = "guernica";

	private static final String NIGHTWATCH = "nightwatch";

	private static final String CONTEXT_1 = "context1";

	private static final String CONTEXT_2 = "context2";

	/*-----------*
	 * Variables *
	 *-----------*/

	protected IRI painter;

	protected IRI paints;

	protected IRI painting;

	protected IRI picasso;

	protected IRI rembrandt;

	protected IRI guernica;

	protected IRI nightwatch;

	protected IRI context1;

	protected IRI context2;

	protected Sail sail;

	protected SailConnection con;

	protected ValueFactory vf;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public RDFStoreTest() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets an instance of the Sail that should be tested.
	 *
	 * @return a Sail.
	 */
	protected abstract Sail createSail();

	@Before
	public void setUp() throws Exception {
		sail = createSail();

		con = sail.getConnection();

		// Create values
		vf = sail.getValueFactory();

		painter = vf.createIRI(EXAMPLE_NS, PAINTER);
		paints = vf.createIRI(EXAMPLE_NS, PAINTS);
		painting = vf.createIRI(EXAMPLE_NS, PAINTING);
		picasso = vf.createIRI(EXAMPLE_NS, PICASSO);
		guernica = vf.createIRI(EXAMPLE_NS, GUERNICA);
		rembrandt = vf.createIRI(EXAMPLE_NS, REMBRANDT);
		nightwatch = vf.createIRI(EXAMPLE_NS, NIGHTWATCH);

		context1 = vf.createIRI(EXAMPLE_NS, CONTEXT_1);
		context2 = vf.createIRI(EXAMPLE_NS, CONTEXT_2);

	}

	@After
	public void tearDown() throws Exception {
		try {
			if (con.isOpen()) {
				try {
					if (con.isActive()) {
						con.rollback();
					}
				} finally {
					con.close();
				}
			}
		} finally {
			sail.shutDown();
			sail = null;
		}
	}

	@Test
	public void testEmptyRepository() throws Exception {
		// repository should be empty
		Assert.assertEquals("Empty repository should not return any statements", 0, countAllElements());

		Assert.assertEquals("Named context should be empty", 0, countContext1Elements());

		Assert.assertEquals("Empty repository should not return any context identifiers", 0,
				countElements(con.getContextIDs()));

		Assert.assertEquals("Empty repository should not return any query results", 0,
				countQueryResults("select * where { ?s ?p ?o}"));
	}

	@Test
	public void testValueRoundTrip1() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		IRI obj = vf.createIRI(EXAMPLE_NS + GUERNICA);

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testValueRoundTrip2() throws Exception {
		BNode subj = vf.createBNode();
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		IRI obj = vf.createIRI(EXAMPLE_NS + GUERNICA);

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testValueRoundTrip3() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("guernica");

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testValueRoundTrip4() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("guernica", "es");

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testValueRoundTrip5() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral(3);

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testDecimalRoundTrip() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("3", XSD.DECIMAL);

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testTimeZoneRoundTrip() throws Exception {
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("2006-08-23+00:00", CoreDatatype.XSD.DATE);
		testValueRoundTrip(subj, pred, obj);

		con.begin();
		con.removeStatements(null, null, null);
		con.commit();

		obj = vf.createLiteral("2006-08-23", CoreDatatype.XSD.DATE);
		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testLongURIRoundTrip() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 512; i++) {
			sb.append(Character.toChars('A' + (i % 26)));
		}
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		IRI obj = vf.createIRI(EXAMPLE_NS + GUERNICA + sb.toString());

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testLongLiteralRoundTrip() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 512; i++) {
			sb.append(Character.toChars('A' + (i % 26)));
		}
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("guernica" + sb.toString());

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testReallyLongLiteralRoundTrip() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1024000; i++) {
			sb.append(Character.toChars('A' + (i % 26)));
		}
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("guernica" + sb.toString());

		testValueRoundTrip(subj, pred, obj);
	}

	@Test
	public void testLongLangRoundTrip() throws Exception {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 512; i++) {
			sb.append(Character.toChars('A' + (i % 26)));
		}
		IRI subj = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI pred = vf.createIRI(EXAMPLE_NS + PAINTS);
		Literal obj = vf.createLiteral("guernica" + sb.toString(), "es");

		testValueRoundTrip(subj, pred, obj);
	}

	private void testValueRoundTrip(Resource subj, IRI pred, Value obj) {
		con.begin();
		con.addStatement(subj, pred, obj);
		con.commit();

		try (CloseableIteration<? extends Statement, SailException> stIter = con.getStatements(null, null, null,
				false)) {
			Assert.assertTrue(stIter.hasNext());

			Statement st = stIter.next();
			Assert.assertEquals(subj, st.getSubject());
			Assert.assertEquals(pred, st.getPredicate());
			Assert.assertEquals(obj, st.getObject());
			Assert.assertFalse(stIter.hasNext());
		}

		ParsedTupleQuery tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"SELECT * WHERE { ?S ?P ?O. FILTER(?P = <" + pred.stringValue() + ">)}", null);

		CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;
		iter = con.evaluate(tupleQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false);

		try {
			Assert.assertTrue(iter.hasNext());

			BindingSet bindings = iter.next();
			Assert.assertEquals(subj, bindings.getValue("S"));
			Assert.assertEquals(pred, bindings.getValue("P"));
			Assert.assertEquals(obj, bindings.getValue("O"));
			Assert.assertTrue(!iter.hasNext());
		} finally {
			iter.close();
		}
	}

	@Test
	public void testCreateURI1() {
		IRI picasso1 = vf.createIRI(EXAMPLE_NS, PICASSO);
		IRI picasso2 = vf.createIRI(EXAMPLE_NS + PICASSO);
		con.begin();
		con.addStatement(picasso1, paints, guernica);
		con.addStatement(picasso2, paints, guernica);
		con.commit();

		Assert.assertEquals("createURI(Sring) and createURI(String, String) should create equal URIs", 1, con.size());
	}

	@Test
	public void testCreateURI2() {
		IRI picasso1 = vf.createIRI(EXAMPLE_NS + PICASSO);
		IRI picasso2 = vf.createIRI(EXAMPLE_NS, PICASSO);
		con.begin();
		con.addStatement(picasso1, paints, guernica);
		con.addStatement(picasso2, paints, guernica);
		con.commit();

		Assert.assertEquals("createURI(Sring) and createURI(String, String) should create equal URIs", 1, con.size());
	}

	@Test
	public void testInvalidDateTime() {
		// SES-711
		Literal date1 = vf.createLiteral("2004-12-20", XSD.DATETIME);
		Literal date2 = vf.createLiteral("2004-12-20", CoreDatatype.XSD.DATETIME);
		Assert.assertEquals(date1, date2);
	}

	@Test
	public void testSize() {
		Assert.assertEquals("Size of empty repository should be 0", 0, con.size());

		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();

		Assert.assertEquals("Size of repository should be 5", 5, con.size());
		Assert.assertEquals("Size of named context should be 3", 3, con.size(context1));

		IRI unknownContext = vf.createIRI(EXAMPLE_NS + "unknown");

		Assert.assertEquals("Size of unknown context should be 0", 0, con.size(unknownContext));

		IRI uriImplContext1 = vf.createIRI(context1.toString());

		Assert.assertEquals("Size of named context (defined as URIImpl) should be 3", 3, con.size(uriImplContext1));
	}

	@Test
	public void testAddData() throws Exception {
		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();

		Assert.assertEquals("Repository should contain 5 statements in total", 5, countAllElements());

		Assert.assertEquals("Named context should contain 3 statements", 3, countContext1Elements());

		Assert.assertEquals("Repository should have 1 context identifier", 1, countElements(con.getContextIDs()));

		Assert.assertEquals("Repository should contain 5 statements in total", 5,
				countQueryResults("select * where {?s ?p ?o}"));

		// Check for presence of the added statements

		assertThat(con.hasStatement(painter, RDF.TYPE, RDFS.CLASS, true)).isTrue();
		assertThat(con.hasStatement(painting, RDF.TYPE, RDFS.CLASS, true)).isTrue();
		assertThat(con.hasStatement(picasso, RDF.TYPE, painter, true)).isTrue();
		assertThat(con.hasStatement(guernica, RDF.TYPE, painting, true)).isTrue();
		assertThat(con.hasStatement(picasso, paints, guernica, true)).isTrue();

		// Check for absence of non-added statements
		assertThat(con.hasStatement(painter, paints, painting, true)).isFalse();

		// Various other checks
		Assert.assertEquals("Repository should contain 2 statements matching (picasso, _, _)", 2,
				countQueryResults("select * where { ex:picasso ?P ?O}"));

		Assert.assertEquals("Repository should contain 1 statement matching (picasso, paints, _)", 1,
				countQueryResults("select * where {ex:picasso ex:paints ?O}"));

		Assert.assertEquals("Repository should contain 4 statements matching (_, type, _)", 4,
				countQueryResults("select * where {?S rdf:type ?O}"));

		Assert.assertEquals("Repository should contain 2 statements matching (_, _, Class)", 2,
				countQueryResults("select * where { ?S ?P rdfs:Class }"));

		Assert.assertEquals("Repository should contain 0 statements matching (_, _, type)", 0,
				countQueryResults("select * where {?S ?P rdf:type}"));
	}

	@Test
	public void testAddWhileQuerying() {
		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter);
		con.addStatement(guernica, RDF.TYPE, painting);
		con.addStatement(picasso, paints, guernica);
		con.commit();

		ParsedTupleQuery tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"SELECT ?C WHERE { [] a ?C }", null);

		CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;
		iter = con.evaluate(tupleQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false);

		con.begin();

		while (iter.hasNext()) {
			BindingSet bindings = iter.next();
			Value c = bindings.getValue("C");
			if (c instanceof Resource) {
				con.addStatement((Resource) c, RDF.TYPE, RDFS.CLASS);
			}
		}

		con.commit();

		Assert.assertEquals(3, countElements(con.getStatements(null, RDF.TYPE, RDFS.CLASS, false)));

		// simulate auto-commit
		tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, "SELECT ?P WHERE { [] ?P [] .}", null);
		iter = con.evaluate(tupleQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false);

		while (iter.hasNext()) {
			BindingSet bindings = iter.next();
			Value p = bindings.getValue("P");
			if (p instanceof IRI) {
				con.begin();
				con.addStatement((IRI) p, RDF.TYPE, RDF.PROPERTY);
				con.commit();
			}
		}

		Assert.assertEquals(2, countElements(con.getStatements(null, RDF.TYPE, RDF.PROPERTY, false)));
	}

	@Test
	public void testRemoveAndClear() throws Exception {
		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();

		// Test removal of statements
		con.begin();
		con.removeStatements(painting, RDF.TYPE, RDFS.CLASS);
		con.commit();

		Assert.assertEquals("Repository should contain 4 statements in total", 4, countAllElements());

		Assert.assertEquals("Named context should contain 3 statements", 3, countContext1Elements());

		assertThat(con.hasStatement(painting, RDF.TYPE, RDFS.CLASS, true)).isFalse();

		con.begin();
		con.removeStatements(null, null, null, context1);
		con.commit();

		Assert.assertEquals("Repository should contain 1 statement in total", 1, countAllElements());

		Assert.assertEquals("Named context should be empty", 0, countContext1Elements());

		con.begin();
		con.clear();
		con.commit();

		Assert.assertEquals("Repository should no longer contain any statements", 0, countAllElements());
	}

	@Test
	public void testClose() {
		try {
			con.close();
			con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
			Assert.fail("Operation on connection after close should result in IllegalStateException");
		} catch (IllegalStateException e) {
			// do nothing, this is expected
		} catch (SailException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testContexts() throws Exception {
		con.begin();
		// Add schema data to the repository, no context
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);

		// Add stuff about picasso to context1
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);

		// Add stuff about rembrandt to context2
		con.addStatement(rembrandt, RDF.TYPE, painter, context2);
		con.addStatement(nightwatch, RDF.TYPE, painting, context2);
		con.addStatement(rembrandt, paints, nightwatch, context2);

		con.commit();

		Assert.assertEquals("context1 should contain 3 statements", 3, countContext1Elements());
		Assert.assertEquals("context2 should contain 3 statements", 3,
				countElements(con.getStatements(null, null, null, false, context2)));
		Assert.assertEquals("Repository should contain 8 statements", 8, countAllElements());
		Assert.assertEquals("statements without context should equal 2", 2,
				countElements(con.getStatements(null, null, null, false, (Resource) null)));

		Assert.assertEquals("Statements without context and statements in context 1 together should total 5", 5,
				countElements(con.getStatements(null, null, null, false, null, context1)));

		Assert.assertEquals("Statements without context and statements in context 2 together should total 5", 5,
				countElements(con.getStatements(null, null, null, false, null, context2)));

		Assert.assertEquals("Statements in context 1 and in context 2 together should total 6", 6,
				countElements(con.getStatements(null, null, null, false, context1, context2)));

		// remove two statements from context1.
		con.begin();
		con.removeStatements(picasso, null, null, context1);
		con.commit();

		Assert.assertEquals("context1 should contain 1 statements", 1, countContext1Elements());

		Assert.assertEquals("Repository should contain 6 statements", 6, countAllElements());

		Assert.assertEquals("Statements without context and statements in context 1 together should total 3", 3,
				countElements(con.getStatements(null, null, null, false, null, context1)));

		Assert.assertEquals("Statements without context and statements in context 2 together should total 5", 5,
				countElements(con.getStatements(null, null, null, false, context2, null)));

		Assert.assertEquals("Statements in context 1 and in context 2 together should total 4", 4,
				countElements(con.getStatements(null, null, null, false, context1, context2)));
	}

	@Test
	public void testQueryBindings() {
		// Add some data to the repository
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();

		// Query 1
		ParsedTupleQuery tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select ?X where { ?X a ?Y . ?Y a rdfs:Class. }", null);
		TupleExpr tupleExpr = tupleQuery.getTupleExpr();

		MapBindingSet bindings = new MapBindingSet(2);
		CloseableIteration<? extends BindingSet, QueryEvaluationException> iter;

		iter = con.evaluate(tupleExpr, null, bindings, false);
		int resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 2, resultCount);

		bindings.addBinding("Y", painter);
		iter = con.evaluate(tupleExpr, null, bindings, false);
		resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 1, resultCount);

		bindings.addBinding("Z", painting);
		iter = con.evaluate(tupleExpr, null, bindings, false);
		resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 1, resultCount);

		bindings.removeBinding("Y");
		iter = con.evaluate(tupleExpr, null, bindings, false);
		resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 2, resultCount);

		// Query 2
		tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"select ?X WHERE { ?X a ?Y . ?Y a rdfs:Class. filter(?Y = ?Z) }", null);
		tupleExpr = tupleQuery.getTupleExpr();
		bindings.clear();

		iter = con.evaluate(tupleExpr, null, bindings, false);
		resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 0, resultCount);

		bindings.addBinding("Z", painter);
		iter = con.evaluate(tupleExpr, null, bindings, false);
		resultCount = verifyQueryResult(iter, 1);
		Assert.assertEquals("Wrong number of query results", 1, resultCount);
	}

	@Test
	public void testStatementEquals() {
		Statement st = vf.createStatement(picasso, RDF.TYPE, painter);
		Assert.assertEquals(st, vf.createStatement(picasso, RDF.TYPE, painter));
		Assert.assertNotEquals(st, vf.createStatement(picasso, RDF.TYPE, painter, context1));
		Assert.assertNotEquals(st, vf.createStatement(picasso, RDF.TYPE, painter, context2));
	}

	@Test
	public void testStatementSerialization() throws Exception {
		Statement st = vf.createStatement(picasso, RDF.TYPE, painter);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(baos);
		out.writeObject(st);
		out.close();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream in = new ObjectInputStream(bais);
		Statement deserializedStatement = (Statement) in.readObject();
		in.close();

		Assert.assertTrue(st.equals(deserializedStatement));
	}

	@Test
	public void testGetNamespaces() {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.commit();

		try (CloseableIteration<? extends Namespace, SailException> namespaces = con.getNamespaces()) {
			Assert.assertTrue(namespaces.hasNext());
			Namespace rdf = namespaces.next();
			Assert.assertEquals("rdf", rdf.getPrefix());
			Assert.assertEquals(RDF.NAMESPACE, rdf.getName());
			Assert.assertFalse(namespaces.hasNext());
		}
	}

	@Test
	public void testGetNamespace() {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.commit();
		Assert.assertEquals(RDF.NAMESPACE, con.getNamespace("rdf"));
	}

	@Test
	public void testClearNamespaces() {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.setNamespace("rdfs", RDFS.NAMESPACE);
		con.clearNamespaces();
		con.commit();
		Assert.assertTrue(!con.getNamespaces().hasNext());
	}

	@Test
	public void testRemoveNamespaces() throws Exception {
		con.begin();
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.removeNamespace("rdf");
		con.commit();
		Assert.assertNull(con.getNamespace("rdf"));
	}

	@Test
	public void testNullNamespaceDisallowed() {
		try {
			con.setNamespace("foo", null);
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException e) {
			// expected
		}
	}

	@Test
	public void testNullPrefixDisallowed() {
		try {
			con.setNamespace(null, "foo");
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException e) {
			// expected
		}
		try {
			con.getNamespace(null);
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException e) {
			// expected
		}
		try {
			con.removeNamespace(null);
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException e) {
			// expected
		}
	}

	@Test
	public void testGetContextIDs() {
		Assert.assertEquals(0, countElements(con.getContextIDs()));

		// load data
		con.begin();
		con.addStatement(picasso, paints, guernica, context1);
		Assert.assertEquals(1, countElements(con.getContextIDs()));
		Assert.assertEquals(context1, first(con.getContextIDs()));

		con.removeStatements(picasso, paints, guernica, context1);
		Assert.assertEquals(0, countElements(con.getContextIDs()));
		con.commit();

		Assert.assertEquals(0, countElements(con.getContextIDs()));

		con.begin();
		con.addStatement(picasso, paints, guernica, context2);
		Assert.assertEquals(1, countElements(con.getContextIDs()));
		Assert.assertEquals(context2, first(con.getContextIDs()));
		con.commit();
	}

	@Test
	public void testOldURI() throws Exception {
		Assert.assertEquals(0, countAllElements());
		con.begin();
		con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
		con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
		con.addStatement(picasso, RDF.TYPE, painter, context1);
		con.addStatement(guernica, RDF.TYPE, painting, context1);
		con.addStatement(picasso, paints, guernica, context1);
		Assert.assertEquals(5, countAllElements());
		con.commit();

		con.begin();
		con.clear();
		con.commit();

		con.begin();
		con.addStatement(picasso, paints, guernica, context1);
		con.commit();
		Assert.assertEquals(1, countAllElements());
	}

	@Test
	public void testDualConnections() throws Exception {
		try (SailConnection con2 = sail.getConnection()) {
			Assert.assertEquals(0, countAllElements());
			con.begin();
			con.addStatement(painter, RDF.TYPE, RDFS.CLASS);
			con.addStatement(painting, RDF.TYPE, RDFS.CLASS);
			con.addStatement(picasso, RDF.TYPE, painter, context1);
			con.addStatement(guernica, RDF.TYPE, painting, context1);
			con.commit();
			Assert.assertEquals(4, countAllElements());
			con2.begin();
			con2.addStatement(RDF.NIL, RDF.TYPE, RDF.LIST);
			String query = "SELECT * WHERE { ?S ?P ?O }";
			ParsedTupleQuery tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
			Assert.assertEquals(5, countElements(
					con2.evaluate(tupleQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false)));
			Runnable clearer = () -> {
				try {
					con.begin();
					con.clear();
					con.commit();
				} catch (SailException e) {
					throw new RuntimeException(e);
				}
			};
			Thread thread = new Thread(clearer);
			thread.start();
			Thread.yield();
			Thread.yield();
			con2.commit();
			thread.join();
		}
	}

	@Test
	public void testBNodeReuse() {
		con.begin();
		con.addStatement(RDF.VALUE, RDF.VALUE, RDF.VALUE);
		Assert.assertEquals(1, con.size());
		BNode b1 = vf.createBNode();
		con.addStatement(b1, RDF.VALUE, b1);
		con.removeStatements(b1, RDF.VALUE, b1);
		Assert.assertEquals(1, con.size());
		BNode b2 = vf.createBNode();
		con.addStatement(b2, RDF.VALUE, b2);
		con.addStatement(b1, RDF.VALUE, b1);
		Assert.assertEquals(3, con.size());
		con.commit();
	}

	@Test
	public void testDuplicateCount() {
		con.begin();

		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		con.begin();
		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Assert.assertEquals("Statement should appear once", 1, con.size());
		con.commit();

	}

	@Test
	public void testDuplicateGetStatement() {
		con.begin();

		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		con.begin();
		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		try (Stream<? extends Statement> stream = con.getStatements(null, null, null, false).stream()) {
			long count = stream.count();
			Assert.assertEquals("Statement should appear once", 1, count);
		}
		con.commit();
	}

	@Test
	public void testDuplicateGetStatementAfterCommit() {
		con.begin();

		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		con.begin();
		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		try (Stream<? extends Statement> stream = con.getStatements(null, null, null, false).stream()) {
			long count = stream.count();
			Assert.assertEquals("Statement should appear once", 1, count);

		}
	}

	@Test
	public void testDuplicateCountAfterComit() {
		con.begin();

		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		con.begin();
		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();

		Assert.assertEquals("Statement should appear once", 1, con.size());
	}

	@Test
	public void testDuplicateCountMultipleTimes() {
		con.begin();

		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		con.commit();
		con.begin();
		con.addStatement(RDF.SUBJECT, RDF.PREDICATE, RDF.OBJECT);
		Assert.assertEquals("Statement should appear once", 1, con.size());
		Assert.assertEquals("Statement should appear once", 1, con.size());
		Assert.assertEquals("Statement should appear once", 1, con.size());
		con.commit();
	}

	private <T, X extends Exception> T first(CloseableIteration<T, X> iter) throws X {
		try (iter) {
			if (iter.hasNext()) {
				return iter.next();
			}
		}

		return null;
	}

	protected int countContext1Elements() {
		return countElements(con.getStatements(null, null, null, false, context1));
	}

	protected int countAllElements() {
		return countElements(con.getStatements(null, null, null, false));
	}

	private <T, X extends Exception> int countElements(CloseableIteration<T, X> iter) throws X {
		int count = 0;

		try (iter) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}

		return count;
	}

	protected int countQueryResults(String query) {
		ParsedTupleQuery tupleQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"PREFIX ex: <" + EXAMPLE_NS + "> \n" + query, null);

		return countElements(con.evaluate(tupleQuery.getTupleExpr(), null, EmptyBindingSet.getInstance(), false));
	}

	private int verifyQueryResult(CloseableIteration<? extends BindingSet, QueryEvaluationException> resultIter,
			int expectedBindingCount) throws QueryEvaluationException {
		int resultCount = 0;

		while (resultIter.hasNext()) {
			BindingSet resultBindings = resultIter.next();
			resultCount++;

			Assert.assertEquals("Wrong number of binding names for binding set", expectedBindingCount,
					resultBindings.getBindingNames().size());

			int bindingCount = 0;
			Iterator<Binding> bindingIter = resultBindings.iterator();
			while (bindingIter.hasNext()) {
				bindingIter.next();
				bindingCount++;
			}

			Assert.assertEquals("Wrong number of bindings in binding set", expectedBindingCount, bindingCount);
		}

		return resultCount;
	}
}
