/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AbstractQueryModelNodeTest {

	@Test
	public void getCardinalityString() {

		{
			StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
			String cardinalityString = statementPattern.toHumanReadableNumber(statementPattern.getResultSizeEstimate());
			assertEquals("UNKNOWN", cardinalityString);
		}

		{
			StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
			statementPattern.setResultSizeEstimate(1234);
			String cardinalityString = statementPattern.toHumanReadableNumber(statementPattern.getResultSizeEstimate());
			assertEquals("1.2K", cardinalityString);
		}

		{
			StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
			statementPattern.setResultSizeEstimate(1910000);
			String cardinalityString = statementPattern.toHumanReadableNumber(statementPattern.getResultSizeEstimate());
			assertEquals("1.9M", cardinalityString);
		}

		{
			StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
			statementPattern.setResultSizeEstimate(1990000);
			String cardinalityString = statementPattern.toHumanReadableNumber(statementPattern.getResultSizeEstimate());
			assertEquals("2.0M", cardinalityString);
		}

		{
			StatementPattern statementPattern = new StatementPattern(new Var("s"), new Var("p"), new Var("o"));
			statementPattern.setResultSizeEstimate(912000);
			String cardinalityString = statementPattern.toHumanReadableNumber(statementPattern.getResultSizeEstimate());
			assertEquals("912.0K", cardinalityString);
		}

	}
}
