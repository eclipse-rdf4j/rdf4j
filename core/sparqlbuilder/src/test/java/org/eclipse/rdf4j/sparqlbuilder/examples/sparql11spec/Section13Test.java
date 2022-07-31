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

import org.eclipse.rdf4j.sparqlbuilder.core.Dataset;
import org.eclipse.rdf4j.sparqlbuilder.core.From;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.Assert;
import org.junit.Test;

public class Section13Test extends BaseExamples {
	@Test
	public void example_13_2_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name");
		Variable x = var("x");
		From defaultGraph = SparqlBuilder.from(iri("http://example.org/foaf/aliceFoaf"));
		query.prefix(foaf).select(name).from(defaultGraph).where(x.has(foaf.iri("name"), name));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT  ?name\n"
						+ "FROM    <http://example.org/foaf/aliceFoaf>\n"
						+ "WHERE   { ?x foaf:name ?name . }"));
	}

	@Test
	public void example_13_2_2() {
		Dataset dataset = SparqlBuilder.dataset(SparqlBuilder.fromNamed(iri("http://example.org/alice")),
				SparqlBuilder.fromNamed(iri("http://example.org/bob")));
		Assert.assertThat(dataset.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(""
				+ "FROM NAMED <http://example.org/alice>\n"
				+ "FROM NAMED <http://example.org/bob>"));
	}

	@Test
	public void example_13_2_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));
		From defaultGraph = SparqlBuilder.from(iri("http://example.org/dft.ttl"));
		From aliceGraph = SparqlBuilder.fromNamed(iri("http://example.org/alice"));
		From bobGraph = SparqlBuilder.fromNamed(iri("http://example.org/bob"));
		Variable who = var("who"),
				g = var("g"),
				mbox = var("mbox"),
				x = var("x");
		GraphPattern namedGraph = GraphPatterns.and(x.has(foaf.iri("mbox"), mbox)).from(g);
		query.prefix(foaf, dc)
				.select(who, g, mbox)
				.from(defaultGraph, aliceGraph, bobGraph)
				.where(g.has(dc.iri("publisher"), who), namedGraph);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
						+ "\n"
						+ "SELECT ?who ?g ?mbox\n"
						+ "FROM <http://example.org/dft.ttl>\n"
						+ "FROM NAMED <http://example.org/alice>\n"
						+ "FROM NAMED <http://example.org/bob>\n"
						+ "WHERE\n"
						+ "{\n"
						+ "   ?g dc:publisher ?who .\n"
						+ "   GRAPH ?g { ?x foaf:mbox ?mbox . }\n"
						+ "}"));
	}

	@Test
	public void example_13_3_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable src = var("src"), bobNick = var("bobNick"), x = var("x");
		// TODO: still need to bracket GGP's that aren't explicitly GGP instances,
		// even if there's only 1
		query.prefix(foaf)
				.select(src, bobNick)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/foaf/aliceFoaf")),
						SparqlBuilder.fromNamed(iri("http://example.org/foaf/bobFoaf")))
				.where(GraphPatterns
						.and(x.has(foaf.iri("mbox"), iri("mailto:bob@work.example")),
								x.has(foaf.iri("nick"), bobNick))
						.from(src));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT ?src ?bobNick\n"
						+ "FROM NAMED <http://example.org/foaf/aliceFoaf>\n"
						+ "FROM NAMED <http://example.org/foaf/bobFoaf>\n"
						+ "WHERE\n"
						+ "  {\n"
						+ "    GRAPH ?src\n"
						+ "    { ?x foaf:mbox <mailto:bob@work.example> .\n"
						+ "      ?x foaf:nick ?bobNick .\n"
						+ "    }\n"
						+ "  }"));
	}

	@Test
	public void example_13_3_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix data = SparqlBuilder.prefix("data", iri("http://example.org/foaf/"));
		Variable x = var("x"), nick = var("nick");
		query.prefix(foaf, data)
				.select(nick)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/foaf/aliceFoaf")),
						SparqlBuilder.fromNamed(iri("http://example.org/foaf/bobFoaf")))
				.where(GraphPatterns
						.and(x.has(foaf.iri("mbox"), iri("mailto:bob@work.example")),
								x.has(foaf.iri("nick"), nick))
						.from(data.iri("bobFoaf")));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX data: <http://example.org/foaf/>\n"
						+ "\n"
						+ "SELECT ?nick\n"
						+ "FROM NAMED data:aliceFoaf\n"
						+ "FROM NAMED data:bobFoaf\n"
						+ "WHERE\n"
						+ "  {\n"
						+ "     GRAPH data:bobFoaf {\n"
						+ "         ?x foaf:mbox <mailto:bob@work.example> .\n"
						+ "         ?x foaf:nick ?nick .}\n"
						+ "  }"
		));
	}

	@Test
	public void example_13_3_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix data = SparqlBuilder.prefix("data", iri("http://example.org/foaf/"));
		Prefix rdfs = SparqlBuilder.prefix("rdfs", iri("http://www.w3.org/2000/01/rdf-schema#"));
		Variable mbox = var("mbox"),
				nick = var("nick"),
				ppd = var("ppd"),
				alice = var("alice"),
				whom = var("whom"),
				w = var("w");
		Iri foafMbox = foaf.iri("mbox");
		GraphPattern aliceFoafGraph = GraphPatterns
				.and(alice.has(foafMbox, iri("mailto:alice@work.example")).andHas(foaf.iri("knows"), whom),
						whom.has(foafMbox, mbox).andHas(rdfs.iri("seeAlso"), ppd),
						ppd.isA(foaf.iri("PersonalProfileDocument")))
				.from(data.iri("aliceFoaf"));
		GraphPattern ppdGraph = GraphPatterns.and(w.has(foafMbox, mbox).andHas(foaf.iri("nick"), nick)).from(ppd);
		query.prefix(data, foaf, rdfs)
				.select(mbox, nick, ppd)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/foaf/aliceFoaf")),
						SparqlBuilder.fromNamed(iri("http://example.org/foaf/bobFoaf")))
				.where(aliceFoafGraph, ppdGraph);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  data:  <http://example.org/foaf/>\n"
						+ "PREFIX  foaf:  <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX  rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n"
						+ "\n"
						+ "SELECT ?mbox ?nick ?ppd\n"
						+ "FROM NAMED data:aliceFoaf\n"
						+ "FROM NAMED data:bobFoaf\n"
						+ "WHERE\n"
						+ "{\n"
						+ "  GRAPH data:aliceFoaf\n"
						+ "  {\n"
						+ "    ?alice foaf:mbox <mailto:alice@work.example> ;\n"
						+ "           foaf:knows ?whom .\n"
						+ "    ?whom  foaf:mbox ?mbox ;\n"
						+ "           rdfs:seeAlso ?ppd .\n"
						+ "    ?ppd  a foaf:PersonalProfileDocument .\n"
						+ "  } \n"
						+ "  GRAPH ?ppd\n"
						+ "  {\n"
						+ "      ?w foaf:mbox ?mbox ;\n"
						+ "         foaf:nick ?nick .\n"
						+ "  }\n"
						+ "}"));
	}

	@Test
	public void example_13_3_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));
		Variable name = var("name"),
				mbox = var("mbox"),
				date = var("date"),
				g = var("g"),
				person = var("person");
		query.prefix(foaf, dc)
				.select(name, mbox, date)
				.where(g.has(dc.iri("publisher"), name).andHas(dc.iri("date"), date),
						GraphPatterns.and(person.has(foaf.iri("name"), name)
								.andHas(foaf.iri("mbox"), mbox)).from(g));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX dc:   <http://purl.org/dc/elements/1.1/>\n"
						+ "\n"
						+ "SELECT ?name ?mbox ?date\n"
						+ "WHERE\n"
						+ "  {  ?g dc:publisher ?name ;\n"
						+ "        dc:date ?date .\n"
						+ "    GRAPH ?g\n"
						+ "      { ?person foaf:name ?name ; foaf:mbox ?mbox . }\n"
						+ "  }"
		));
	}
}
