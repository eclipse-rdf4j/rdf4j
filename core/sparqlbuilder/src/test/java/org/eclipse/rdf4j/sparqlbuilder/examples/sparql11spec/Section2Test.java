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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignment;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.PrefixDeclarations;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Assert;
import org.junit.Test;

public class Section2Test extends BaseExamples {
	@Test
	public void example_2_1() {
		Variable title = var("title");

		TriplePattern book1_has_title = GraphPatterns.tp(Rdf.iri(EXAMPLE_ORG_BOOK_NS, "book1"), Rdf.iri(DC_NS, "title"),
				title);

		query.select(title).where(book1_has_title);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?title\n"
						+ "WHERE\n"
						+ "{\n"
						+ "  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .\n"
						+ "}"
		));
	}

	@Test
	public void example_2_1_model() {
		Variable title = var("title");
		String ex = EXAMPLE_ORG_BOOK_NS;
		IRI book1 = VF.createIRI(ex, "book1");

		TriplePattern book1_has_title = GraphPatterns.tp(book1, DC.TITLE, title);

		query.select(title).where(book1_has_title);

		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?title\n"
						+ "WHERE\n"
						+ "{\n"
						+ "  <http://example.org/book/book1> <http://purl.org/dc/elements/1.1/title> ?title .\n"
						+ "}"
		));
	}

	@Test
	public void example_2_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));

		/**
		 * As a shortcut, Query objects can create variables that will be unique to the query instance.
		 */
		Variable name = var("name"), mbox = var("mbox"), x = var("x");

		TriplePattern x_hasFoafName_name = GraphPatterns.tp(x, foaf.iri("name"), name);
		TriplePattern x_hasFoafMbox_mbox = GraphPatterns.tp(x, foaf.iri("mbox"), mbox);

		query.prefix(foaf).select(name, mbox).where(x_hasFoafName_name, x_hasFoafMbox_mbox);

		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox\n"
						+ "WHERE\n"
						+ "  { ?x foaf:name ?name .\n"
						+ "    ?x foaf:mbox ?mbox .}"
		));
	}

	@Test
	public void example_2_2_model() {
		Prefix foaf = SparqlBuilder.prefix(FOAF.NS);

		/**
		 * As a shortcut, Query objects can create variables that will be unique to the query instance.
		 */
		Variable name = var("name"), mbox = var("mbox"), x = var("x");

		TriplePattern x_hasFoafName_name = GraphPatterns.tp(x, foaf.iri("name"), name);
		TriplePattern x_hasFoafMbox_mbox = GraphPatterns.tp(x, foaf.iri("mbox"), mbox);

		query.prefix(foaf).select(name, mbox).where(x_hasFoafName_name, x_hasFoafMbox_mbox);

		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name ?mbox\n"
						+ "WHERE\n"
						+ "  { ?x foaf:name ?name .\n"
						+ "    ?x foaf:mbox ?mbox .}"
		));
	}

	@Test
	public void example_2_3_1() {
		Variable v = var("v"), p = var("p");

		TriplePattern v_hasP_cat = GraphPatterns.tp(v, p, Rdf.literalOf("cat"));

		query.select(v).where(v_hasP_cat);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?v WHERE { ?v ?p \"cat\" .}"
		));

		TriplePattern v_hasP_cat_en = GraphPatterns.tp(v, p, Rdf.literalOfLanguage("cat", "en"));
		SelectQuery queryWithLangTag = Queries.SELECT(v).where(v_hasP_cat_en);
		Assert.assertThat(queryWithLangTag.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?v WHERE { ?v ?p \"cat\"@en . }"
		));
	}

	@Test
	public void example_2_3_2() {
		Variable v = var("v"), p = var("p");

		TriplePattern v_hasP_42 = GraphPatterns.tp(v, p, Rdf.literalOf(42));

		query.select(v).where(v_hasP_42);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?v WHERE { ?v ?p 42 . }"
		));
	}

	@Test
	public void example_2_3_3() {
		String datatype = "specialDatatype";
		Variable v = var("v"), p = var("p");
		TriplePattern v_hasP_abc_dt = GraphPatterns.tp(v, p,
				Rdf.literalOfType("abc", Rdf.iri(EXAMPLE_DATATYPE_NS, datatype)));

		query.select(v).where(v_hasP_abc_dt);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?v WHERE { ?v ?p \"abc\"^^<http://example.org/datatype#specialDatatype> .}"
		));
	}

	@Test
	public void example_2_3_3_model() {
		String datatype = "specialDatatype";
		Variable v = var("v"), p = var("p");

		Literal lit = VF.createLiteral("abc", VF.createIRI(EXAMPLE_DATATYPE_NS, datatype));

		TriplePattern v_hasP_abc_dt = GraphPatterns.tp(v, p, lit);

		query.select(v).where(v_hasP_abc_dt);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"SELECT ?v WHERE { ?v ?p \"abc\"^^<http://example.org/datatype#specialDatatype> .}"
		));

	}

	@Test
	public void example_2_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));

		Variable x = var("x"), name = var("name");
		query.prefix(foaf).select(x, name).where(x.has(foaf.iri("name"), name));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?x ?name\n"
						+ "WHERE  { ?x foaf:name ?name . }"
		));
	}

	@Test
	public void example_2_5() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS));
		Variable G = var("G"), P = var("P"), S = var("S"),
				name = var("name");

		Assignment concatAsName = SparqlBuilder.as(Expressions.concat(G, Rdf.literalOf(" "), S), name);

		query.prefix(foaf)
				.select(concatAsName)
				.where(GraphPatterns.tp(P, foaf.iri("givenName"), G).andHas(foaf.iri("surname"), S));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ( CONCAT(?G, \" \", ?S) AS ?name )\n"
						+ "WHERE  { ?P foaf:givenName ?G ; foaf:surname ?S .}"
		));
		query = Queries.SELECT();
		query.prefix(foaf)
				.select(name)
				.where(
						P
								.has(foaf.iri("givenName"), G)
								.andHas(foaf.iri("surname"), S),
						Expressions.bind(Expressions.concat(G, Rdf.literalOf(" "), S), name));

		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT ?name\n"
						+ "WHERE  { \n"
						+ "   ?P foaf:givenName ?G ; \n"
						+ "      foaf:surname ?S . \n"
						+ "   BIND(CONCAT(?G, \" \", ?S) AS ?name)\n"
						+ "}"
		));
	}

	@Test
	public void example_2_6() {
		Prefix foaf = SparqlBuilder.prefix("foaf", Rdf.iri(FOAF_NS)),
				org = SparqlBuilder.prefix("org", Rdf.iri(EXAMPLE_COM_NS));
		PrefixDeclarations prefixes = SparqlBuilder.prefixes(foaf, org);

		ConstructQuery graphQuery = Queries.CONSTRUCT();
		Variable x = var("x"), name = var("name");

		TriplePattern foafName = GraphPatterns.tp(x, foaf.iri("name"), name);
		TriplePattern orgName = GraphPatterns.tp(x, org.iri("employeeName"), name);

		graphQuery.prefix(prefixes).construct(foafName).where(orgName);
		Assert.assertThat(graphQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:   <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX org:    <https://example.com/ns#>\n"
						+ "\n"
						+ "CONSTRUCT { ?x foaf:name ?name .}\n"
						+ "WHERE  { ?x org:employeeName ?name .}"
		));
	}
}
