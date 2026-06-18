/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

/**
 * Lateral join iterator.
 * <p>
 * This iterator produces results by evaluating the right-hand side for each solution mapping from the left-hand side.
 * Note that this is similar to a join but without any join condition.
 *
 */
public class LateralIterator extends LookAheadIteration<BindingSet> {

	@FunctionalInterface
	public interface RightEvaluator {
		CloseableIteration<BindingSet> evaluate(BindingSet leftBindings);
	}

	private final CloseableIteration<BindingSet> leftIter;

	private CloseableIteration<BindingSet> rightIter;

	private final RightEvaluator rightEvaluator;

	private final Set<String> rightInputBindingNames;

	private final BindingSet originalBindings;

	private BindingSet currentLeftBindings;

	public LateralIterator(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, Set<String> rightInputBindingNames)
			throws QueryEvaluationException {
		leftIter = leftPrepared.evaluate(bindings);
		this.rightInputBindingNames = rightInputBindingNames;
		this.originalBindings = bindings;
		this.rightEvaluator = leftBindings -> preparedRight.evaluate(filterRightInput(leftBindings));
	}

	private LateralIterator(CloseableIteration<BindingSet> leftIter, QueryEvaluationStep preparedRight,
			Set<String> rightInputBindingNames, BindingSet originalBindings) throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.rightInputBindingNames = rightInputBindingNames;
		this.originalBindings = originalBindings;
		this.rightEvaluator = leftBindings -> preparedRight.evaluate(filterRightInput(leftBindings));
	}

	private LateralIterator(CloseableIteration<BindingSet> leftIter, RightEvaluator rightEvaluator)
			throws QueryEvaluationException {
		this.leftIter = leftIter;
		this.rightEvaluator = rightEvaluator;
		this.rightInputBindingNames = Set.of();
		this.originalBindings = EmptyBindingSet.getInstance();
	}

	public static CloseableIteration<BindingSet> getInstance(QueryEvaluationStep leftPrepared,
			QueryEvaluationStep preparedRight, BindingSet bindings, Set<String> rightInputBindingNames) {
		CloseableIteration<BindingSet> leftIter = leftPrepared.evaluate(bindings);
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		return new LateralIterator(leftIter, preparedRight, rightInputBindingNames, bindings);
	}

	public static CloseableIteration<BindingSet> getInstance(CloseableIteration<BindingSet> leftIter,
			QueryEvaluationStep preparedRight, Set<String> rightInputBindingNames) {
		return getInstance(leftIter, preparedRight, rightInputBindingNames, EmptyBindingSet.getInstance());
	}

	public static CloseableIteration<BindingSet> getInstance(CloseableIteration<BindingSet> leftIter,
			QueryEvaluationStep preparedRight, Set<String> rightInputBindingNames, BindingSet originalBindings) {
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		return new LateralIterator(leftIter, preparedRight, rightInputBindingNames, originalBindings);
	}

	public static CloseableIteration<BindingSet> getInstance(CloseableIteration<BindingSet> leftIter,
			RightEvaluator rightEvaluator) {
		if (leftIter == QueryEvaluationStep.EMPTY_ITERATION) {
			return leftIter;
		}

		return new LateralIterator(leftIter, rightEvaluator);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		while (rightIter != null) {
			while (rightIter.hasNext()) {
				BindingSet joined = merge(currentLeftBindings, rightIter.next());
				if (joined != null) {
					return joined;
				}
			}
			rightIter.close();
			rightIter = null;
		}

		while (leftIter.hasNext()) {
			currentLeftBindings = leftIter.next();
			rightIter = rightEvaluator.evaluate(currentLeftBindings);
			while (rightIter.hasNext()) {
				BindingSet joined = merge(currentLeftBindings, rightIter.next());
				if (joined != null) {
					return joined;
				}
			}
			if (rightIter != null) {
				rightIter.close();
				rightIter = null;
			}
		}

		return null;
	}

	private BindingSet filterRightInput(BindingSet bindings) {
		if (rightInputBindingNames.isEmpty()) {
			return originalBindings.isEmpty() ? EmptyBindingSet.getInstance() : originalBindings;
		}

		if (originalBindings.isEmpty()) {
			return filterLateralInputBindings(bindings);
		}

		QueryBindingSet filtered = new QueryBindingSet(originalBindings.size() + rightInputBindingNames.size());
		filtered.addAll(originalBindings);
		for (String bindingName : rightInputBindingNames) {
			Binding binding = bindings.getBinding(bindingName);
			if (binding != null && !filtered.hasBinding(bindingName)) {
				filtered.addBinding(binding);
			}
		}
		return filtered;
	}

	private BindingSet filterLateralInputBindings(BindingSet bindings) {
		boolean allBindingsAllowed = true;
		for (String bindingName : bindings.getBindingNames()) {
			if (!rightInputBindingNames.contains(bindingName)) {
				allBindingsAllowed = false;
				break;
			}
		}
		if (allBindingsAllowed) {
			return bindings;
		}

		QueryBindingSet filtered = new QueryBindingSet(Math.min(bindings.size(), rightInputBindingNames.size()));
		for (String bindingName : rightInputBindingNames) {
			Binding binding = bindings.getBinding(bindingName);
			if (binding != null) {
				filtered.addBinding(binding);
			}
		}
		return filtered;
	}

	private static BindingSet merge(BindingSet leftBindings, BindingSet rightBindings) {
		if (!leftBindings.isCompatible(rightBindings)) {
			return null;
		}

		QueryBindingSet merged = new QueryBindingSet(leftBindings.size() + rightBindings.size());
		merged.addAll(leftBindings);
		for (Binding binding : rightBindings) {
			if (!merged.hasBinding(binding.getName())) {
				merged.addBinding(binding);
			}
		}
		return merged;
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		try {
			leftIter.close();
		} finally {
			if (rightIter != null) {
				rightIter.close();
			}
		}
	}
}
