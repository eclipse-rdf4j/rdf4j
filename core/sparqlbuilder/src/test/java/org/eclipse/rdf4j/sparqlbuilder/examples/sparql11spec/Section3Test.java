/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import org.eclipse.rdf4j.sparqlbuilder.graphpattern.*;
import org.junit.Test;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;

public class Section3Test extends BaseExamples {
	@Test
	public void example_3_1() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));

		Variable x = query.var(), title = SparqlBuilder.var("title");
		TriplePattern xTitle = GraphPatterns.tp(x, dc.iri("title"), title);

		Expression<?> regex = Expressions.regex(title, Rdf.literalOf("^SPARQL"));
		GraphPattern where = xTitle.filter(regex);

		query.prefix(dc).select(title).where(where);
		p();

		where.filter(Expressions.regex(title, "web", "i"));
		p();
	}

	@Test
	public void example_3_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS)), ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_COM_NS));

		Variable title = SparqlBuilder.var("title"), price = SparqlBuilder.var("price");
		Variable x = query.var();
		Expression<?> priceConstraint = Expressions.lt(price, 30.5);

		GraphPattern where = GraphPatterns.and(x.has(ns.iri("price"), price), x.has(dc.iri("title"), title))
				.filter(priceConstraint);

		query.prefix(dc, ns).select(title, price).where(where);
		p();
	}
}