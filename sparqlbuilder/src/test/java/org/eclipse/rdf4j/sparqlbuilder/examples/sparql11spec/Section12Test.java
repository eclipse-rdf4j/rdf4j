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
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.SubSelect;

public class Section12Test extends BaseExamples {
	@Test
	public void example_12() {
		Prefix base = SparqlBuilder.prefix(iri("http://people.example/"));

		// using this method of variable creation, as ?y and ?minName will be
		// used in both the outer and inner queries
		Variable y = SparqlBuilder.var("y"), minName = SparqlBuilder.var("minName");

		SubSelect sub = GraphPatterns.select();
		Variable name = sub.var();
		sub.select(y, Expressions.min(name).as(minName)).where(y.has(base.iri("name"), name)).groupBy(y);

		query.prefix(base, base) // SparqlBuilder even fixes typos for you ;)
				.select(y, minName)
				.where(base.iri("alice").has(base.iri("knows"), y), sub);
		p();
	}
}