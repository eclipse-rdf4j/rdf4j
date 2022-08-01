/*******************************************************************************
 * Copyright (c) ${year} Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.constraint;

import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ExpressionsTest {

	@Test
	public void bnodeProducesProperQueryString() {
		Expression<?> expression = Expressions.bnode("name");
		assertEquals("BNODE( \"name\" )", expression.getQueryString());
	}

	@Test
	public void bnodeNoArgumentProducesProperQueryString() {
		Expression<?> expression = Expressions.bnode();
		assertEquals("BNODE()", expression.getQueryString());
	}

	@Test
	public void testLogicalExpression() {
		Expression expression = Expressions.or(Expressions.lt(Rdf.literalOf(30), Rdf.literalOf(20)),
				Expressions.and(Expressions.gt(Rdf.literalOf(30), Rdf.literalOf(50)),
						Expressions.or(Expressions.gt(Rdf.literalOf(30), Rdf.literalOf(60)),
								Expressions.lt(Rdf.literalOf(30), Rdf.literalOf(70)))));

		assertEquals(expression.getQueryString(), "( 30 < 20 || ( 30 > 50 && ( 30 > 60 || 30 < 70 ) ) )");
	}

	@Test
	public void testArithmeticExpression() {
		Expression expression = Expressions.lt(Rdf.literalOf(30), Expressions.subtract(
				Expressions.divide(Rdf.literalOf(100), Rdf.literalOf(20)),
				Expressions.multiply(Rdf.literalOf(2),
						Expressions.add(Rdf.literalOf(5), Rdf.literalOf(3)))));

		assertEquals(expression.getQueryString(), "30 < ( ( 100 / 20 ) - ( 2 * ( 5 + 3 ) ) )");
	}

	@Test
	public void testArithmeticAndLogicalExpression() {
		Expression expression = Expressions.or(Expressions.lt(Rdf.literalOf(30), Expressions.add(Rdf.literalOf(20),
				Expressions.divide(Rdf.literalOf(10), Rdf.literalOf(5)))),
				Expressions.lt(Rdf.literalOf(30), Rdf.literalOf(50)));

		assertEquals(expression.getQueryString(), "( 30 < ( 20 + ( 10 / 5 ) ) || 30 < 50 )");
	}

	@Test
	public void test_BIND_fromOtherVariable() {
		Variable from = SparqlBuilder.var("from");
		Variable to = SparqlBuilder.var("to");
		Assertions.assertEquals(
				"BIND( ?from AS ?to )", Expressions.bind(from, to).getQueryString());
	}

	@Test
	public void test_NOT_IN_twoIris() {
		Variable test = SparqlBuilder.var("test");
		Assertions.assertEquals(
				"?test NOT IN ( <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, <http://www.w3.org/2000/01/rdf-schema#subClassOf> )",
				Expressions.notIn(test, Rdf.iri(RDF.TYPE), Rdf.iri(RDFS.SUBCLASSOF))
						.getQueryString());
	}

	@Test
	public void test_IS_BLANK() {
		Variable test = SparqlBuilder.var("test");
		Assertions.assertEquals(
				"isBLANK( ?test )", Expressions.isBlank(test).getQueryString());
	}
}
