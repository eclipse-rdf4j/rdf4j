/*
 * *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * *****************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder;

import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.toRdfLiteralArray;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PropertyPathTest {
	@Test
	public void testEmptyPropertyPathBuilderFromExpressions() {
		PropertyPath p = PropertyPaths.path().pred(iri(RDFS.LABEL)).build();
		Assertions.assertEquals("<" + RDFS.LABEL + ">", p.getQueryString());
	}

	@Test
	public void testPredPropertyPathBuilderFromExpressions() {
		PropertyPath p = PropertyPaths.path(iri(RDFS.LABEL)).build();
		Assertions.assertEquals("<" + RDFS.LABEL + ">", p.getQueryString());
	}

	@Test
	public void testAlt() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.LABEL))
				.or(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testAltWithSubBuilder() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.LABEL))
				.or(builder -> builder.pred(iri(RDFS.COMMENT)))
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testAltWithSubPath() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.LABEL))
				.or(PropertyPaths.path(iri(RDFS.COMMENT)).build())
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testSeq() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubBuilder() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(builder -> builder.pred(iri(RDFS.COMMENT)))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubPath() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(PropertyPaths.path(iri(RDFS.COMMENT)).build())
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubAltPath() {
		PropertyPath p = PropertyPaths
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(b -> b.pred(iri(RDFS.COMMENT)).or(iri(RDFS.LABEL)))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / ( <" + RDFS.COMMENT + "> | <" + RDFS.LABEL + "> )",
				p.getQueryString());
	}

	@Test
	public void testGroupedPath() {
		PropertyPath p = PropertyPaths
				.path(iri(RDFS.COMMENT))
				.group()
				.build();
		Assertions.assertEquals("( <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testInversePath() {
		PropertyPath p = PropertyPaths
				.path(iri(RDFS.COMMENT))
				.inv()
				.build();
		Assertions.assertEquals("^ ( <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testOneOrMorePath() {
		PropertyPath p = PropertyPaths
				.path(iri(RDFS.COMMENT))
				.oneOrMore()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> +", p.getQueryString());
	}

	@Test
	public void testZeroOrMorePath() {
		PropertyPath p = PropertyPaths
				.path(iri(RDFS.COMMENT))
				.zeroOrMore()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> *", p.getQueryString());
	}

	@Test
	public void testZeroOrOnePath() {
		PropertyPath p = PropertyPaths
				.path(iri(RDFS.COMMENT))
				.zeroOrOne()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> ?", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetSingle() {
		PropertyPath p = PropertyPaths
				.path()
				.negProp()
				.pred(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("! <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetSingleInverted() {
		PropertyPath p = PropertyPaths
				.path()
				.negProp()
				.invPred(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("! ^ <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetMultipleInverted() {
		PropertyPath p = PropertyPaths
				.path()
				.negProp()
				.invPred(iri(RDFS.COMMENT))
				.invPred(iri(RDFS.LABEL))
				.build();
		Assertions.assertEquals("! ( ^ <" + RDFS.COMMENT + "> | ^ <" + RDFS.LABEL + "> )", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetMultipleMixed() {
		PropertyPath p = PropertyPaths
				.path()
				.negProp()
				.invPred(iri(RDFS.SUBCLASSOF))
				.pred(iri(RDFS.LABEL))
				.invPred(iri(RDFS.SUBPROPERTYOF))
				.pred(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("! ( ^ <" + RDFS.SUBCLASSOF + "> | <" + RDFS.LABEL + "> | ^ <" + RDFS.SUBPROPERTYOF
				+ "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testRdfSubjectHasPropertyPathRdfObject() {
		Variable x = var("x");
		TriplePattern tp = x.has(p -> p.pred(iri(FOAF.ACCOUNT)).then(iri(FOAF.MBOX)),
				toRdfLiteralArray("bob@example.com"));
		Assert.assertEquals("?x " + iri(FOAF.ACCOUNT).getQueryString() + " / " + iri(FOAF.MBOX).getQueryString()
				+ " \"bob@example.com\" .", tp.getQueryString());
	}

	@Test
	public void testRdfSubjectHasPropertyPathValue() {
		Variable x = var("x");
		TriplePattern tp = x.has(p -> p.pred(iri(FOAF.ACCOUNT)).then(iri(FOAF.MBOX)), iri("mailto:bob@example.com"));
		Assert.assertEquals("?x " + iri(FOAF.ACCOUNT).getQueryString() + " / " + iri(FOAF.MBOX).getQueryString()
				+ " <mailto:bob@example.com> .", tp.getQueryString());
	}

	@Test
	public void testRdfSubjectHasPropertyPathString() {
		Variable x = var("x");
		TriplePattern tp = x.has(p -> p.pred(iri(FOAF.ACCOUNT)).then(iri(FOAF.MBOX)), "bob@example.com");
		Assert.assertEquals("?x " + iri(FOAF.ACCOUNT).getQueryString() + " / " + iri(FOAF.MBOX).getQueryString()
				+ " \"bob@example.com\" .", tp.getQueryString());
	}

	@Test
	public void testRdfSubjectHasPropertyPathNumber() {
		Variable x = var("x");
		TriplePattern tp = x.has(p -> p.pred(iri(FOAF.KNOWS)).then(iri(FOAF.AGE)), 20);
		Assert.assertEquals("?x " + iri(FOAF.KNOWS).getQueryString() + " / " + iri(FOAF.AGE).getQueryString() + " 20 .",
				tp.getQueryString());
	}

	@Test
	public void testRdfSubjectHasPropertyPathBoolean() {
		Variable x = var("x");
		TriplePattern tp = x.has(p -> p.pred(iri(FOAF.ACCOUNT)).then(iri("http://example.com/ns#premium")), true);
		Assert.assertEquals("?x " + iri(FOAF.ACCOUNT).getQueryString() + " / "
				+ iri("http://example.com/ns#premium").getQueryString() + " true .", tp.getQueryString());
	}

}
