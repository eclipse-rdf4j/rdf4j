/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.SPARQLMinusIteration;

public class MinusQueryEvaluationStep implements QueryEvaluationStep {
	public static final String MAX_MATERIALIZED_RIGHT_ROWS_PROPERTY = "org.eclipse.rdf4j.query.algebra.evaluation.minus."
			+ "maxMaterializedRightRows";
	private static final long DEFAULT_MAX_MATERIALIZED_RIGHT_ROWS = 1_000_000L;

	private final QueryEvaluationStep leftQes;
	private final QueryEvaluationStep rightQes;
	private final long maxMaterializedRightRows;

	public MinusQueryEvaluationStep(QueryEvaluationStep leftQes, QueryEvaluationStep rightQes) {
		this(leftQes, rightQes, maxMaterializedRightRows());
	}

	MinusQueryEvaluationStep(QueryEvaluationStep leftQes, QueryEvaluationStep rightQes, long maxMaterializedRightRows) {
		this.leftQes = leftQes;
		this.rightQes = rightQes;
		this.maxMaterializedRightRows = maxMaterializedRightRows;
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		CloseableIteration<BindingSet> left = leftQes.evaluate(bindings);
		if (maxMaterializedRightRows == Long.MAX_VALUE) {
			return new SPARQLMinusIteration(left, new QueryEvaluationStep.DelayedEvaluationIteration(rightQes,
					bindings));
		}
		return new AdaptiveMinusIteration(left, rightQes, bindings, maxMaterializedRightRows);
	}

	private static long maxMaterializedRightRows() {
		String value = System.getProperty(MAX_MATERIALIZED_RIGHT_ROWS_PROPERTY);
		if (value == null || value.isBlank()) {
			return DEFAULT_MAX_MATERIALIZED_RIGHT_ROWS;
		}
		try {
			long parsed = Long.parseLong(value.trim());
			return parsed < 0L ? Long.MAX_VALUE : parsed;
		} catch (NumberFormatException ignored) {
			return DEFAULT_MAX_MATERIALIZED_RIGHT_ROWS;
		}
	}

	private static final class AdaptiveMinusIteration extends LookAheadIteration<BindingSet> {
		private final CloseableIteration<BindingSet> leftIter;
		private final QueryEvaluationStep rightQes;
		private final BindingSet parentBindings;
		private final long maxMaterializedRightRows;
		private CloseableIteration<BindingSet> materializedMinus;
		private boolean initialized;
		private boolean overflowed;

		private AdaptiveMinusIteration(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep rightQes,
				BindingSet parentBindings, long maxMaterializedRightRows) {
			this.leftIter = leftIter;
			this.rightQes = rightQes;
			this.parentBindings = parentBindings;
			this.maxMaterializedRightRows = maxMaterializedRightRows;
		}

		@Override
		protected BindingSet getNextElement() {
			initialize();
			if (!overflowed) {
				if (materializedMinus.hasNext()) {
					return materializedMinus.next();
				}
				return null;
			}
			while (leftIter.hasNext()) {
				BindingSet left = leftIter.next();
				if (!excludedByReopenedRight(left)) {
					return left;
				}
			}
			return null;
		}

		private void initialize() {
			if (initialized) {
				return;
			}
			initialized = true;
			Set<BindingSet> rightRows = new LinkedHashSet<>();
			try (CloseableIteration<BindingSet> rightIter = rightQes.evaluate(parentBindings)) {
				while (rightIter.hasNext()) {
					BindingSet right = rightIter.next();
					if (rightRows.size() >= maxMaterializedRightRows) {
						overflowed = true;
						return;
					}
					rightRows.add(right);
				}
			}
			materializedMinus = materializedMinus(leftIter, rightRows);
		}

		private SPARQLMinusIteration materializedMinus(CloseableIteration<BindingSet> left,
				Set<BindingSet> rightRows) {
			return new SPARQLMinusIteration(left, QueryEvaluationStep.EMPTY_ITERATION) {
				@Override
				protected Set<BindingSet> makeSet(CloseableIteration<BindingSet> rightArg) {
					return rightRows;
				}
			};
		}

		private boolean excludedByReopenedRight(BindingSet left) {
			try (CloseableIteration<BindingSet> rightIter = rightQes.evaluate(parentBindings)) {
				while (rightIter.hasNext()) {
					if (minusCompatible(left, rightIter.next())) {
						return true;
					}
				}
				return false;
			}
		}

		private static boolean minusCompatible(BindingSet left, BindingSet right) {
			Set<String> leftNames = left.getBindingNames();
			Set<String> rightNames = right.getBindingNames();
			boolean hasSharedBindings = false;
			if (leftNames.size() <= rightNames.size()) {
				for (String name : leftNames) {
					if (rightNames.contains(name)) {
						hasSharedBindings = true;
						break;
					}
				}
			} else {
				for (String name : rightNames) {
					if (leftNames.contains(name)) {
						hasSharedBindings = true;
						break;
					}
				}
			}
			return hasSharedBindings && right.isCompatible(left);
		}

		@Override
		protected void handleClose() {
			if (materializedMinus != null) {
				materializedMinus.close();
			} else {
				leftIter.close();
			}
		}
	}
}
