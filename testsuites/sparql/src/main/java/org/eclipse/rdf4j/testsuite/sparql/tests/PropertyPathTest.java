/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.Test;

/**
 * Tests on SPARQL property paths.
 * 
 * @author Jeen Broekstra
 *
 * @see ArbitraryLengthPathTest
 */
public class PropertyPathTest extends AbstractComplianceTest {

	@Test
	public void testSES2147PropertyPathsWithIdenticalSubsPreds() throws Exception {

		String data = "<urn:s1> <urn:p> <urn:s2> .\n" +
				"<urn:s2> <urn:p> <urn:s3> .\n" +
				"<urn:s3> <urn:q> <urn:s4> .\n" +
				"<urn:s1> <urn:p> <urn:s5> .\n" +
				"<urn:s5> <urn:q> <urn:s6> .\n";

		conn.begin();
		conn.add(new StringReader(data), "", RDFFormat.NTRIPLES);
		conn.commit();

		String query = getNamespaceDeclarations() +
				"SELECT ?x \n" +
				"WHERE { ?x <urn:p>*/<urn:q> <urn:s4> . \n" +
				"        ?x <urn:p>*/<urn:q> <urn:s6> . \n" +
				"} \n";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {

			assertNotNull(result);
			assertTrue(result.hasNext());

			Value x = result.next().getValue("x");
			assertNotNull(x);
			assertTrue(x instanceof IRI);
			assertEquals("urn:s1", x.stringValue());
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSES2024PropertyPathAnonVarSharing() throws Exception {
		loadTestData("/testdata-query/dataset-ses2024.trig");
		String query = "PREFIX : <http://example.org/> SELECT * WHERE { ?x1 :p/:lit ?l1 . ?x1 :diff ?x2 . ?x2 :p/:lit ?l2 . }";

		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult result = tq.evaluate()) {
			assertNotNull(result);

			BindingSet bs = result.next();
			Literal l1 = (Literal) bs.getValue("l1");
			Literal l2 = (Literal) bs.getValue("l2");

			assertNotNull(l1);
			assertNotEquals(l1, l2);
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPropertyPathNegationInversion() throws Exception {
		String data = "@prefix : <http://example.org/>.\n"
				+ ":Mary :parentOf :Jim.\n"
				+ ":Jim :knows :Jane.\n"
				+ ":Jane :worksFor :IBM.";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);
		String query1 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) :Jane } ";

		assertTrue(conn.prepareBooleanQuery(query1).evaluate());

		String query2 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) ?a } ";
		assertTrue(conn.prepareBooleanQuery(query2).evaluate());

		String query3 = "prefix : <http://example.org/> ASK WHERE { :IBM (^(:|!:))* :Mary } ";
		assertTrue(conn.prepareBooleanQuery(query3).evaluate());

	}

	@Test
	public void testSES2336NegatedPropertyPathMod() throws Exception {
		loadTestData("/testdata-query/dataset-ses2336.trig");
		String query = "prefix : <http://example.org/> select * where { ?s a :Test ; !:p? ?o . }";

		ValueFactory vf = conn.getValueFactory();
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertNotNull(result);

			IRI a = vf.createIRI(EX.NAMESPACE, "a");
			IRI b = vf.createIRI(EX.NAMESPACE, "b");
			IRI c = vf.createIRI(EX.NAMESPACE, "c");
			IRI d = vf.createIRI(EX.NAMESPACE, "d");
			IRI e = vf.createIRI(EX.NAMESPACE, "e");
			IRI test = vf.createIRI(EX.NAMESPACE, "Test");

			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", a)));
			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", test)));
			assertTrue(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", c)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", d)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", e)));
			assertTrue(containsSolution(result, new SimpleBinding("s", d), new SimpleBinding("o", test)));

			assertFalse(containsSolution(result, new SimpleBinding("s", a), new SimpleBinding("o", b)));

		} catch (QueryEvaluationException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testSES1685propPathSameVar() throws Exception {
		final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);

		try (Stream<BindingSet> result = query.evaluate().stream()) {
			long count = result.count();
			assertEquals(2, count);
		}
	}

	@Test
	public void testSES1073InverseSymmetricPattern() {
		IRI a = iri("http://example.org/a");
		IRI b1 = iri("http://example.org/b1");
		IRI b2 = iri("http://example.org/b2");
		IRI c1 = iri("http://example.org/c1");
		IRI c2 = iri("http://example.org/c2");
		IRI a2b = iri("http://example.org/a2b");
		IRI b2c = iri("http://example.org/b2c");
		conn.add(a, a2b, b1);
		conn.add(a, a2b, b2);
		conn.add(b1, b2c, c1);
		conn.add(b2, b2c, c2);
		String query = "select * ";
		query += "where{ ";
		query += "?c1 ^<http://example.org/b2c>/^<http://example.org/a2b>/<http://example.org/a2b>/<http://example.org/b2c> ?c2 . ";
		query += " } ";
		TupleQuery tq = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);

		try (Stream<BindingSet> result = tq.evaluate().stream()) {
			long count = result.count();
			assertEquals(4, count);
		}
	}

	/**
	 * @see <a href="https://github.com/eclipse/rdf4j/issues/2727">GH-2727</a>
	 */
	@Test
	public void testNestedInversePropertyPathWithZeroLength() {
		String insert = "insert data {\n"
				+ "    <urn:1> <urn:prop> <urn:object> .\n"
				+ "    <urn:2> <urn:prop> <urn:mid:1> .\n"
				+ "    <urn:mid:1> <urn:prop> <urn:object> .\n"
				+ "    <urn:3> <urn:prop> <urn:mid:2> .\n"
				+ "    <urn:mid:2> <urn:prop> <urn:mid:3> .\n"
				+ "    <urn:mid:3> <urn:prop> <urn:object> .\n"
				+ "}";

		String query = "select * where { \n"
				+ "    <urn:object> (^<urn:prop>)? ?o .\n"
				+ "}";

		conn.prepareUpdate(insert).execute();

		TupleQuery tq = conn.prepareTupleQuery(query);

		List<BindingSet> result = QueryResults.asList(tq.evaluate());
		assertThat(result).hasSize(4);
	}

	private boolean containsSolution(List<BindingSet> result, Binding... solution) {
		final MapBindingSet bs = new MapBindingSet();
		for (Binding b : solution) {
			bs.addBinding(b);
		}
		return result.contains(bs);
	}
}
