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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.StringLiteral;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.junit.jupiter.api.Test;

public class Section4Test extends BaseExamples {
	Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
	Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name");

	@Test
	public void example_4_1_4() {
		Prefix defPrefix = SparqlBuilder.prefix(iri(DC_NS));

		// [ :p "v" ] .
		PropertiesBlankNode bnode = Rdf.bNode(defPrefix.iri("p"), Rdf.literalOf("v"));
		assertThat(bnode.toTp().getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace("[ :p \"v\"] ."));

		// [] :p "v" .
		TriplePattern tp = Rdf.bNode().has(defPrefix.iri("p"), Rdf.literalOf("v"));
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace("[] :p \"v\" ."));

		// [ :p "v" ] :q "w" .
		tp = bnode.has(defPrefix.iri("q"), Rdf.literalOf("w"));
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace("[ :p \"v\" ] :q \"w\" ."));

		// :x :q [ :p "v" ] .
		tp = defPrefix.iri("x").has(defPrefix.iri("q"), bnode);
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(":x :q [ :p \"v\" ] ."));

		RdfBlankNode labelledNode = Rdf.bNode("b57");
		tp = defPrefix.iri("x").has(defPrefix.iri("q"), labelledNode);
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(":x :q _:b57 ."));
		tp = labelledNode.has(defPrefix.iri("p"), "v");
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace("_:b57 :p \"v\". "));

		// [ foaf:name ?name ;
		// foaf:mbox <mailto:alice@example.org> ]
		bnode = Rdf.bNode(foaf.iri("name"), name).andHas(foaf.iri("mbox"), iri("mailto:alice@example.org"));
		assertThat(bnode.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"[ foaf:name ?name ;"
						+ "foaf:mbox <mailto:alice@example.org> ]"
		));
	}

	@Test
	public void example_4_2_1() {
		Variable mbox = SparqlBuilder.var("mbox");

		TriplePattern tp = GraphPatterns.tp(x, foaf.iri("name"), name).andHas(foaf.iri("mbox"), mbox);
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"    ?x  foaf:name  ?name ;\n"
						+ "        foaf:mbox  ?mbox ."
		));
	}

	@Test
	public void example_4_2_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name");
		Iri nick = foaf.iri("nick");
		StringLiteral aliceNick = Rdf.literalOf("Alice"), alice_Nick = Rdf.literalOf("Alice_");

		TriplePattern tp = GraphPatterns.tp(x, nick, aliceNick, alice_Nick);
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"?x foaf:nick  \"Alice\" , \"Alice_\" ."
		));

		tp = x.has(foaf.iri("name"), name).andHas(nick, aliceNick, alice_Nick);
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"?x  foaf:name ?name ; foaf:nick  \"Alice\" , \"Alice_\" ."
		));
	}

	@Test
	public void example_4_2_4() {
		Prefix defPrefix = SparqlBuilder.prefix(iri(DC_NS));

		// isA() is a shortcut method to create triples using the "a" keyword
		TriplePattern tp = SparqlBuilder.var("x").isA(defPrefix.iri("Class1"));
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace("?x  a  :Class1 ."));

		// the isA predicate is a static member of RdfPredicate
		tp = Rdf.bNode(RdfPredicate.a, defPrefix.iri("appClass")).has(defPrefix.iri("p"), "v");
		assertThat(tp.getQueryString()).is(stringEqualsIgnoreCaseAndWhitespace(
				"  [ a :appClass ] :p \"v\" ."
		));
	}
}
