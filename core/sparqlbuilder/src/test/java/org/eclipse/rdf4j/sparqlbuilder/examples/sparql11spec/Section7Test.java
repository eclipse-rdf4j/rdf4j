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

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.Assert;
import org.junit.Test;

public class Section7Test extends BaseExamples {
	@Test
	public void example_7() {
		String DC_10_NS = "http://purl.org/dc/elements/1.0/";
		Prefix dc10 = SparqlBuilder.prefix("dc10", iri(DC_10_NS)), dc11 = SparqlBuilder.prefix("dc11", iri(DC_NS));
		Variable title = var("title"), book = var("book");

		Iri dc10TitleIri = dc10.iri("title");
		Iri dc11TitleIri = dc11.iri("title");

		GraphPattern titlePattern = GraphPatterns.union(book.has(dc10TitleIri, title), book.has(dc11TitleIri, title));

		query.prefix(dc10).prefix(dc11).select(title).where(titlePattern);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc10:  <http://purl.org/dc/elements/1.0/>\n"
						+ "PREFIX dc11:  <http://purl.org/dc/elements/1.1/>\n"
						+ "\n"
						+ "SELECT ?title\n"
						+ "WHERE  { { ?book dc10:title  ?title . } UNION { ?book dc11:title  ?title . } }"
		));

		resetQuery();
		Variable x = var("x"), y = var("y"), author = var("author");
		GraphPattern dc10Title = book.has(dc10TitleIri, x), dc11Title = book.has(dc11TitleIri, y);
		query.prefix(dc10, dc11).select(x, y).where(GraphPatterns.union(dc10Title, dc11Title));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc10:  <http://purl.org/dc/elements/1.0/>\n"
						+ "PREFIX dc11:  <http://purl.org/dc/elements/1.1/>\n"
						+ "\n"
						+ "SELECT ?x ?y\n"
						+ "WHERE  { { ?book dc10:title ?x . } UNION { ?book dc11:title  ?y . } }"
		));

		resetQuery();
		GraphPattern dc10Author = book.has(dc10.iri("creator"), author),
				dc11Author = book.has(dc11.iri("creator"), author);
		dc10Title = book.has(dc10TitleIri, title);
		dc11Title = book.has(dc11TitleIri, title);
		query.prefix(dc10, dc11)
				.select(title, author)
				.where(GraphPatterns.and(dc10Title, dc10Author).union(GraphPatterns.and(dc11Title, dc11Author)));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc10:  <http://purl.org/dc/elements/1.0/>\n"
						+ "PREFIX dc11:  <http://purl.org/dc/elements/1.1/>\n"
						+ "\n"
						+ "SELECT ?title ?author\n"
						+ "WHERE  { { ?book dc10:title ?title .  ?book dc10:creator ?author .}\n"
						+ "         UNION\n"
						+ "         { ?book dc11:title ?title .  ?book dc11:creator ?author .}\n"
						+ "       }"
		));
	}
}
