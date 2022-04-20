/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.Test;

public class MinimalContextNowTest {

	@Test
	public void testNow() {

		// Tests that the now value is correctly initialized.
		QueryEvaluationContext.Minimal context = new QueryEvaluationContext.Minimal(null);
		QueryValueEvaluationStep prepared = new QueryValueEvaluationStep.ConstantQueryValueEvaluationStep(
				context.getNow());
		assertNotNull(prepared);
		Value nowValue = prepared.evaluate(EmptyBindingSet.getInstance());
		assertTrue(nowValue.isLiteral());
		Literal nowLiteral = (Literal) nowValue;
		assertEquals(CoreDatatype.XSD.DATETIME, nowLiteral.getCoreDatatype());
	}
}
