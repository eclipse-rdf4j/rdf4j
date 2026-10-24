/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SubQueryValueOperator;
import org.eclipse.rdf4j.query.algebra.Union;

/**
 * Detects operators in a re-invocable position: the right side of a non-Union binary operator, a property-path body, or
 * an EXISTS-style subquery. Such operators may be re-evaluated once per upstream binding, so per-invocation
 * observations recorded at close time are conditioned on the outer stream and do not describe the operator's standalone
 * behavior.
 */
final class LmdbReinvocablePositions {

	private LmdbReinvocablePositions() {
	}

	static final String FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY = "rdf4j.optimizer.lmdb.filterLearningReinvocablePolicy";

	/*
	 * A filter on the inner side of a nested loop exhausts its input once per outer binding, so its pass counts are
	 * conditioned on the outer stream (a correlated probe can pass ~100% of rows the outer side pre-selected). The
	 * learning keys carry no binding context, so recording those counts would poison standalone uses of the same
	 * filter. Unlike the operator-feedback path there is no completed-root aggregate to defer to — and even a final
	 * aggregate stays conditioned on the outer stream — so re-invocable positions are simply not recorded ("skip"; set
	 * the policy property to "record" to restore the old behavior).
	 */
	static boolean filterOutcomeRecordable(QueryModelNode filter) {
		return "record".equalsIgnoreCase(
				System.getProperty(FILTER_LEARNING_REINVOCABLE_POLICY_PROPERTY, "skip"))
				|| !isReinvocable(filter);
	}

	static boolean isReinvocable(QueryModelNode node) {
		QueryModelNode current = node;
		QueryModelNode parent = current == null ? null : current.getParentNode();
		while (parent != null) {
			if (parent instanceof BinaryTupleOperator binary && binary.getRightArg() == current
					&& !(parent instanceof Union)) {
				return true;
			}
			if (parent instanceof ArbitraryLengthPath || parent instanceof SubQueryValueOperator) {
				return true;
			}
			current = parent;
			parent = parent.getParentNode();
		}
		return false;
	}
}
