/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns.and;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.junit.Assert;
import org.junit.Test;

public class Section5Test extends BaseExamples {
	@Test
	public void example_5_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), mbox = var("mbox");
		Variable x = var("x");
		query.prefix(foaf).select(name, mbox).where(x.has(foaf.iri("name"), name), x.has(foaf.iri("mbox"), mbox));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox\n"
						+ "WHERE  {\n"
						+ "          ?x foaf:name ?name .\n"
						+ "          ?x foaf:mbox ?mbox .\n"
						+ "       }"
		));
		GraphPattern namePattern = and(x.has(foaf.iri("name"), name));
		GraphPattern mboxPattern = and(x.has(foaf.iri("mbox"), mbox));
		QueryPattern where = SparqlBuilder.where(and(namePattern, mboxPattern));
		query.where(where);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox\n"
						+ "WHERE  { { ?x foaf:name ?name . }\n"
						+ "         { ?x foaf:mbox ?mbox . }\n"
						+ "       }"
		));
	}

	@Test
	public void example_5_2_1() {
		Variable x = var("x");
		query.select(x);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?x\n"
						+ "WHERE {}"));
	}

	@Test
	public void example_5_2_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable x = var("x"), name = var("name"), mbox = var("mbox");
		Assert.assertThat(
				x.has(foaf.iri("name"), name)
						.and(x.has(foaf.iri("mbox"), mbox))
						.and()
						.filter(Expressions.regex(name, "Smith"))
						.getQueryString(),
				stringEqualsIgnoreCaseAndWhitespace(
						" {  ?x foaf:name ?name .\n"
								+ "    ?x foaf:mbox ?mbox .\n"
								+ "    FILTER ( regex(?name, \"Smith\"))\n"
								+ " }\n"
				));
		// NOTE: removed the other two examples in which the filter expression is before or between the
		// triple patterns, respectively. The SparqlBuilder cannot generate this without additional curly
		// braces, and the point of the examples is to show that all these versions are equivalent, so
		// we can probably live witouth being able to generate filters at random places inside the
		// graph pattern.
	}
}
