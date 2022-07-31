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
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class Section3Test extends BaseExamples {
	@Test
	public void example_3_1() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));

		Variable x = var("x"), title = var("title");
		TriplePattern xTitle = GraphPatterns.tp(x, dc.iri("title"), title);

		Expression<?> regex = Expressions.regex(title, Rdf.literalOf("^SPARQL"));
		GraphPattern where = xTitle.filter(regex);

		query.prefix(dc).select(title).where(where);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "SELECT  ?title\n"
						+ "WHERE   { ?x dc:title ?title .\n"
						+ "          FILTER (regex(?title, \"^SPARQL\")) \n"
						+ "        }"
		));
		query = Queries.SELECT();
		query.prefix(dc).select(title).where(xTitle.filter(Expressions.regex(title, "web", "i")));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "SELECT  ?title\n"
						+ "WHERE   { ?x dc:title ?title .\n"
						+ "          FILTER (regex(?title, \"web\", \"i\" ) )\n"
						+ "        }"
		));
	}

	@Test
	public void example_3_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_COM_NS));

		Variable title = var("title"), price = var("price");
		Variable x = var("x");
		Expression<?> priceConstraint = Expressions.lt(price, 30.5);

		GraphPattern where = x.has(ns.iri("price"), price).and(x.has(dc.iri("title"), title)).filter(priceConstraint);

		query.prefix(dc, ns).select(title, price).where(where);
		// NOTE: had to move FILTER to the end of the group graph pattern (in the original, it's between the two triple
		// patterns).
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "PREFIX  ns:  <https://example.com/ns#>\n"
						+ "SELECT  ?title ?price\n"
						+ "WHERE   { ?x ns:price ?price .\n"
						+ "          ?x dc:title ?title .\n "
						+ "          FILTER (?price < 30.5) "
						+ "}"
		));
	}
}
