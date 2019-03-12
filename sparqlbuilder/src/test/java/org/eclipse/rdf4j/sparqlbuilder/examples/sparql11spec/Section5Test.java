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
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;

public class Section5Test extends BaseExamples {
	@Test
	public void example_5_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = SparqlBuilder.var("name"), mbox = SparqlBuilder.var("mbox");
		Variable x = query.var();

		query.prefix(foaf).select(name, mbox).where(x.has(foaf.iri("name"), name), x.has(foaf.iri("mbox"), mbox));
		p();

		GraphPattern namePattern = GraphPatterns.and(x.has(foaf.iri("name"), name));
		GraphPattern mboxPattern = GraphPatterns.and(x.has(foaf.iri("mbox"), mbox));
		QueryPattern where = SparqlBuilder.where(GraphPatterns.and(namePattern, mboxPattern));
		query.where(where);
		p();
	}

	@Test
	public void example_5_2_1() {
		p(GraphPatterns.and());

		query.select(query.var());
		p();
	}

	@Test
	public void example_5_2_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name"), mbox = SparqlBuilder.var("mbox");

		p(GraphPatterns.and(x.has(foaf.iri("name"), name), x.has(foaf.iri("mbox"), mbox)),

				GraphPatterns.and(x.has(foaf.iri("name"), name), x.has(foaf.iri("mbox"), mbox))
						.filter(Expressions.regex(name, "Smith")),

				GraphPatterns.and(x.has(foaf.iri("name"), name), GraphPatterns.and(), x.has(foaf.iri("mbox"), mbox)));
	}
}