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

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Spanqit;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatternNotTriple;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

public class Section6Test extends BaseExamples {
	@Test
	public void example_6_1() {
		Variable name = Spanqit.var("name"), mbox = Spanqit.var("mbox");
		Variable x = query.var();
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		
		GraphPatternNotTriple where = GraphPatterns.and(
				x.has(foaf.iri("name"), name),
				GraphPatterns.optional(x.has(foaf.iri("mbox"), mbox)));

		query.prefix(foaf).select(name, mbox).where(where);
		p();
	}

	@Test
	public void example_6_2() {
		Prefix dc = Spanqit.prefix("dc", iri(DC_NS)),
			   ns = Spanqit.prefix("ns", iri(EXAMPLE_ORG_NS));
		Variable title = Spanqit.var("title"), price = Spanqit
				.var("price"), x = Spanqit.var("x");

		GraphPatternNotTriple pricePattern = GraphPatterns
				.and(x.has(ns.iri("price"), price))
				.filter(Expressions.lt(price, 30)).optional();

		query.prefix(dc, ns).select(title, price)
				.where(x.has(dc.iri("title"), title), pricePattern);
		p();
	}

	@Test
	public void example_6_3() {
		Prefix foaf = Spanqit.prefix("foaf", iri(FOAF_NS));
		Variable name = Spanqit.var("name"), mbox = Spanqit.var("mbox"), hpage = Spanqit
				.var("hpage");
		Variable x = query.var();

		TriplePattern namePattern = x.has(foaf.iri("name"), name);

		query.prefix(foaf)
				.select(name, mbox, hpage)
				.where(namePattern,
						GraphPatterns.and(x.has(foaf.iri("mbox"), mbox))
								.optional(),
						GraphPatterns.and(
								x.has(foaf.iri("homepage"), hpage))
								.optional());
		p();
	}
}