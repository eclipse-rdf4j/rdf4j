/******************************************************************************* 
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Distribution License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php. 
 ********************************************************************************/
package org.eclipse.rdf4j.testsuite.sparql.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.testsuite.sparql.AbstractComplianceTest;
import org.junit.Test;

/**
 * Tests on SPARQL DESCRIBE queries
 * 
 * @author Jeen Broekstra
 */
public class DescribeTest extends AbstractComplianceTest {

	@Test
	public void testDescribeA() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:a";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Value> objects = result.filter(a, p, null).objects();
			assertThat(objects).isNotNull();
			for (Value object : objects) {
				if (object instanceof BNode) {
					assertThat(result.contains((Resource) object, null, null)).isTrue();
					assertThat(result.filter((Resource) object, null, null)).hasSize(2);
				}
			}
		}
	}

	@Test
	public void testDescribeAWhere() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ?x WHERE {?x rdfs:label \"a\". } ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI a = f.createIRI("http://example.org/a");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Value> objects = result.filter(a, p, null).objects();
			assertThat(objects).isNotNull();
			for (Value object : objects) {
				if (object instanceof BNode) {
					assertThat(result.contains((Resource) object, null, null)).isTrue();
					assertThat(result.filter((Resource) object, null, null)).hasSize(2);
				}
			}
		}
	}

	@Test
	public void testDescribeWhere() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ?x WHERE {?x rdfs:label ?y . } ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI a = vf.createIRI("http://example.org/a");
		IRI b = vf.createIRI("http://example.org/b");
		IRI c = vf.createIRI("http://example.org/c");
		IRI e = vf.createIRI("http://example.org/e");
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");

		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			assertThat(result.contains(a, p, null)).isTrue();
			assertThat(result.contains(b, RDFS.LABEL, null)).isTrue();
			assertThat(result.contains(c, RDFS.LABEL, null)).isTrue();
			assertThat(result.contains(null, p, b)).isTrue();
			assertThat(result.contains(e, RDFS.LABEL, null)).isTrue();
			assertThat(result.contains(null, p, e)).isTrue();
			assertThat(result.contains(f, null, null)).isFalse();
			Set<Value> objects = result.filter(a, p, null).objects();
			assertThat(objects).isNotNull();
			for (Value object : objects) {
				if (object instanceof BNode) {
					assertThat(result.contains((Resource) object, null, null)).isTrue();
					assertThat(result.filter((Resource) object, null, null)).hasSize(2);
				}
			}
		}
	}

	@Test
	public void testDescribeB() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:b";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI b = f.createIRI("http://example.org/b");
		IRI p = f.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			Set<Resource> subjects = result.filter(null, p, b).subjects();
			assertThat(subjects).isNotNull();
			for (Value subject : subjects) {
				if (subject instanceof BNode) {
					assertThat(result.contains(null, null, subject)).isTrue();
				}
			}
		}
	}

	@Test
	public void testDescribeD() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:d";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory f = conn.getValueFactory();
		IRI d = f.createIRI("http://example.org/d");
		IRI p = f.createIRI("http://example.org/p");
		IRI e = f.createIRI("http://example.org/e");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertThat(result.contains(null, p, e)).isTrue();
			assertThat(result.contains(e, null, null)).isFalse();

			Set<Value> objects = result.filter(d, p, null).objects();
			assertThat(objects).isNotNull();
			for (Value object : objects) {
				if (object instanceof BNode) {
					Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
					assertThat(childObjects).isNotEmpty();
					for (Value childObject : childObjects) {
						if (childObject instanceof BNode) {
							assertThat(result.contains((BNode) childObject, null, null)).isTrue();
						}
					}
				}
			}
		}
	}

	@Test
	public void testDescribeF() throws Exception {
		loadTestData("/testdata-query/dataset-describe.trig");
		String query = getNamespaceDeclarations() +
				"DESCRIBE ex:f";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI f = vf.createIRI("http://example.org/f");
		IRI p = vf.createIRI("http://example.org/p");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertThat(result).isNotNull().hasSize(4);

			Set<Value> objects = result.filter(f, p, null).objects();
			for (Value object : objects) {
				if (object instanceof BNode) {
					Set<Value> childObjects = result.filter((BNode) object, null, null).objects();
					assertThat(childObjects).isNotEmpty();
					for (Value childObject : childObjects) {
						if (childObject instanceof BNode) {
							assertThat(result.contains((BNode) childObject, null, null)).isTrue();
						}
					}
				}
			}
		}
	}

	@Test
	public void testDescribeMultipleA() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1> . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");

		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);
			assertThat(result.contains(urn1, p1, null)).isTrue();
			assertThat(result.contains(null, blank, urn1)).isTrue();
			assertThat(result.contains(urn2, p2, null)).isTrue();
		}
	}

	@Test
	public void testDescribeMultipleB() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertThat(result.contains(urn1, p1, null)).isTrue();
			assertThat(result.contains(urn1, blank, null)).isTrue();
			assertThat(result.contains(urn2, p2, null)).isTrue();
		}
	}

	@Test
	public void testDescribeMultipleC() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:1> <urn:blank> [] . <urn:2> <urn:p2> <urn:3> . } ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertThat(result.contains(urn1, p1, null)).isTrue();
			assertThat(result.contains(urn1, blank, null)).isTrue();
			assertThat(result.contains(null, blank, urn1)).isTrue();
			assertThat(result.contains(urn2, p2, null)).isTrue();
		}
	}

	@Test
	public void testDescribeMultipleD() {
		String update = "insert data { <urn:1> <urn:p1> <urn:v> . [] <urn:blank> <urn:1>. <urn:2> <urn:p2> <urn:3> . [] <urn:blank> <urn:2> . <urn:4> <urn:p2> <urn:3> . <urn:4> <urn:blank> [] .} ";
		conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();

		String query = getNamespaceDeclarations() +
				"DESCRIBE <urn:1> <urn:2> <urn:4> ";

		GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);

		ValueFactory vf = conn.getValueFactory();
		IRI urn1 = vf.createIRI("urn:1");
		IRI p1 = vf.createIRI("urn:p1");
		IRI p2 = vf.createIRI("urn:p2");
		IRI urn2 = vf.createIRI("urn:2");
		IRI urn4 = vf.createIRI("urn:4");
		IRI blank = vf.createIRI("urn:blank");
		try (GraphQueryResult evaluate = gq.evaluate()) {
			Model result = QueryResults.asModel(evaluate);

			assertThat(result.contains(urn1, p1, null)).isTrue();
			assertThat(result.contains(null, blank, urn1)).isTrue();
			assertThat(result.contains(urn2, p2, null)).isTrue();
			assertThat(result.contains(urn4, p2, null)).isTrue();
			assertThat(result.contains(urn4, blank, null)).isTrue();
		}
	}

}
