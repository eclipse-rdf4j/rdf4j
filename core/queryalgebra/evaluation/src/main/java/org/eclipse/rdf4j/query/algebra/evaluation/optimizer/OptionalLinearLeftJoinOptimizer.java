/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

/*
 * OptionalLinearLeftJoinOptimizer
 *
 * A QueryOptimizer for RDF4J that "linearizes" OPTIONAL patterns when safe,
 * by pushing the LeftJoin condition into a Filter on the right-hand side.
 *
 * This follows the spirit of Jena's TransformJoinStrategy + LeftJoinClassifier.
 * See: org.apache.jena.sparql.algebra.optimize.TransformJoinStrategy
 *      org.apache.jena.sparql.engine.main.LeftJoinClassifier
 */

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

public class OptionalLinearLeftJoinOptimizer implements QueryOptimizer {

	private final boolean debug;

	public OptionalLinearLeftJoinOptimizer() {
		this(false);
	}

	public OptionalLinearLeftJoinOptimizer(boolean debug) {
		this.debug = debug;
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		// Bottom-up rewrite: visit children first, then transform the parent.
		tupleExpr.visit(new Rewriter(debug));
	}

	/**
	 * Performs the tree rewrite for each LeftJoin.
	 */
	private static final class Rewriter extends AbstractQueryModelVisitor<RuntimeException> {
		private final boolean debug;

		Rewriter(boolean debug) {
			this.debug = debug;
		}

		@Override
		public void meet(LeftJoin node) {
			// Optimize subtrees first
			super.meet(node);

			TupleExpr left = node.getLeftArg();
			TupleExpr right = node.getRightArg();

			if (isLinear(left, right, node.getCondition(), debug)) {
				// Push LJ condition into RHS as a Filter, and clear the LJ condition.

				ValueExpr cond = node.getCondition();
				if (cond != null) {
					// Detach the condition from the LeftJoin *before* reattaching it under Filter
					// to avoid parent-pointer inconsistencies in the query model tree.
					node.setCondition(null);

					Filter pushed = new Filter(right, cond);
					// set RHS to the filtered version
					node.setRightArg(pushed);

					if (debug) {
						System.err.println("[OptionalLinearLJ] Pushed condition into RHS Filter, linearized LeftJoin.");
					}
				} else {
					if (debug) {
						System.err.println(
								"[OptionalLinearLJ] LeftJoin had no condition; left as-is but considered linear.");
					}
				}
			} else {
				if (debug) {
					System.err.println("[OptionalLinearLJ] Not linear; leaving LeftJoin unchanged.");
				}
			}
		}
	}

	// ===== Classification logic (Jena's LeftJoinClassifier cases 1-4, with an added Case 0 guard) =====

	private static boolean isLinear(TupleExpr left, TupleExpr right, ValueExpr cond, boolean debug) {
		// Visible variables on the left (conservative: all non-constant vars syntactically present)
		Set<String> leftVars = visibleVars(left);

		// Variable usage on the right (split into fixed/opt/filter/assign)
		VarUsage usage = VarUsage.analyzeRight(right);

		if (debug) {
			System.err.println("LJ Linearization check:");
		}

		// Case 0: The LeftJoin condition (if any) must be evaluable using only RHS-bound variables.
		// Otherwise, pushing it into a RHS Filter would drop access to LHS-only bindings.
		if (cond != null) {
			Set<String> condVars = VarNameCollector.process(cond);
			Set<String> rhsVisible = visibleVars(right); // required patterns + BIND targets (not mere filter refs)

			Set<String> notInRhs = new LinkedHashSet<>(condVars);
			notInRhs.removeAll(rhsVisible);

			if (debug) {
				System.err.println("  LJ cond vars      : " + condVars);
				System.err.println("  RHS visible vars  : " + rhsVisible);
				System.err.println("  Case 0 notInRhs   : " + notInRhs + " (must be empty)");
			}

			if (!notInRhs.isEmpty()) {
				if (debug) {
					System.err.println("  -> NOT linear (Case 0: cond depends on left-only or unbound vars)");
				}
				return false;
			}
		}

		// Case 1: variables that occur only in filters (not defined in RHS via patterns or BIND)
		// If present, evaluation order may matter too much; play safe.
		Set<String> filterOnly = new HashSet<>(usage.filter);
		filterOnly.removeAll(usage.fixed);
		filterOnly.removeAll(usage.opt);
		filterOnly.removeAll(usage.assignTargets);

		if (debug) {
			System.err.println("  Left visible vars : " + leftVars);
			System.err.println("  Right fixed vars  : " + usage.fixed);
			System.err.println("  Right opt vars    : " + usage.opt);
			System.err.println("  Right filter vars : " + usage.filter);
			System.err.println("  Right assign deps : " + usage.assignDeps);
			System.err.println("  Right assign tgs  : " + usage.assignTargets);
			System.err.println("  Case 1 filterOnly : " + filterOnly + " (must be empty)");
		}

		if (!filterOnly.isEmpty()) {
			if (debug) {
				System.err.println("  -> NOT linear (Case 1)");
			}
			return false;
		}

		// Case 2: A variable that is optional (nested OPTIONAL in RHS) also occurs on LHS.
		// Then linearization could break scoping.
		boolean case2 = intersects(leftVars, usage.opt);
		if (debug) {
			System.err.println("  Case 2 (left ∩ optRight)      : " + case2);
		}
		if (case2) {
			return false;
		}

		// Case 3: A variable mentioned in a filter inside RHS already exists on LHS.
		// Changing evaluation order could change semantics of that filter.
		boolean case3 = intersects(leftVars, usage.filter);
		if (debug) {
			System.err.println("  Case 3 (left ∩ filterVarsRight): " + case3);
		}
		if (case3) {
			return false;
		}

		// Case 4: BIND in RHS depends on a variable that is not introduced as fixed in RHS.
		// (I.e., BIND depends on LHS or optional variables). That’s unsafe.
		Set<String> unsafeAssignDeps = new HashSet<>(usage.assignDeps);
		unsafeAssignDeps.removeAll(usage.fixed);
		boolean case4 = !unsafeAssignDeps.isEmpty();
		if (debug) {
			System.err.println(
					"  Case 4 (assignDeps \\ fixedRight): " + unsafeAssignDeps + " -> " + (case4 ? "unsafe" : "ok"));
		}
		if (case4) {
			return false;
		}

		if (debug) {
			System.err.println("  => Linearizable");
		}
		return true;
	}

