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

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.junit.Assert;
import org.junit.Test;

public class Section6Test extends BaseExamples {
	@Test
	public void example_6_1() {
		Variable name = var("name"), mbox = var("mbox");
		Variable x = var("x");
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));

		GraphPatternNotTriples where = GraphPatterns.and(x.has(foaf.iri("name"), name),
				GraphPatterns.optional(x.has(foaf.iri("mbox"), mbox)));

		query.prefix(foaf).select(name, mbox).where(where);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox\n"
						+ "WHERE  { ?x foaf:name  ?name .\n"
						+ "         OPTIONAL { ?x  foaf:mbox  ?mbox . }\n"
						+ "       }"
		));
	}

	@Test
	public void example_6_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
		Variable title = var("title"), price = var("price"), x = var("x");

		GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
				.filter(Expressions.lt(price, 30))
				.optional();

		query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "PREFIX  ns:  <https://example.org/ns#>\n"
						+ "SELECT  ?title ?price\n"
						+ "WHERE   { ?x dc:title ?title .\n"
						+ "          OPTIONAL { ?x ns:price ?price . FILTER (?price < 30) }\n"
						+ "        }"
		));
	}

	@Test
	public void example_6_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), mbox = var("mbox"), hpage = var("hpage");
		Variable x = var("x");

		TriplePattern namePattern = x.has(foaf.iri("name"), name);

		query.prefix(foaf)
				.select(name, mbox, hpage)
				.where(namePattern, GraphPatterns.and(x.has(foaf.iri("mbox"), mbox)).optional(),
						GraphPatterns.and(x.has(foaf.iri("homepage"), hpage)).optional());
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox ?hpage\n"
						+ "WHERE  { ?x foaf:name  ?name .\n"
						+ "         OPTIONAL { ?x foaf:mbox ?mbox . } \n"
						+ "         OPTIONAL { ?x foaf:homepage ?hpage .}\n"
						+ "       }"
		));
	}
}
