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
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.Distinct;
import org.eclipse.rdf4j.query.algebra.Lateral;
import org.eclipse.rdf4j.query.algebra.MultiProjection;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.ProjectionElem;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Reduced;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.LateralIterator;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;

public final class LateralQueryEvaluationStep implements QueryEvaluationStep {
	private final QueryEvaluationStep left;
	private final QueryEvaluationStep right;
	private final EvaluationStrategy strategy;
	private final TupleExpr rightArg;
	private final QueryEvaluationContext context;
	private final Set<String> rightInputBindingNames;

	public static QueryEvaluationStep supply(EvaluationStrategy strategy, Lateral lateral,
			QueryEvaluationContext context) {
		QueryEvaluationStep left = strategy.precompile(lateral.getLeftArg(), context);
		return new LateralQueryEvaluationStep(left, strategy, lateral.getRightArg(), context, lateral);
	}

	public LateralQueryEvaluationStep(QueryEvaluationStep left, QueryEvaluationStep right, Lateral lateral) {
		this.left = left;
		this.right = right;
		this.strategy = null;
		this.rightArg = null;
		this.context = null;
		this.rightInputBindingNames = lateral.getRightInputBindingNames();
	}

	private LateralQueryEvaluationStep(QueryEvaluationStep left, EvaluationStrategy strategy, TupleExpr rightArg,
			QueryEvaluationContext context, Lateral lateral) {
		this.left = left;
		this.strategy = strategy;
		this.rightArg = rightArg;
		this.context = context;
		this.right = strategy.precompile(rightArg, context);
		this.rightInputBindingNames = lateral.getRightInputBindingNames();
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) {
		// Evaluate left side
		CloseableIteration<BindingSet> leftResults = left.evaluate(bindings);

		if (strategy == null || rightInputBindingNames.isEmpty()) {
			return LateralIterator.getInstance(leftResults, right, rightInputBindingNames, bindings);
		}

		QueryEvaluationStep rightForBindings = prepareRight(bindings);
		return LateralIterator.getInstance(leftResults,
				leftBindings -> rightForBindings.evaluate(filterRightInput(bindings, leftBindings)));
	}

	private QueryEvaluationStep prepareRight(BindingSet originalBindings) {
		Set<String> lateralBindingNames = lateralBindingNames(originalBindings);
		if (lateralBindingNames.isEmpty() || preservesDirectSubSelectBindings(lateralBindingNames)) {
			return right;
		}

		TupleExpr scopedRight = rightArg.clone();
		scopedRight.visit(new HiddenLateralBindingRenamer(lateralBindingNames));
		return strategy.precompile(scopedRight, context);
	}

	private BindingSet filterRightInput(BindingSet originalBindings, BindingSet leftBindings) {
		if (rightInputBindingNames.isEmpty()) {
			return originalBindings.isEmpty() ? EmptyBindingSet.getInstance() : originalBindings;
		}

		if (originalBindings.isEmpty()) {
			return filterLateralInputBindings(EmptyBindingSet.getInstance(), leftBindings);
		}

		QueryBindingSet filtered = new QueryBindingSet(originalBindings.size() + rightInputBindingNames.size());
		filtered.addAll(originalBindings);
		for (String bindingName : rightInputBindingNames) {
			Binding binding = leftBindings.getBinding(bindingName);
			if (binding != null && !filtered.hasBinding(bindingName)) {
				filtered.addBinding(binding);
			}
		}
		return filtered;
	}

	private BindingSet filterLateralInputBindings(BindingSet originalBindings, BindingSet leftBindings) {
		QueryBindingSet filtered = new QueryBindingSet(Math.min(leftBindings.size(), rightInputBindingNames.size()));
		for (String bindingName : rightInputBindingNames) {
			Binding binding = originalBindings.hasBinding(bindingName) ? null : leftBindings.getBinding(bindingName);
			if (binding != null) {
				filtered.addBinding(binding);
			}
		}
		return filtered.isEmpty() ? EmptyBindingSet.getInstance() : filtered;
	}

