/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.util;

import static org.eclipse.rdf4j.query.algebra.MathExpr.MathOp;
import static org.junit.Assert.assertEquals;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.Test;

/**
 * @author Thomas Pellissier Tanon
 */
public class XMLDatatypeMathUtilTest {

	private ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void testCompute() throws Exception {
		Literal float1 = vf.createLiteral("12", XSD.INTEGER);
		Literal float2 = vf.createLiteral("2", XSD.INTEGER);
		Literal duration1 = vf.createLiteral("P1Y1M", XSD.YEARMONTHDURATION);
		Literal duration2 = vf.createLiteral("P1Y", XSD.YEARMONTHDURATION);
		Literal yearMonth1 = vf.createLiteral("2012-10", XSD.GYEARMONTH);

		assertComputeEquals(vf.createLiteral("14", XSD.INTEGER), float1, float2, MathOp.PLUS);
		assertComputeEquals(vf.createLiteral("10", XSD.INTEGER), float1, float2, MathOp.MINUS);
		assertComputeEquals(vf.createLiteral("24", XSD.INTEGER), float1, float2, MathOp.MULTIPLY);
		assertComputeEquals(vf.createLiteral("6", XSD.DECIMAL), float1, float2, MathOp.DIVIDE);

		assertComputeEquals(vf.createLiteral("P2Y1M", XSD.YEARMONTHDURATION), duration1, duration2, MathOp.PLUS);
		assertComputeEquals(vf.createLiteral("P0Y1M", XSD.YEARMONTHDURATION), duration1, duration2, MathOp.MINUS);

		assertComputeEquals(vf.createLiteral("P12Y", XSD.YEARMONTHDURATION), float1, duration2, MathOp.MULTIPLY);
		assertComputeEquals(vf.createLiteral("P12Y", XSD.YEARMONTHDURATION), duration2, float1, MathOp.MULTIPLY);

		assertComputeEquals(vf.createLiteral("2013-11", XSD.GYEARMONTH), yearMonth1, duration1, MathOp.PLUS);
		assertComputeEquals(vf.createLiteral("2011-09", XSD.GYEARMONTH), yearMonth1, duration1, MathOp.MINUS);
		assertComputeEquals(vf.createLiteral("2013-11", XSD.GYEARMONTH), duration1, yearMonth1, MathOp.PLUS);
	}

	private void assertComputeEquals(Literal result, Literal lit1, Literal lit2, MathOp op) throws Exception {
		assertEquals(result, XMLDatatypeMathUtil.compute(lit1, lit2, op));
	}
}
