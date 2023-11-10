/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.eclipse.rdf4j.testsuite.sparql.vocabulary.EX;
import org.junit.jupiter.api.DynamicTest;

/**
 * Tests on SPARQL property paths.
 *
 * @author Jeen Broekstra
 *
 * @see ArbitraryLengthPathTest
 */
public class PropertyPathTest extends AbstractComplianceTest {

	public PropertyPathTest(Supplier<Repository> repo) {
		super(repo);
	}

	private void testSES2147PropertyPathsWithIdenticalSubsPreds(RepositoryConnection conn) throws Exception {

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

	private void testSES2024PropertyPathAnonVarSharing(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses2024.trig", conn);
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

	private void testPropertyPathNegationInversion(RepositoryConnection conn) throws Exception {
		String data = "@prefix : <http://example.org/>.\n" + ":Mary :parentOf :Jim.\n" + ":Jim :knows :Jane.\n"
				+ ":Jane :worksFor :IBM.";

		conn.add(new StringReader(data), "", RDFFormat.TURTLE);
		String query1 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) :Jane } ";

		assertTrue(conn.prepareBooleanQuery(query1).evaluate());

		String query2 = "prefix : <http://example.org/> ASK WHERE { :IBM ^(:|!:) ?a } ";
		assertTrue(conn.prepareBooleanQuery(query2).evaluate());

		String query3 = "prefix : <http://example.org/> ASK WHERE { :IBM (^(:|!:))* :Mary } ";
		assertTrue(conn.prepareBooleanQuery(query3).evaluate());

	}

	private void testSES2336NegatedPropertyPathMod(RepositoryConnection conn) throws Exception {
		loadTestData("/testdata-query/dataset-ses2336.trig", conn);
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

	private void testSES1685propPathSameVar(RepositoryConnection conn) throws Exception {
		final String queryStr = "PREFIX : <urn:> SELECT ?x WHERE {?x :p+ ?x}";

		conn.add(new StringReader("@prefix : <urn:> . :a :p :b . :b :p :a ."), "", RDFFormat.TURTLE);

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);

		try (Stream<BindingSet> result = query.evaluate().stream()) {
			long count = result.count();
			assertEquals(2, count);
		}
	}

	private void testSES1073InverseSymmetricPattern(RepositoryConnection conn) {
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

	private void testNestedInversePropertyPathWithZeroLength(RepositoryConnection conn) {
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

		try (final TupleQueryResult evaluate = tq.evaluate()) {
			List<BindingSet> result = QueryResults.asList(evaluate);
			assertThat(result).hasSize(4);
		}
	}

	private void testComplexPath(RepositoryConnection conn) {
		conn.add(Values.bnode(), SKOS.BROADER, Values.bnode());
		conn.add(Values.bnode(), SKOS.TOP_CONCEPT_OF, Values.bnode());

		TupleQuery tupleQuery = conn.prepareTupleQuery("PREFIX skos:<http://www.w3.org/2004/02/skos/core#> \r\n" +
				" SELECT *  " +
				" WHERE {\r\n" +
				"   ?s (skos:broader|^skos:narrower|skos:topConceptOf|^skos:hasTopConcept) ?o.\r\n" +
				" }");
		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			List<BindingSet> collect = evaluate.stream().collect(Collectors.toList());
			assertEquals(2, collect.size());
		}
	}

	private void testInversePath(RepositoryConnection conn) {
		BNode bnode1 = Values.bnode("bnode1");

		conn.add(Values.bnode(), FOAF.KNOWS, bnode1);
		conn.add(Values.bnode(), FOAF.KNOWS, bnode1);

		TupleQuery tupleQuery = conn.prepareTupleQuery("PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
				"SELECT * WHERE {\n" +
				"  ?x foaf:knows/^foaf:knows ?y . \n" +
				"  FILTER(?x != ?y)\n" +
				"}");

		try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
			List<BindingSet> collect = evaluate.stream().collect(Collectors.toList());
			assertEquals(2, collect.size());
		}
	}

	private boolean containsSolution(List<BindingSet> result, Binding... solution) {
		final MapBindingSet bs = new MapBindingSet();
		for (Binding b : solution) {
			bs.addBinding(b);
		}
		return result.contains(bs);
	}

	public Stream<DynamicTest> tests() {
		return Stream.of(
				makeTest("SES2147PropertyPathsWithIdenticalSubsPreds",
						this::testSES2147PropertyPathsWithIdenticalSubsPreds),
				makeTest("InversePath", this::testInversePath), makeTest("ComplexPath", this::testComplexPath),
				makeTest("NestedInversePropertyPathWithZeroLength", this::testNestedInversePropertyPathWithZeroLength),
				makeTest("SES1073InverseSymmetricPattern", this::testSES1073InverseSymmetricPattern),
				makeTest("SES1685propPathSameVar", this::testSES1685propPathSameVar),
				makeTest("SES2336NegatedPropertyPathMod", this::testSES2336NegatedPropertyPathMod),
				makeTest("PropertyPathNegationInversion", this::testPropertyPathNegationInversion),
				makeTest("SES2024PropertyPathAnonVarSharing", this::testSES2024PropertyPathAnonVarSharing));
	}
}
