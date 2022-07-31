/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.util;

import java.util.HashSet;

import org.eclipse.rdf4j.federated.algebra.ConjunctiveFilterExpr;
import org.eclipse.rdf4j.federated.algebra.FilterExpr;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Compare.CompareOp;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FilterUtilTest {

	@Test
	public void testConjunctiveFilterExpr() throws Exception {

		FilterExpr left = createFilterExpr("age", 15, CompareOp.GT);
		FilterExpr right = createFilterExpr("age", 25, CompareOp.LT);
		ConjunctiveFilterExpr expr = new ConjunctiveFilterExpr(left, right);

		Assertions.assertEquals(
				"( ( ?age > '15'^^<http://www.w3.org/2001/XMLSchema#int> ) && ( ?age < '25'^^<http://www.w3.org/2001/XMLSchema#int> ) )",
				FilterUtils.toSparqlString(expr));
	}

	private FilterExpr createFilterExpr(String leftVarName, int rightConstant, CompareOp operator) {
		Compare compare = new Compare(new Var(leftVarName), valueConstant(rightConstant), operator);
		return new FilterExpr(compare, new HashSet<>());

	}

	private ValueExpr valueConstant(int constant) {
		return new ValueConstant(FedXUtil.valueFactory().createLiteral(constant));
	}
}
