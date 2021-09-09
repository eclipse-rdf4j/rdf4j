/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Expressions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

public class PropertyPathTest {
	@Test
	public void testEmptyPropertyPathBuilderFromExpressions() {
		PropertyPath p = Expressions.path().pred(iri(RDFS.LABEL)).build();
		Assertions.assertEquals("<" + RDFS.LABEL + ">", p.getQueryString());
	}

	@Test
	public void testPredPropertyPathBuilderFromExpressions() {
		PropertyPath p = Expressions.path(iri(RDFS.LABEL)).build();
		Assertions.assertEquals("<" + RDFS.LABEL + ">", p.getQueryString());
	}

	@Test
	public void testAlt() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.LABEL))
				.or(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testAltWithSubBuilder() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.LABEL))
				.or(builder -> builder.pred(iri(RDFS.COMMENT)))
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testAltWithSubPath() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.LABEL))
				.or(Expressions.path(iri(RDFS.COMMENT)).build())
				.build();
		Assertions.assertEquals("( <" + RDFS.LABEL + "> | <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testSeq() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubBuilder() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(builder -> builder.pred(iri(RDFS.COMMENT)))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubPath() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(Expressions.path(iri(RDFS.COMMENT)).build())
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testSeqWithSubAltPath() {
		PropertyPath p = Expressions
				.path()
				.pred(iri(RDFS.SUBCLASSOF))
				.then(b -> b.pred(iri(RDFS.COMMENT)).or(iri(RDFS.LABEL)))
				.build();
		Assertions.assertEquals("<" + RDFS.SUBCLASSOF + "> / ( <" + RDFS.COMMENT + "> | <" + RDFS.LABEL + "> )",
				p.getQueryString());
	}

	@Test
	public void testGroupedPath() {
		PropertyPath p = Expressions
				.path(iri(RDFS.COMMENT))
				.group()
				.build();
		Assertions.assertEquals("( <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testInversePath() {
		PropertyPath p = Expressions
				.path(iri(RDFS.COMMENT))
				.inv()
				.build();
		Assertions.assertEquals("^ ( <" + RDFS.COMMENT + "> )", p.getQueryString());
	}

	@Test
	public void testOneOrMorePath() {
		PropertyPath p = Expressions
				.path(iri(RDFS.COMMENT))
				.oneOrMore()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> +", p.getQueryString());
	}

	@Test
	public void testZeroOrMorePath() {
		PropertyPath p = Expressions
				.path(iri(RDFS.COMMENT))
				.zeroOrMore()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> *", p.getQueryString());
	}

	@Test
	public void testZeroOrOnePath() {
		PropertyPath p = Expressions
				.path(iri(RDFS.COMMENT))
				.zeroOrOne()
				.build();
		Assertions.assertEquals("<" + RDFS.COMMENT + "> ?", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetSingle() {
		PropertyPath p = Expressions
				.path()
				.negProp()
				.pred(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("! <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetSingleInverted() {
		PropertyPath p = Expressions
				.path()
				.negProp()
				.invPred(iri(RDFS.COMMENT))
				.build();
		Assertions.assertEquals("! ^ <" + RDFS.COMMENT + ">", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetMultipleInverted() {
		PropertyPath p = Expressions
				.path()
				.negProp()
				.invPred(iri(RDFS.COMMENT))
				.invPred(iri(RDFS.LABEL))
				.build();
		Assertions.assertEquals("! ( ^ <" + RDFS.COMMENT + "> | ^ <" + RDFS.LABEL + "> )", p.getQueryString());
	}

	@Test
	public void testNegatedPropertySetMultipleMixed() {
		PropertyPath p = Expressions
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
}
