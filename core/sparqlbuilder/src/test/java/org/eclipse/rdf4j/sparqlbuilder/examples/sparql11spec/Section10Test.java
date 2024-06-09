/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.notEquals;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Values;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.junit.jupiter.api.Test;

public class Section10Test extends BaseExamples {
	private final Prefix rdfs = SparqlBuilder.prefix("rdfs", iri(RDFS.NS.getName()));
	private final Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));
	private final Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
	private final Prefix base = SparqlBuilder.prefix("", iri("http://example/"));
	private final Prefix ex = prefix("ex", iri("http://example/"));
	private final Prefix rdf = prefix("rdf", iri(RDF.NAMESPACE));

	private final Prefix prefixBook = prefix("", iri("http://example.org/book/"));
	private final Prefix ns = prefix("ns", iri("http://example.org/ns#"));
	private final Variable displayString = var("displayString");
	private final Variable x = var("x");

	private final Variable book = var("book");
	private final Variable title = var("title");
	private final Variable price = var("price");

	private final Variable name = var("name");
	private final Variable y = var("y");
	private final Variable z = var("z");
	private final Variable ancestor = var("ancestor");
	private final Variable type = var("type");
	private final Variable p = var("p");
	private final Variable v = var("v");
	private final Variable element = var("element");
	private final Variable s = var("s");
	private final Variable total = var("total");
	private final Variable person = var("person");
	private final Iri property = base.iri("property");
	private final Iri me = iri("#me");
	private final Iri thing = iri("http://example/thing");
	private final Iri book1 = base.iri("book1");
	private final Iri book3 = base.iri("book3");
	private final Iri order = base.iri("order");
	private final Iri mailto = iri("mailto:alice@example");
	private final Iri list = base.iri("list");

	@Test
	public void example_10_2_1__two_vars_two_solutions_one_undef() {
		Values values = Values.builder()
				.variables(x, y)
				.values(base.iri("uri1"), Rdf.literalOf(1))
				.values(base.iri("uri2"), null)
				.build();
		String str = values.getQueryString();
		assertThat(str).is(stringEqualsIgnoreCaseAndWhitespace(
				"VALUES (?x ?y) {\n"
						+ "  (:uri1 1)\n"
						+ "  (:uri2 UNDEF)\n"
						+ "}"
		));
	}

	@Test
	public void example_10_2_1__one_var_two_solutions() {
		Values values = Values.builder().variables(z).value(Rdf.literalOf("abc")).value(Rdf.literalOf("def")).build();
		String str = values.getQueryString();
		assertThat(str).is(stringEqualsIgnoreCaseAndWhitespace(
				"VALUES ?z { \"abc\" \"def\" }"
		));
	}

	@Test
	public void example_10_2_2__values__in__graphpattern() {
		String str = Queries.SELECT(book, title, price)
				.prefix(dc, prefixBook, ns)
				.where(Values.builder()
						.variables(book)
						.values(book1, book3)
						.build()
						.and(book.has(dc.iri("title"), title)
								.andHas(ns.iri("price"), price)))
				.getQueryString();
		assertThat(str).is(stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc:   <http://purl.org/dc/elements/1.1/>\n"
						+ "\tPREFIX :     <http://example.org/book/>\n"
						+ "\tPREFIX ns:   <http://example.org/ns#>\n"
						+ "\n"
						+ "\tSELECT ?book ?title ?price\n"
						+ "WHERE {\n"
						+ "\t\tVALUES ?book { :book1 :book3 }\n"
						+ "   ?book dc:title ?title ;\n"
						+ "\t\tns:price ?price .\n"
						+ "\t}"
		));
	}

	@Test
	public void example_10_2_2__values__at__end() {
		String str = Queries.SELECT(book, title, price)
				.prefix(dc, prefixBook, ns)
				.where(book.has(dc.iri("title"), title)
						.andHas(ns.iri("price"), price))
				.values(v -> v
						.variables(book, title)
						.values(null, Rdf.literalOf("SPARQL Tutorial"))
						.values(prefixBook.iri("book2"), null))
				.getQueryString();
		assertThat(str).is(stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX dc:   <http://purl.org/dc/elements/1.1/> \n"
						+ "PREFIX :     <http://example.org/book/> \n"
						+ "PREFIX ns:   <http://example.org/ns#> \n"
						+ "\n"
						+ "SELECT ?book ?title ?price\n"
						+ "WHERE {\n"
						+ "   ?book dc:title ?title ;\n"
						+ "         ns:price ?price .\n"
						+ "}\n"
						+ "VALUES (?book ?title)\n"
						+ "{ (UNDEF \"SPARQL Tutorial\")\n"
						+ "  (:book2 UNDEF)\n"
						+ "}"
		));

	}

}