	private Set<String> lateralBindingNames(BindingSet originalBindings) {
		if (originalBindings.isEmpty()) {
			return rightInputBindingNames;
		}

		Set<String> lateralBindingNames = new HashSet<>(rightInputBindingNames);
		lateralBindingNames.removeAll(originalBindings.getBindingNames());
		return lateralBindingNames;
	}

	private boolean preservesDirectSubSelectBindings(Set<String> lateralBindingNames) {
		TupleExpr tupleExpr = rightArg;
		while (tupleExpr instanceof Slice || tupleExpr instanceof Order || tupleExpr instanceof Distinct
				|| tupleExpr instanceof Reduced) {
			tupleExpr = ((UnaryTupleOperator) tupleExpr).getArg();
		}
		return tupleExpr instanceof Projection && !rightArg.getBindingNames().containsAll(lateralBindingNames);
	}

	private static final class HiddenLateralBindingRenamer extends AbstractSimpleQueryModelVisitor<RuntimeException> {

		private final Set<String> lateralBindingNames;
		private final Deque<Map<String, String>> replacementScopes = new ArrayDeque<>();
		private int replacementIndex;

		private HiddenLateralBindingRenamer(Set<String> lateralBindingNames) {
			super(true);
			this.lateralBindingNames = lateralBindingNames;
		}

		@Override
		public void meet(Projection node) {
			Map<String, String> replacements = hiddenProjectionSourceBindings(node.getProjectionElemList());
			visitProjectionArgument(node.getArg(), replacements);
		}

		@Override
		public void meet(MultiProjection node) {
			Map<String, String> replacements = hiddenProjectionSourceBindings(node.getProjections());
			visitProjectionArgument(node.getArg(), replacements);
		}

		@Override
		public void meet(Lateral node) {
			node.getLeftArg().visit(this);
		}

		@Override
		public void meet(Var var) {
			if (!var.hasValue()) {
				String replacementName = replacementName(var.getName());
				if (replacementName != null) {
					Var replacement = Var.of(replacementName, var.getValue(), var.isAnonymous(), var.isConstant(),
							var.isBNode());
					replacement.setVariableScopeChange(var.isVariableScopeChange());
					var.replaceWith(replacement);
				}
			}
		}

		private void visitProjectionArgument(TupleExpr arg, Map<String, String> replacements) {
			if (replacements.isEmpty()) {
				arg.visit(this);
				return;
			}

			replacementScopes.push(replacements);
			try {
				arg.visit(this);
			} finally {
				replacementScopes.pop();
			}
		}

		private Map<String, String> hiddenProjectionSourceBindings(ProjectionElemList projection) {
			Map<String, String> replacements = new HashMap<>();
			for (String bindingName : lateralBindingNames) {
				if (!containsSourceName(projection, bindingName)) {
					replacements.put(bindingName, nextReplacementName(bindingName));
				}
			}
			return replacements;
		}

		private Map<String, String> hiddenProjectionSourceBindings(List<ProjectionElemList> projections) {
			Map<String, String> replacements = new HashMap<>();
			for (String bindingName : lateralBindingNames) {
				if (!containsSourceName(projections, bindingName)) {
					replacements.put(bindingName, nextReplacementName(bindingName));
				}
			}
			return replacements;
		}

		private static boolean containsSourceName(List<ProjectionElemList> projections, String bindingName) {
			for (ProjectionElemList projection : projections) {
				if (containsSourceName(projection, bindingName)) {
					return true;
				}
			}
			return false;
		}

		private static boolean containsSourceName(ProjectionElemList projection, String bindingName) {
			for (ProjectionElem projectionElem : projection.getElements()) {
				if (projectionElem.getName().equals(bindingName)) {
					return true;
				}
			}
			return false;
		}

		private String replacementName(String name) {
			for (Map<String, String> replacementScope : replacementScopes) {
				String replacementName = replacementScope.get(name);
				if (replacementName != null) {
					return replacementName;
				}
			}
			return null;
		}

		private String nextReplacementName(String bindingName) {
			return "-lateral-hidden-" + replacementIndex++ + "-" + bindingName;
		}
	}
}
