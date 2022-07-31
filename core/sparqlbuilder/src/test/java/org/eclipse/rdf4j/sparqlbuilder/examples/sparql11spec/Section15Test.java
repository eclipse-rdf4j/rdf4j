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

import org.eclipse.rdf4j.sparqlbuilder.core.OrderBy;
import org.eclipse.rdf4j.sparqlbuilder.core.OrderCondition;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.PrefixDeclarations;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.junit.Assert;
import org.junit.Test;

public class Section15Test extends BaseExamples {
	@Test
	public void example_15_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), x = var("x");

		TriplePattern employeePattern = x.has(foaf.iri("name"), name);
		query.prefix(foaf).select(name).where(employeePattern).orderBy(name);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name .}\n"
						+ "ORDER BY ?name"
		));

		Prefix base = SparqlBuilder.prefix(iri("http://example.org/ns#"));
		PrefixDeclarations prefixes = SparqlBuilder.prefixes(base, foaf);
		Variable emp = var("emp");

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
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX     :    <http://example.org/ns#>\n"
						+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name ; :empId ?emp .}\n"
						+ "ORDER BY DESC(?emp)"
		));

		OrderBy order = SparqlBuilder.orderBy(name, empDesc);
		query.orderBy(order);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX     :    <http://example.org/ns#>\n"
						+ "PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name ; :empId ?emp . }\n"
						+ "ORDER BY ?name DESC(?emp)"
		));
	}

	@Test
	public void example_15_3_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), x = var("x");

		query.prefix(foaf).select(name).distinct().where(x.has(foaf.iri("name"), name));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "SELECT DISTINCT ?name WHERE { ?x foaf:name ?name .}"
		));
	}

	@Test
	public void example_15_3_2() {
		System.err.println("REDUCED not yet implemented");
	}

	@Test
	public void example_15_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), x = var("x");

		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name)).orderBy(name).limit(5).offset(10);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT  ?name\n"
						+ "WHERE   { ?x foaf:name ?name . }\n"
						+ "ORDER BY ?name\n"
						+ "LIMIT   5\n"
						+ "OFFSET  10"
		));
	}

	@Test
	public void example_15_5() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable name = var("name"), x = var("x");

		query.prefix(foaf).select(name).where(x.has(foaf.iri("name"), name)).limit(20);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "SELECT ?name\n"
						+ "WHERE { ?x foaf:name ?name . }\n"
						+ "LIMIT 20"
		));
	}
}
