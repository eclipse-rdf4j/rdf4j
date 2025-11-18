/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.jupiter.api.Test;

public class ModifyQueryTest extends BaseExamples {

	/**
	 * DELETE { GRAPH ?g1 { ?subject <http://my-example.com/anyIRI/> ?object . ?subject ?predicate
	 * <http://my-example.com/anyIRI/> . } } WHERE { GRAPH ?g1 { OPTIONAL { ?subject ?predicate
	 * <http://my-example.com/anyIRI/> . } OPTIONAL { ?subject <http://my-example.com/anyIRI/> ?object . } } }
	 */
	@Test
	public void example_issue_1481() {
		ModifyQuery modify = Queries.MODIFY();
		Iri g1 = () -> "<g1>";

		Variable subject = SparqlBuilder.var("subject");
		Variable obj = SparqlBuilder.var("object");
		Variable predicate = SparqlBuilder.var("predicate");

		TriplePattern delTriple1 = subject.has(iri("http://my-example.com/anyIRI/"), obj);
		TriplePattern delTriple2 = subject.has(predicate, iri("http://my-example.com/anyIRI/"));
		TriplePattern whereTriple1 = subject.has(predicate, iri("http://my-example.com/anyIRI/"));
		TriplePattern whereTriple2 = subject.has(iri("http://my-example.com/anyIRI/"), obj);

		modify.with(g1)
				.delete(delTriple1, delTriple2)
				.where(and(GraphPatterns.optional(whereTriple1), GraphPatterns.optional(whereTriple2)));

		assertEquals(modify.getQueryString(), "WITH <g1>\n" +
				"DELETE { ?subject <http://my-example.com/anyIRI/> ?object .\n" +
				"?subject ?predicate <http://my-example.com/anyIRI/> . }\n" +
				"WHERE { OPTIONAL { ?subject ?predicate <http://my-example.com/anyIRI/> . }\n" +
				"OPTIONAL { ?subject <http://my-example.com/anyIRI/> ?object . } }");
	}

	@Test
	public void example_broken_filter_not_exists() {
		// given
		Iri subjectIri = iri("http://my-example.com/anyIRI/");
		Iri classIri = iri("http://my-example.com/SomeClass/");
		TriplePattern triple = subjectIri.isA(classIri);

		String queryString = Queries.MODIFY()
				.insert(triple)
				.where(GraphPatterns.filterNotExists(triple))
				.getQueryString();

		assertEquals("INSERT { <http://my-example.com/anyIRI/> a <http://my-example.com/SomeClass/> . }\n" +
		// the WHERE clause is incorrectly generated:
		// "WHERE { <http://my-example.com/anyIRI/> a <http://my-example.com/SomeClass/> . }",
		// should be:
				"WHERE { FILTER NOT EXISTS { <http://my-example.com/anyIRI/> a <http://my-example.com/SomeClass/> . } }",
				queryString
		);
	}

	@Test
	public void test_GraphPatternNotTriples_getQueryString() {
		// given
		Iri subjectIri = iri("http://my-example.com/anyIRI/");
		Iri classIri = iri("http://my-example.com/SomeClass/");
		TriplePattern triple = subjectIri.isA(classIri);

		String queryString = GraphPatterns.filterNotExists(triple).getQueryString();

		assertEquals(
				"FILTER NOT EXISTS { <http://my-example.com/anyIRI/> a <http://my-example.com/SomeClass/> . }",
				queryString
		);
	}

	@Test
	public void test_GraphPatterns_and_getQueryString() {
		GraphPatternNotTriples actual = GraphPatterns.and();
		assertEquals("{}", actual.getQueryString());
	}

	@Test
	public void test_GraphPatterns_and_FilterExistsGraphPattern_getQueryString() {

		TriplePattern triple = iri("http://my-example.com/anyIRI/").isA(iri("http://my-example.com/SomeClass/"));

		// emptyGraphPattern by itself yields "{}", see test_GraphPatterns_and_getQueryString
		GraphPatternNotTriples emptyGraphPattern = GraphPatterns.and();

		// filterNotExists by itself yields "FILTER NOT EXISTS { ... }", see test_GraphPatternNotTriples_getQueryString
		GraphPatternNotTriples filterNotExists = GraphPatterns.filterNotExists(triple);

		// this is the cause oft the failing example_broken_filter_not_exists test
		GraphPatternNotTriples withFilterNotExists = emptyGraphPattern.and(filterNotExists);

		String actual = withFilterNotExists.getQueryString();

		assertEquals(
				"{ FILTER NOT EXISTS { <http://my-example.com/anyIRI/> a <http://my-example.com/SomeClass/> . } }",
				actual
		);
	}
}
