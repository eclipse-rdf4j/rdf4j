/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class Section8Test extends BaseExamples {
	@Test
	public void example_8_1_1() {
		String rdf_ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		Prefix rdf = SparqlBuilder.prefix("rdf", iri(rdf_ns));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable person = var("person");

		GraphPattern personWithName = person.has(foaf.iri("name"), var("name"));
		GraphPatternNotTriples personOfTypePerson = GraphPatterns.and(person.has(rdf.iri("type"), foaf.iri("Person")));
		query.prefix(rdf, foaf).select(person).where(personOfTypePerson.filterNotExists(personWithName));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
						+ "PREFIX  foaf:   <http://xmlns.com/foaf/0.1/> \n"
						+ "\n"
						+ "SELECT ?person\n"
						+ "WHERE \n"
						+ "{\n"
						+ "    ?person rdf:type  foaf:Person .\n"
						+ "    FILTER NOT EXISTS { ?person foaf:name ?name .}\n"
						+ "}     "
		));
	}

	@Test
	public void example_8_1_2() {
		String rdf_ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		Prefix rdf = SparqlBuilder.prefix("rdf", iri(rdf_ns));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable person = var("person");

		GraphPattern personWithName = person.has(foaf.iri("name"), var("name"));
		GraphPatternNotTriples personOfTypePerson = GraphPatterns.and(person.has(rdf.iri("type"), foaf.iri("Person")));
		query.prefix(rdf, foaf).select(person).where(personOfTypePerson.filterExists(personWithName));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
						+ "PREFIX  foaf:   <http://xmlns.com/foaf/0.1/> \n"
						+ "\n"
						+ "SELECT ?person\n"
						+ "WHERE \n"
						+ "{\n"
						+ "    ?person rdf:type  foaf:Person .\n"
						+ "    FILTER EXISTS { ?person foaf:name ?name . }\n"
						+ "}"
		));
	}

	@Test
	public void example_8_2() {
		Prefix base = SparqlBuilder.prefix(iri("http://example/"));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable s = var("s"), p = var("p"), o = var("o");
		/*
		 * "{ ?s ?x1 ?x2} MINUS { ?s foaf:givenName "Bob" }
		 */
		GraphPattern allNotNamedBob = GraphPatterns.and(s.has(p, o))
				.minus(s.has(foaf.iri("givenName"), Rdf.literalOf("Bob")));
		query.prefix(base, foaf).select(s).distinct().where(allNotNamedBob);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX :       <http://example/>\n"
						+ "PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT DISTINCT ?s\n"
						+ "WHERE {\n"
						+ "   ?s ?p ?o .\n"
						+ "   MINUS {\n"
						+ "      ?s foaf:givenName \"Bob\" .\n"
						+ "   }\n"
						+ "}"
		));
	}

	@Test
	public void example_8_3_2() {
		Prefix base = SparqlBuilder.prefix(iri("http://example/"));
		Variable s = var("s"), p = var("p"), o = var("o");
		Iri a = base.iri("a"), b = base.iri("b"), c = base.iri("c");

		query.prefix(base).all().where(GraphPatterns.and(s.has(p, o)).filterNotExists(GraphPatterns.tp(a, b, c)));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example/>\n"
						+ "SELECT * WHERE \n"
						+ "{ \n"
						+ "  ?s ?p ?o .\n"
						+ "  FILTER NOT EXISTS { :a :b :c .}\n"
						+ "}"
		));

		QueryPattern where = SparqlBuilder.where(GraphPatterns.and(s.has(p, o)).minus(GraphPatterns.tp(a, b, c)));

		// passing a QueryPattern object to the query (rather than graph
		// pattern(s)) replaces (rather than augments) the query's
		// query pattern. This allows reuse of the other elements of the query.
		query.where(where);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example/>\n"
						+ "SELECT * WHERE \n"
						+ "{ \n"
						+ "  ?s ?p ?o .\n"
						+ "  MINUS { :a :b :c . }\n"
						+ "}"
		));
	}

	@Test
	public void example_8_3_3() {
		Prefix base = SparqlBuilder.prefix(iri("http://example.com/"));
		Variable x = var("x"), m = var("m"), n = var("n");
		Expression<?> filter = Expressions.equals(n, m);

		GraphPattern notExistsFilter = GraphPatterns.and(x.has(base.iri("p"), n))
				.filterNotExists(GraphPatterns.and(x.has(base.iri("q"), m)).filter(filter));

		query.prefix(base).select().all().where(notExistsFilter);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example.com/>\n"
						+ "SELECT * WHERE {\n"
						+ "        ?x :p ?n . \n"
						+ "        FILTER NOT EXISTS {\n"
						+ "                ?x :q ?m .\n"
						+ "                FILTER(?n = ?m)\n"
						+ "        }\n"
						+ "}"
		));

		QueryPattern where = SparqlBuilder.where(GraphPatterns.and(x.has(base.iri("p"), n))
				.minus(GraphPatterns.and(x.has(base.iri("q"), m)).filter(filter)));
		query.where(where);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX : <http://example.com/>\n"
						+ "SELECT * WHERE {\n"
						+ "        ?x :p ?n . \n"
						+ "        MINUS {\n"
						+ "                ?x :q ?m .\n"
						+ "                FILTER(?n = ?m)\n"
						+ "        }\n"
						+ "}"
		));
	}
}
