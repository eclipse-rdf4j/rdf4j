/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleFunctionCall;

public class TupleFunctionEvaluationStatistics extends EvaluationStatistics {

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new TupleFunctionCardinalityCalculator();
	}

	protected static class TupleFunctionCardinalityCalculator extends CardinalityCalculator {

		private static final double VAR_CARDINALITY = 10;

		@Override
		protected void meetNode(QueryModelNode node) {
			if (node instanceof TupleFunctionCall) {
				cardinality = getCardinality(VAR_CARDINALITY, ((TupleFunctionCall) node).getResultVars());
			} else {
				super.meetNode(node);
			}
		}
	}
}
