/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.examples.sparql11spec;

import static org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions.notEquals;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.prefix;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.junit.Assert;
import org.junit.Test;

public class Section9Test extends BaseExamples {
	private final Prefix rdfs = SparqlBuilder.prefix("rdfs", iri(RDFS.NS.getName()));
	private final Prefix dc = SparqlBuilder.prefix("dc", iri(DC_NS));
	private final Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
	private final Prefix base = SparqlBuilder.prefix("", iri("http://example/"));
	private final Prefix ex = prefix("ex", iri("http://example/"));
	private final Prefix rdf = prefix("rdf", iri(RDF.NAMESPACE));
	private final Variable displayString = var("displayString");
	private final Variable x = var("x");
	private final Variable name = var("name");
	private final Variable y = var("y");
	private final Variable ancestor = var("ancestor");
	private final Variable type = var("type");
	private final Variable p = var("p");
	private final Variable v = var("v");
	private final Variable element = var("element");
	private final Variable s = var("s");
	private final Variable total = var("total");
	private final Variable person = var("person");
	private final Iri property = base.iri("property");
	private final Iri me = iri("#me");
	private final Iri thing = iri("http://example/thing");
	private final Iri book1 = base.iri("book1");
	private final Iri order = base.iri("order");
	private final Iri mailto = iri("mailto:alice@example");
	private final Iri list = base.iri("list");

