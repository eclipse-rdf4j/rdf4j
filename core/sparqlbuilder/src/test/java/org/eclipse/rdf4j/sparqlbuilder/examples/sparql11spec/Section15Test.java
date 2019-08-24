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

import org.eclipse.rdf4j.sparqlbuilder.core.OrderBy;
import org.eclipse.rdf4j.sparqlbuilder.core.OrderCondition;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.PrefixDeclarations;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;

public class Section15Test extends BaseExamples {
	@Test
	public void example_15_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();

		TriplePattern employeePattern = x.has(foaf.iri("name"), name);
		query.prefix(foaf).select(name).where(employeePattern).orderBy(name);
		p();

		Prefix base = SparqlBuilder.prefix(iri("http://example.org/ns#"));
		PrefixDeclarations prefixes = SparqlBuilder.prefixes(foaf, base);
		Variable emp = query.var();

		OrderCondition empDesc = SparqlBuilder.desc(emp);

		// calling prefix() with a PrefixDeclarations instance (rather than
		// Prefix objects) replaces (rather than augments) the query's
		// prefixes
		query.prefix(prefixes);

		// we can still modify graph patterns
		employeePattern.andHas(base.iri("empId"), emp);

		// similarly, calling orderBy() with an OrderBy instance (rather
		// than Orderable instances) replaces (rather than augments)
		// the query's order conditions
		query.orderBy(SparqlBuilder.orderBy(empDesc));
		p();

		OrderBy order = SparqlBuilder.orderBy(name, empDesc);
		query.orderBy(order);
		p();
	}

	@Test
	public void example_15_3_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();

		query.prefix(foaf).select(name).distinct().where(x.has(foaf.iri("name"), name));
		p();
	}

	@Test
	public void example_15_3_2() {
		p("REDUCED not yet implemented");
	}

	@Test
	public void example_15_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();

		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name)).orderBy(name).limit(5).offset(10);
		p();
	}

	@Test
	public void example_15_5() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = query.var(), x = query.var();

		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name)).limit(20);
		p();
	}
}