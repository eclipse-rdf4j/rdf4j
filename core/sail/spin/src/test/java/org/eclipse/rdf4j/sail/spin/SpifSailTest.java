/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.spin;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.spin.functions.SpinFunctionTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests are provided by {@link SpinFunctionTest} </br>
 * Runs the spif test cases.
 */
@Deprecated
public class SpifSailTest {

	private Repository repo;

	private RepositoryConnection conn;

	@Before
	public void setup()
		throws RepositoryException
	{
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		ForwardChainingRDFSInferencer rdfsInferencer = new ForwardChainingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		repo = new SailRepository(spinSail);
		repo.initialize();
		conn = repo.getConnection();
	}

	@After
	public void tearDown()
		throws RepositoryException
	{
		if (conn != null) {
			conn.close();
		}
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	@Ignore
	public void runTests()
		throws Exception
	{
		loadRDF("/schema/owl.ttl");
		loadRDF("/schema/spif.ttl");
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"prefix spin: <http://spinrdf.org/spin#> " + "prefix spl: <http://spinrdf.org/spl#> "
						+ "select ?testCase ?expected ?actual where {?testCase a spl:TestCase. ?testCase spl:testResult ?expected. ?testCase spl:testExpression ?expr. "
						+ "BIND(spin:eval(?expr) as ?actual) " + "FILTER(?expected != ?actual) "
						+ "FILTER(strstarts(str(?testCase), 'http://spinrdf.org/spif#'))}");
		// returns failed tests
		TupleQueryResult tqr = tq.evaluate();
		try {
			while (tqr.hasNext()) {
				BindingSet bs = tqr.next();
				Value testCase = bs.getValue("testCase");
				Value expected = bs.getValue("expected");
				Value actual = bs.getValue("actual");
				assertEquals(testCase.stringValue(), expected, actual);
			}
		}
		finally {
			tqr.close();
		}
	}

	private void loadRDF(String path)
		throws IOException, OpenRDFException
	{
		URL url = getClass().getResource(path);
		InputStream in = url.openStream();
		try {
			conn.add(in, url.toString(), RDFFormat.TURTLE);
		}
		finally {
			in.close();
		}
	}

	@Test
	@Ignore
	public void testCast()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:cast(3.14, xsd:integer) = 3)}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testIndexOf()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:indexOf('test', 't', 2) = 3)}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testLastIndexOf()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:lastIndexOf('test', 't') = 3)}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testEncodeURL()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:encodeURL('Hello world') = 'Hello+world')}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testBuildString()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:buildString('{?1} {?2}', 'Hello', 'world') = 'Hello world')}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testBuildURI()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:buildURI('<http://example.org/{?1}#{?2}>', 'schema', 'prop') = <http://example.org/schema#prop>)}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testName()
		throws Exception
	{
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {?s rdfs:label ?l. filter(spif:name(?s) = ?l)}");
		assertTrue(bq.evaluate());
	}

	@Test
	@Ignore
	public void testForEach()
		throws Exception
	{
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> " + "select ?x where {?x spif:foreach (1 2 3)}");
		TupleQueryResult tqr = tq.evaluate();
		for (int i = 1; i <= 3; i++) {
			BindingSet bs = tqr.next();
			assertThat(((Literal)bs.getValue("x")).intValue(), is(i));
		}
		assertFalse(tqr.hasNext());
	}

	@Test
	@Ignore
	public void testFor()
		throws Exception
	{
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> " + "select ?x where {?x spif:for (1 4)}");
		TupleQueryResult tqr = tq.evaluate();
		for (int i = 1; i <= 4; i++) {
			BindingSet bs = tqr.next();
			assertThat(((Literal)bs.getValue("x")).intValue(), is(i));
		}
		assertFalse(tqr.hasNext());
	}

	@Test
	@Ignore
	public void testSplit()
		throws Exception
	{
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> " + "select ?x where {?x spif:split ('1,2,3' ',')}");
		TupleQueryResult tqr = tq.evaluate();
		for (int i = 1; i <= 3; i++) {
			BindingSet bs = tqr.next();
			assertThat(((Literal)bs.getValue("x")).stringValue(), is(Integer.toString(i)));
		}
		assertFalse(tqr.hasNext());
	}

	@Test
	@Ignore
	public void testCanInvoke()
		throws Exception
	{
		loadRDF("/schema/spif.ttl");
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:canInvoke(spif:indexOf, 'foobar', 'b'))}");
		assertTrue(bq.evaluate());
	}

	/**
	 * In {@link SpinFunctionTest} the SPARQL request from this test was changed into tuple type. It can be
	 * found in file <code>src/test/resources/functions/spif/canInvoke2.rq</code>.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testCantInvoke()
		throws Exception
	{
		loadRDF("/schema/spif.ttl");
		BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
				"prefix spif: <http://spinrdf.org/spif#> "
						+ "ask where {filter(spif:canInvoke(spif:indexOf, 'foobar', 2))}");
		assertFalse(bq.evaluate());
	}
}