	@Test
	public void example_9_2__1_alt() {
		TriplePattern tp = book1.has(path -> path
				.pred(dc.iri("title"))
				.or(rdfs.iri("label")), displayString);
		// NOTE: changed example: removed curly braces around, added brackets in path, added dot at end
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				":book1 ( dc:title | rdfs:label ) ?displayString ."
		));
	}

	@Test
	public void example_9_2__2_alt_noprefix() {
		TriplePattern tp = book1.has(path -> path.pred(DC.TITLE).or(RDFS.LABEL), displayString);
		// NOTE: changed example: removed curly braces around, added brackets in path, added dot at end
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				":book1 ( <http://purl.org/dc/elements/1.1/title> | <http://www.w3.org/2000/01/rdf-schema#label> ) ?displayString ."
		));
	}

	@Test
	public void example_9_2__3_sequence() {
		GraphPattern gp = x.has(path -> path.pred(foaf.iri("mbox")), mailto)
				.and(x.has(path -> path
						.pred(foaf.iri("knows"))
						.then(foaf.iri("name")), name));
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"{\n"
						+ "    ?x foaf:mbox <mailto:alice@example> .\n"
						+ "    ?x foaf:knows / foaf:name ?name .\n"
						+ "  }"
						+ ""
		));
	}

	@Test
	public void example_9_2__4_double_sequence() {
		GraphPattern gp = x.has(path -> path.pred(foaf.iri("mbox")), mailto)
				.and(x.has(path -> path
						.pred(foaf.iri("knows"))
						.then(foaf.iri("knows"))
						.then(foaf.iri("name")), name));
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"{\n"
						+ "    ?x foaf:mbox <mailto:alice@example> .\n"
						+ "    ?x foaf:knows / foaf:knows / foaf:name ?name .\n"
						+ "  }"
						+ ""
		));
	}

	@Test
	public void example_9_2__5_double_sequence_filter() {
		GraphPattern gp = x.has(path -> path.pred(foaf.iri("mbox")), mailto)
				.and(x.has(path -> path
						.pred(foaf.iri("knows"))
						.then(foaf.iri("knows")), y))
				.filter(notEquals(x, y))
				.and(y.has(foaf.iri("name"), name));
		// NOTE: changed example: moved FILTER to end of graph pattern, added dot
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"  { ?x foaf:mbox <mailto:alice@example> .\n"
						+ "    ?x foaf:knows / foaf:knows ?y .\n"
						+ "    ?y foaf:name ?name .\n"
						+ "    FILTER ( ?x != ?y )\n"
						+ "  }"
		));
	}

	@Test
	public void example_9_2__6_inverse() {
		TriplePattern tp = mailto.has(path -> path.pred(foaf.iri("mbox")).inv(), x);
		// NOTE: changed example: removed curly braces, added dot, added brackets in path
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				" <mailto:alice@example> ^ ( foaf:mbox ) ?x ."
		));
	}

	@Test
	public void example_9_2__7_sequence_inverse() {
		GraphPattern gp = x.has(path -> path
				.pred(foaf.iri("knows"))
				.then(b -> b.pred(foaf.iri("knows"))
						.inv()),
				y)
				.filter(notEquals(x, y));
		// NOTE: changed example: added brackets in path
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"{\n"
						+ "    ?x foaf:knows/^ ( foaf:knows ) ?y .  \n"
						+ "    FILTER(?x != ?y)\n"
						+ "  }"
		));
	}

	@Test
	public void example_9_2__8_one_or_more() {
		GraphPattern gp = x.has(foaf.iri("mbox"), mailto)
				.and(x.has(path -> path
						.pred(foaf.iri("knows"))
						.oneOrMore()
						.then(foaf.iri("name")), name
				));
		// NOTE: changed example: added brackets in path
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"  {\n"
						+ "    ?x foaf:mbox <mailto:alice@example> .\n"
						+ "    ?x foaf:knows+/foaf:name ?name .\n"
						+ "  }"
		));
	}

	@Test
	public void example_9_2__9_alt_one_or_more() {
		TriplePattern tp = ancestor.has(path -> path
				.pred(ex.iri("motherOf"))
				.or(ex.iri("fatherOf"))
				.oneOrMore(), me);
		// NOTE: changed example: remove curly braces, added dot
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"?ancestor (ex:motherOf|ex:fatherOf)+ <#me> ."
		));
	}

	@Test
	public void example_9_2__10_sequence_zero_or_more() {
		// NOTE: changed example: remove curly braces, added dot
		TriplePattern tp = thing.has(path -> path
				.pred(rdf.iri("type"))
				.then(s -> s.pred(rdfs.iri("subClassOf"))
						.zeroOrMore()),
				type);
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"<http://example/thing> rdf:type / rdfs:subClassOf * ?type ."
		));
	}

	@Test
	public void example_9_2__11_sequence_one_or_more() {
		// NOTE: changed example: remove curly braces, added dot
		TriplePattern tp = x.has(path -> path
				.pred(rdf.iri("type"))
				.then(s -> s.pred(rdfs.iri("subClassOf"))
						.zeroOrMore()),
				type);
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"?x rdf:type / rdfs:subClassOf * ?type ."
		));
	}

	@Test
	public void example_9_2__12_zero_or_more() {
		// NOTE: changed example: remove curly braces, added dot
		GraphPattern gp = x.has(p, v)
				.and(p.has(path -> path
						.pred(rdfs.iri("subPropertyOf"))
						.zeroOrMore(),
						property));
		Assert.assertThat(gp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"{ ?x ?p ?v . ?p rdfs:subPropertyOf* :property . }"
		));
	}

	@Test
	public void example_9_2__13_negated_property_set() {
		TriplePattern tp = x.has(path -> path
				.negProp()
				.pred(rdf.iri("type"))
				.invPred(rdf.iri("type")), y);
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				" ?x !(rdf:type|^rdf:type) ?y ."
		));
	}

	@Test
	public void example_9_2__14_rdf_collection() {
		// NOTE: changed example: remove curly braces, added dot
		TriplePattern tp = list.has(path -> path
				.pred(rdf.iri("rest"))
				.zeroOrMore()
				.then(rdf.iri("first")), element);
		Assert.assertThat(tp.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				":list rdf:rest*/rdf:first ?element ."
		));
	}

	@Test
	public void example_9_3__1() {
		// Note: changed example: added WHERE,
		query = Queries.SELECT()
				.all()
				.prefix(base)
				.where(s.has(p -> p
						.pred(base.iri("item"))
						.then(base.iri("price")), x));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX :   <http://example/>\n"
						+ "SELECT * \n"
						+ "WHERE {  ?s :item/:price ?x . }"
		));
	}

	@Test
	public void example_9_3__2() {
		// Note: changed example: added WHERE,
		Variable _a = var("_a");
		query = Queries.SELECT()
				.all()
				.prefix(base)
				.where(s.has(base.iri("item"), _a)
						.and(_a.has(base.iri("price"), x)));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX :   <http://example/>\n"
						+ "SELECT * \n"
						+ "WHERE {"
						+ "  ?s :item ?_a .\n"
						+ "   ?_a :price ?x . "
						+ "}"
		));
	}

	@Test
	public void example_9_3__3() {
		// Note: changed example: added WHERE,
		Variable _a = var("_a");
		query = Queries.SELECT()
				.all()
				.prefix(base)
				.where(s.has(base.iri("item"), _a)
						.and(_a.has(base.iri("price"), x)));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX :   <http://example/>\n"
						+ "SELECT * \n"
						+ "WHERE {"
						+ "  ?s :item ?_a .\n"
						+ "   ?_a :price ?x . "
						+ "}"
		));
	}

	@Test
	public void example_9_3__4() {
		// Note: changed example: added WHERE,
		query = Queries.SELECT(SparqlBuilder.as(Expressions.sum(x), total))
				.prefix(base)
				.where(order.has(p -> p
						.pred(base.iri("item"))
						.then(base.iri("price")), x));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				" PREFIX :   <http://example/>\n"
						+ "  SELECT (sum(?x) AS ?total)\n"
						+ "  WHERE { \n"
						+ "    :order :item/:price ?x .\n"
						+ "  }"
		));
	}

	@Test
	public void example_9_4__1() {
		// Note: changed example: added WHERE, added dot
		query = Queries.SELECT(x, type)
				.prefix(RDFS.NS)
				.prefix(RDF.NS)
				.where(x.has(p -> p
						.pred(RDF.TYPE)
						.then(RDFS.SUBCLASSOF)
						.zeroOrMore(),
						type));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX  rdfs:   <http://www.w3.org/2000/01/rdf-schema#> \n"
						+ "  PREFIX  rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "  SELECT ?x ?type\n"
						+ "  WHERE { \n"
						+ "    ?x rdf:type/rdfs:subClassOf* ?type .\n"
						+ "  }"
		));
	}

	@Test
	public void example_9_4__2() {
		// Note: changed example: added WHERE, added dot
		query = Queries.SELECT(person)
				.prefix(FOAF.NS)
				.prefix(base)
				.where(base.iri("x")
						.has(p -> p
								.pred(FOAF.KNOWS)
								.oneOrMore(),
								person));
		Assert.assertThat(query.getQueryString(), stringEqualsIgnoreCaseAndWhitespace(
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
						+ "  PREFIX :     <http://example/>\n"
						+ "  SELECT ?person\n"
						+ "  WHERE { \n"
						+ "    :x foaf:knows+ ?person .\n"
						+ "  }"
		));
	}

}
