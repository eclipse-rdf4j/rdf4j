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

import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.examples.BaseExamples;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral.StringLiteral;

public class Section4Test extends BaseExamples {
	Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
	Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name");

	@Test
	public void example_4_1_4() {
		Prefix defPrefix = SparqlBuilder.prefix(iri(DC_NS));

		// [ :p "v" ] .
		PropertiesBlankNode bnode = Rdf.bNode(defPrefix.iri("p"), Rdf.literalOf("v"));
		p(bnode.toTp());

		// [] :p "v" .
		TriplePattern tp = Rdf.bNode().has(defPrefix.iri("p"), Rdf.literalOf("v"));
		p(tp);

		// [ :p "v" ] :q "w" .
		tp = bnode.has(defPrefix.iri("q"), Rdf.literalOf("w"));
		p(tp);

		// :x :q [ :p "v" ] .
		tp = defPrefix.iri("x").has(defPrefix.iri("q"), bnode);
		p(tp);

		RdfBlankNode labelledNode = Rdf.bNode("b57");
		p(defPrefix.iri("x").has(defPrefix.iri("q"), labelledNode));
		p(labelledNode.has(defPrefix.iri("p"), "v"));

		// [ foaf:name ?name ;
		// foaf:mbox <mailto:alice@example.org> ]
		bnode = Rdf.bNode(foaf.iri("name"), name).andHas(foaf.iri("mbox"), iri("mailto:alice@example.org"));
		p(bnode);
	}

	@Test
	public void example_4_2_1() {
		Variable mbox = SparqlBuilder.var("mbox");

		TriplePattern tp = GraphPatterns.tp(x, foaf.iri("name"), name).andHas(foaf.iri("mbox"), mbox);
		p(tp);
	}

	@Test
	public void example_4_2_2() {
		Prefix foaf = SparqlBuilder.prefix("foaf", iri(FOAF_NS));
		Variable x = SparqlBuilder.var("x"), name = SparqlBuilder.var("name");
		Iri nick = foaf.iri("nick");
		StringLiteral aliceNick = Rdf.literalOf("Alice"), alice_Nick = Rdf.literalOf("Alice_");

		TriplePattern tp = GraphPatterns.tp(x, nick, alice_Nick, aliceNick);
		p(tp);

		tp = x.has(nick, aliceNick, alice_Nick).andHas(foaf.iri("name"), name);
		p(tp);
	}

	@Test
	public void example_4_2_4() {
		Prefix defPrefix = SparqlBuilder.prefix(iri(DC_NS));

		// isA() is a shortcut method to create triples using the "a" keyword
		p(SparqlBuilder.var("x").isA(defPrefix.iri("Class1")));

		// the isA predicate is a static member of RdfPredicate
		p(Rdf.bNode(RdfPredicate.a, defPrefix.iri("appClass")).has(defPrefix.iri("p"), "v"));
	}
}