	/** Collect a conservative set of visible (non-constant) variable names in a TupleExpr. */
	private static Set<String> visibleVars(TupleExpr expr) {
		Set<String> names = new LinkedHashSet<>();
		expr.visit(new AbstractQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(StatementPattern sp) {
				add(sp.getSubjectVar());
				add(sp.getPredicateVar());
				add(sp.getObjectVar());
				add(sp.getContextVar());
				super.meet(sp);
			}

			@Override
			public void meet(Extension node) {
				// assignment targets are visible afterwards
				for (ExtensionElem el : node.getElements()) {
					if (el.getName() != null) {
						names.add(el.getName());
					}
				}
				super.meet(node);
			}

			private void add(Var v) {
				if (v != null && !v.hasValue() && v.getName() != null) {
					names.add(v.getName());
				}
			}
		});
		return names;
	}

	// ===== Right-side Var analysis =====

	/**
	 * Captures right-hand side variable usage roughly analogous to Jena VarFinder: - fixed: variables introduced by
	 * required patterns in RHS - opt : variables introduced in OPTIONAL-nested RHS (right arm of a LeftJoin, and inside
	 * Union we treat as optional) - filter: variables mentioned in Filter nodes inside RHS (not LJ condition) -
	 * assignTargets: variables created by BIND/Extension in RHS - assignDeps: variables referenced by those BIND
	 * expressions
	 */
	private static final class VarUsage {
		final Set<String> fixed = new LinkedHashSet<>();
		final Set<String> opt = new LinkedHashSet<>();
		final Set<String> filter = new LinkedHashSet<>();
		final Set<String> assignTargets = new LinkedHashSet<>();
		final Set<String> assignDeps = new LinkedHashSet<>();

		static VarUsage analyzeRight(TupleExpr right) {
			VarUsage usage = new VarUsage();
			right.visit(new RightVarUsageCollector(usage));
			return usage;
		}
	}

	/**
	 * Visitor that walks the RHS and classifies variables as fixed/opt/filter/assign. - "optionalDepth" is incremented
	 * when we are in the RIGHT arm of a LeftJoin; - "unionDepth" marks that we are in a Union branch (conservative:
	 * treat union vars as optional).
	 */
	private static final class RightVarUsageCollector extends AbstractQueryModelVisitor<RuntimeException> {
		private final VarUsage usage;
		private int optionalDepth = 0;
		private int unionDepth = 0;

		RightVarUsageCollector(VarUsage usage) {
			this.usage = usage;
		}

		private boolean inOptionalContext() {
			return optionalDepth > 0 || unionDepth > 0;
		}

		@Override
		public void meet(LeftJoin node) {
			// LEFT arm is required
			node.getLeftArg().visit(this);
			// RIGHT arm is optional
			optionalDepth++;
			try {
				node.getRightArg().visit(this);
			} finally {
				optionalDepth--;
			}
			// IMPORTANT: do NOT add LJ condition variables to "filter" here.
			// We will potentially push this condition as a Filter ourselves when safe.
		}

		@Override
		public void meet(Union node) {
			unionDepth++;
			try {
				node.getLeftArg().visit(this);
				node.getRightArg().visit(this);
			} finally {
				unionDepth--;
			}
		}

		@Override
		public void meet(Join node) {
			// required on both sides
			super.meet(node);
		}

		@Override
		public void meet(Filter node) {
			// Collect filter variables inside RHS (excludes LJ condition on purpose)
			if (node.getCondition() != null) {
				usage.filter.addAll(VarNameCollector.process(node.getCondition()));
			}
			// Continue traversal
			super.meet(node);
		}

		@Override
		public void meet(Extension node) {
			// BIND targets and deps
			for (ExtensionElem el : node.getElements()) {
				if (el.getName() != null) {
					usage.assignTargets.add(el.getName());
				}
				if (el.getExpr() != null) {
					usage.assignDeps.addAll(VarNameCollector.process(el.getExpr()));
				}
			}
			super.meet(node);
		}

		@Override
		public void meet(StatementPattern sp) {
			// Vars from required patterns are FIXED, from optional contexts are OPT
			add(sp.getSubjectVar());
			add(sp.getPredicateVar());
			add(sp.getObjectVar());
			add(sp.getContextVar());
			super.meet(sp);
		}

		private void add(Var v) {
			if (v == null || v.hasValue() || v.getName() == null) {
				return;
			}
			if (inOptionalContext()) {
				usage.opt.add(v.getName());
			} else {
				usage.fixed.add(v.getName());
			}
		}
	}

	// ===== util =====

	private static boolean intersects(Set<String> a, Set<String> b) {
		if (a.isEmpty() || b.isEmpty()) {
			return false;
		}
		// iterate smaller set
		Set<String> s = (a.size() <= b.size()) ? a : b;
		Set<String> t = (s == a) ? b : a;
		for (String x : s) {
			if (t.contains(x)) {
				return true;
			}
		}
		return false;
	}
}
