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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.core.Assignment;
import org.eclipse.rdf4j.sparqlbuilder.core.GraphTemplate;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.Projection;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryPattern;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.ConstructQuery;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.junit.Assert;
import org.junit.Test;

public class Section16Test extends BaseExamples {
	@Test
	public void example_16_1_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));
		Prefix ns = SparqlBuilder.prefix("ns", iri(EXAMPLE_ORG_NS));
		Variable title = var("title"),
				p = var("p"),
				discount = var("discount"),
				price = var("price"),
				x = var("x");
		Operand one = Rdf.literalOf(1);

		Assignment discountedPrice = Expressions.multiply(p, Expressions.subtract(one, discount).parenthesize())
				.as(price);

		query.prefix(dc, ns)
				.select(title, discountedPrice)
				.where(x.has(ns.iri("price"), p), x.has(dc.iri("title"), title), x.has(ns.iri("discount"), discount));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "PREFIX  ns:  <https://example.org/ns#>\n"
						+ "SELECT  ?title ((?p*(1-?discount)) AS ?price) WHERE\n"
						+ "{ ?x ns:price ?p .\n"
						+ "  ?x dc:title ?title . \n"
						+ "  ?x ns:discount ?discount .\n"
						+ "}"
		));

		Variable fullPrice = var("fullPrice"), customerPrice = var("customerPrice");
		Expression<?> cPrice = Expressions.multiply(fullPrice, Expressions.subtract(one, discount).parenthesize());
		Projection newProjection = SparqlBuilder.select(title, p.as(fullPrice), cPrice.as(customerPrice));

		// similar to other elements, calling select() with a Projection instance
		// (rather than Projectable instances) replaces (rather than augments)
		// the query's projections
		query.select(newProjection);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc:  <http://purl.org/dc/elements/1.1/>\n"
						+ "PREFIX  ns:  <https://example.org/ns#>\n"
						+ "SELECT  ?title (?p AS ?fullPrice) ((?fullPrice*(1-?discount)) AS ?customerPrice) WHERE\n"
						+ "{ ?x ns:price ?p .\n"
						+ "   ?x dc:title ?title . \n"
						+ "   ?x ns:discount ?discount .\n"
						+ "}"
		));
	}

	@Test
	public void example_16_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS)),
				vcard = SparqlBuilder.prefix("vcard", iri("http://www.w3.org/2001/vcard-rdf/3.0#"));
		Iri aliceIri = Rdf.iri("http://example.org/person#", "Alice");
		Variable name = var("name"), x = var("x");
		Assert.assertThat(Queries.CONSTRUCT(aliceIri.has(vcard.iri("FN"), name))
				.where(x.has(foaf.iri("name"), name))
				.prefix(foaf, vcard)
				.getQueryString(),
				stringEqualsIgnoreCaseAndWhitespace(
						"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
								+ "PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>\n"
								+ "CONSTRUCT   { <http://example.org/person#Alice> vcard:FN ?name .}\n"
								+ "WHERE       { ?x foaf:name ?name . }"
				));
	}

	@Test
	public void example_16_2_1() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS)),
				vcard = SparqlBuilder.prefix("vcard", iri("http://www.w3.org/2001/vcard-rdf/3.0#"));

		ConstructQuery cQuery = Queries.CONSTRUCT();
		Variable x = var("x"), gname = var("gname"), fname = var("fname");
		RdfBlankNode v = Rdf.bNode("v");
		GraphTemplate template = SparqlBuilder.construct(x.has(vcard.iri("N"), v), v.has(vcard.iri("givenName"), gname),
				v.has(vcard.iri("familyName"), fname));

		cQuery.prefix(foaf, vcard)
				.construct(template)
				.where(x.has(foaf.iri("firstName"), gname).union(x.has(foaf.iri("givenname"), gname)),
						x.has(foaf.iri("surname"), fname).union(x.has(foaf.iri("family_name"), fname)));

		Assert.assertThat(cQuery.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf:    <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX vcard:   <http://www.w3.org/2001/vcard-rdf/3.0#>\n"
						+ "\n"
						+ "CONSTRUCT { ?x  vcard:N _:v .\n"
						+ "            _:v vcard:givenName ?gname .\n"
						+ "            _:v vcard:familyName ?fname .}\n"
						+ "WHERE\n"
						+ " {\n"
						+ "    { ?x foaf:firstname ?gname .} UNION  { ?x foaf:givenname   ?gname .} \n"
						+ "    { ?x foaf:surname   ?fname .} UNION  { ?x foaf:family_name ?fname .} \n"
						+ " }"
		));
	}

	@Test
	public void example_16_2_2() {
		Prefix dc = SparqlBuilder.prefix("dc", iri("http://purl.org/dc/elements/1.1/")),
				app = SparqlBuilder.prefix("app", iri("http://example.org/ns#")),
				xsd = SparqlBuilder.prefix("xsd", iri("http://www.w3.org/2001/XMLSchema#"));

		Map<String, Variable> vars = Arrays.stream("s,p,o,g,date".split(","))
				.collect(Collectors.toMap(Function.identity(), SparqlBuilder::var));
		Variable s = vars.get("s"), p = vars.get("p"), o = vars.get("o"), g = vars.get("g"), date = vars.get("date");

		QueryPattern where = SparqlBuilder.where(GraphPatterns
				.and(GraphPatterns.and(s.has(p, o)).from(g), g.has(dc.iri("publisher"), iri("http://www.w3.org/")),
						g.has(dc.iri("date"), date))
				.filter(Expressions.gt(Expressions.custom(app.iri("customDate"), date),
						Rdf.literalOfType("2005-02-28T00:00:00Z", xsd.iri("dateTime")))));

		ConstructQuery query = Queries.CONSTRUCT(s.has(p, o)).where(where).prefix(dc, app, xsd);

		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  dc: <http://purl.org/dc/elements/1.1/>\n"
						+ "PREFIX app: <http://example.org/ns#>\n"
						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "\n"
						+ "CONSTRUCT { ?s ?p ?o .} WHERE\n"
						+ " {\n"
						+ "   GRAPH ?g { ?s ?p ?o .} \n"
						+ "   ?g dc:publisher <http://www.w3.org/> .\n"
						+ "   ?g dc:date ?date .\n"
						+ "   FILTER ( app:customDate(?date) > \"2005-02-28T00:00:00Z\"^^xsd:dateTime )\n"
						+ " }"
		));
	}

	@Test
	public void example_16_2_3() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri("http://xmlns.com/foaf/0.1/")),
				site = SparqlBuilder.prefix("site", iri("http://example.org/stats#"));
		Variable name = var("name"), hits = var("hits");
		RdfBlankNode subject = Rdf.bNode();

		ConstructQuery query = Queries.CONSTRUCT(subject.has(foaf.iri("name"), name))
				.prefix(foaf, site)
				.where(subject.has(foaf.iri("name"), name).andHas(site.iri("hits"), hits))
				.orderBy(hits.desc())
				.limit(2);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "PREFIX site: <http://example.org/stats#>\n"
						+ "\n"
						+ "CONSTRUCT { [] foaf:name ?name . }\n"
						+ "WHERE\n"
						+ "{ [] foaf:name ?name ;\n"
						+ "     site:hits ?hits .\n"
						+ "}\n"
						+ "ORDER BY desc(?hits)\n"
						+ "LIMIT 2"
		));
	}

	@Test
	public void example_16_2_4() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri("http://xmlns.com/foaf/0.1/"));
		Variable x = var("x"), name = var("name");

		ConstructQuery query = Queries.CONSTRUCT(x.has(foaf.iri("name"), name))
				.where(x.has(foaf.iri("name"), name))
				.prefix(foaf);
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "\n"
						+ "CONSTRUCT { ?x foaf:name ?name .} \n"
						+ "WHERE\n"
						+ "{ ?x foaf:name ?name .}"
		));
	}
}
