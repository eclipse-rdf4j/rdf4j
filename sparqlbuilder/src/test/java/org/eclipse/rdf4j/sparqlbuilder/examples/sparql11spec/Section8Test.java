/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.junit.Test;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

public class Section8Test extends BaseExamples {
	@Test
	public void example_8_1_1() {
		String rdf_ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		Prefix rdf = SparqlBuilder.prefix("rdf", iri(rdf_ns));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable person = query.var();

		GraphPattern personWithName = person.has(foaf.iri("name"), SparqlBuilder.var("name"));
		GraphPatternNotTriples personOfTypePerson = GraphPatterns.and(person.has(rdf.iri("type"), foaf.iri("Person")));
		query.prefix(rdf, foaf).select(person).where(personOfTypePerson.filterNotExists(personWithName));
		p();
	}

	@Test
	public void example_8_1_2() {
		String rdf_ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
		Prefix rdf = SparqlBuilder.prefix("rdf", iri(rdf_ns));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable person = query.var();

		GraphPattern personWithName = person.has(foaf.iri("name"), SparqlBuilder.var("name"));
		GraphPatternNotTriples personOfTypePerson = GraphPatterns.and(person.has(rdf.iri("type"), foaf.iri("Person")));
		query.prefix(rdf, foaf).select(person).where(personOfTypePerson.filterExists(personWithName));
		p();
	}

	@Test
	public void example_8_2() {
		Prefix base = SparqlBuilder.prefix(iri("http://example/"));
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable s = query.var();
		/*
		 * "{ ?s ?x1 ?x2} MINUS { ?s foaf:givenName "Bob" }
		 */
		GraphPattern allNotNamedBob = GraphPatterns.and(s.has(query.var(), query.var()))
				.minus(s.has(foaf.iri("givenName"), Rdf.literalOf("Bob")));
		query.prefix(base, foaf).select(s).distinct().where(allNotNamedBob);
		p();
	}

	@Test
	public void example_8_3_2() {
		Prefix base = SparqlBuilder.prefix(iri("http://example/"));
		Variable s = query.var(), p = query.var(), o = query.var();
		Iri a = base.iri("a"), b = base.iri("b"), c = base.iri("c");

		query.prefix(base).all().where(GraphPatterns.and(s.has(p, o)).filterNotExists(GraphPatterns.tp(a, b, c)));
		p();

		QueryPattern where = SparqlBuilder.where(GraphPatterns.and(s.has(p, o)).minus(GraphPatterns.tp(a, b, c)));

		// passing a QueryPattern object to the query (rather than graph
		// pattern(s)) replaces (rather than augments) the query's
		// query pattern. This allows reuse of the other elements of the query.
		query.where(where);
		p();
	}

	@Test
	public void example_8_3_3() {
		Prefix base = SparqlBuilder.prefix(iri("http://example/"));
		Variable x = query.var(), m = query.var(), n = query.var();
		Expression<?> filter = Expressions.equals(n, m);

		GraphPattern notExistsFilter = GraphPatterns.and(x.has(base.iri("p"), n))
				.filterNotExists(GraphPatterns.and(x.has(base.iri("q"), m)).filter(filter));

		query.prefix(base).select().all().where(notExistsFilter);
		p();

		QueryPattern where = SparqlBuilder.where(GraphPatterns.and(x.has(base.iri("p"), n))
				.minus(GraphPatterns.and(x.has(base.iri("q"), m)).filter(filter)));
		query.where(where);
		p();
	}
}