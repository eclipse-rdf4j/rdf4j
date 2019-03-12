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

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

public class Section6Test extends BaseExamples {
	@Test
	public void example_6_1() {
		Variable name = SparqlBuilder.var("name"), mbox = SparqlBuilder.var("mbox");
		Variable x = query.var();
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));

		GraphPatternNotTriples where = GraphPatterns.and(x.has(foaf.iri("name"), name),
				GraphPatterns.optional(x.has(foaf.iri("mbox"), mbox)));

		query.prefix(foaf).select(name, mbox).where(where);
		p();
	}

	@Test
	public void example_6_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
		Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price"), x = SparqlBuilder.var("x");

		GraphPatternNotTriples pricePattern = GraphPatterns.and(x.has(ns.iri("price"), price))
				.filter(Expressions.lt(price, 30))
				.optional();

		query.prefix(dc, ns).select(title, price).where(x.has(dc.iri("title"), title), pricePattern);
		p();
	}

	@Test
	public void example_6_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = SparqlBuilder.var("name"), mbox = SparqlBuilder.var("mbox"), hpage = SparqlBuilder.var("hpage");
		Variable x = query.var();

		TriplePattern namePattern = x.has(foaf.iri("name"), name);

		query.prefix(foaf)
				.select(name, mbox, hpage)
				.where(namePattern, GraphPatterns.and(x.has(foaf.iri("mbox"), mbox)).optional(),
						GraphPatterns.and(x.has(foaf.iri("homepage"), hpage)).optional());
		p();
	}
}