/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import org.junit.Test;

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

public class Section13Test extends BaseExamples {
	@Test
	public void example_13_2_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var();
		From defaultGraph = SparqlBuilder.from(iri("http://example.org/foaf/aliceFoaf"));

		query.prefix(foaf).select(name).from(defaultGraph).where(query.var().has(foaf.iri("name"), name));
		p();
	}

	@Test
	public void example_13_2_2() {
		Dataset dataset = SparqlBuilder.dataset(SparqlBuilder.fromNamed(iri("http://example.org/alice")),
				SparqlBuilder.fromNamed(iri("http://example.org/bob")));
		p(dataset);
	}

	@Test
	public void example_13_2_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));

		From defaultGraph = SparqlBuilder.from(iri("http://example.org/dft.ttl"));
		From aliceGraph = SparqlBuilder.fromNamed(iri("http://example.org/alice"));
		From bobGraph = SparqlBuilder.fromNamed(iri("http://example.org/bob"));

		Variable who = query.var(), g = query.var(), mbox = query.var();
		GraphPattern namedGraph = GraphPatterns.and(query.var().has(foaf.iri("mbox"), mbox)).from(g);

		query.prefix(foaf, dc)
				.select(who, g, mbox)
				.from(defaultGraph, aliceGraph, bobGraph)
				.where(g.has(dc.iri("publisher"), who), namedGraph);
		p();
	}

	@Test
	public void example_13_3_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable src = query.var(), bobNick = query.var(), x = query.var();

		// TODO: still need to bracket GGP's that aren't explicitly GGP instances,
		// even if there's only 1
		query.prefix(foaf)
				.select(src, bobNick)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/alice")),
						SparqlBuilder.fromNamed(iri("http://example.org/bob")))
				.where(GraphPatterns
						.and(x.has(foaf.iri("mbox"), iri("mailto:bob@work.example")), x.has(foaf.iri("nick"), bobNick))
						.from(src));
		p();
	}

	@Test
	public void example_13_3_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix data = SparqlBuilder.prefix("data", iri("http://example.org/foaf/"));
		Variable x = query.var(), nick = query.var();

		query.prefix(foaf, data)
				.select(nick)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/foaf/aliceFoaf")),
						SparqlBuilder.fromNamed(iri("http://example.org/foaf/bobFoaf")))
				.where(GraphPatterns
						.and(x.has(foaf.iri("mbox"), iri("mailto:bob@work.example")), x.has(foaf.iri("nick"), nick))
						.from(data.iri("bobFoaf")));
		p();
	}

	@Test
	public void example_13_3_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix data = SparqlBuilder.prefix("data", iri("http://example.org/foaf/"));
		Prefix rdfs = SparqlBuilder.prefix("rdfs", iri("http://www.w3.org/2000/01/rdf-schema#"));

		Variable mbox = query.var(), nick = query.var(), ppd = query.var(), alice = query.var(), whom = query.var(),
				w = query.var();

		Iri foafMbox = foaf.iri("mbox");

		GraphPattern aliceFoafGraph = GraphPatterns
				.and(alice.has(foafMbox, iri("mailto:bob@work.example")).andHas(foaf.iri("knows"), whom),
						whom.has(foafMbox, mbox).andHas(rdfs.iri("seeAlso"), ppd),
						ppd.isA(foaf.iri("PersonalProfileDocument")))
				.from(data.iri("aliceFoaf"));
		GraphPattern ppdGraph = GraphPatterns.and(w.has(foafMbox, mbox).andHas(foaf.iri("nick"), nick)).from(ppd);

		query.prefix(data, foaf, rdfs)
				.select(mbox, nick, ppd)
				.from(SparqlBuilder.fromNamed(iri("http://example.org/foaf/aliceFoaf")),
						SparqlBuilder.fromNamed(iri("http://example.org/foaf/bobFoaf")))
				.where(aliceFoafGraph, ppdGraph);
		p();
	}

	@Test
	public void example_13_3_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));

		Variable name = query.var(), mbox = query.var(), date = query.var(), g = query.var(), person = query.var();

		query.prefix(foaf, dc)
				.select(name, mbox, date)
				.where(g.has(dc.iri("publisher"), name).andHas(dc.iri("date"), date),
						GraphPatterns.and(person.has(foaf.iri("name"), name).andHas(foaf.iri("mbox"), mbox)).from(g));
		p();
	}
}