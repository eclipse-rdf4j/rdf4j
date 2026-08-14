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

import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.Exists;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.Not;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * Hoists (NOT) EXISTS and variable-to-variable comparison filters out of inner-join chains so the packed cascades
 * planner sees the wrapped statement patterns as free join factors instead of one opaque filter atom.
 *
 * A filter sitting between two joins ({@code Join(X, Filter(cond, Join(A, B)))}) is a hard region boundary for the
 * packed join enumerator: the inner join is planned in isolation (unbound), then nested-looped per outer row. Hoisting
 * the filter above its join parents ({@code Filter(cond, Join(X, Join(A, B)))}) is binding-safe for inner joins (a
 * filter above a join sees a superset of the bindings it saw below, and inner joins project nothing away) and lets the
 * enumerator order all patterns in one region. Only conditions that cannot become small-literal lookup anchors are
 * hoisted — EXISTS/NOT EXISTS and var-var comparisons — so selective constant filters keep their placement.
 */
final class LmdbFilterHoistOptimizer implements QueryOptimizer {

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		Set<String> anchorNames = bindingSetAssignmentNames(tupleExpr);
		tupleExpr.visit(new AbstractSimpleQueryModelVisitor<RuntimeException>() {

			@Override
			public void meet(Filter filter) {
				super.meet(filter);
				if (filter.isVariableScopeChange() || !isHoistableCondition(filter.getCondition())) {
					return;
				}
				// Vars the condition references (including inside EXISTS subqueries). Hoisting must
				// never make one of them newly visible: a producer that occurs outside the filter's
				// group must not retroactively correlate the condition.
				Set<String> conditionVars = VarNameCollector.process(filter.getCondition());
				// The packed rewrite rules (trivial-bind-alias, eligibility-union-exists, ...) match
				// alias filters in place; relocating them disables those rewrites.
				if (referencesExtensionAlias(filter.getArg(), conditionVars)) {
					return;
				}
				while (filter.getParentNode()instanceof Join parentJoin && !parentJoin.isVariableScopeChange()) {
					TupleExpr otherArg = parentJoin.getLeftArg() == filter
							? parentJoin.getRightArg()
							: parentJoin.getLeftArg();
					if (bindsNewConditionVar(otherArg, conditionVars, filter.getArg())
							|| containsBindingSetAssignment(otherArg)
							|| containsBindingSetAssignment(filter.getArg())
							|| bindsAnyName(otherArg, anchorNames)) {
						// Merging a VALUES anchor into a larger region can strand disconnected
						// anchors in a connected-prefix-only enumeration where they can no longer
						// pair up before a bridging pattern; leave anchor-bearing chains untouched.
						// Likewise, swallowing a pattern that shares a variable with an anchor
						// elsewhere in the query would wall that pattern off from its anchor behind
						// the filter's region boundary, turning the anchor into a cross-join driver
						// that re-executes the region once per anchor row.
						break;
					}
					parentJoin.replaceWith(filter);
					parentJoin.setLeftArg(otherArg);
					parentJoin.setRightArg(filter.getArg());
					filter.setArg(parentJoin);
				}
			}
		});
	}

	private static Set<String> bindingSetAssignmentNames(TupleExpr tupleExpr) {
		Set<String> names = new java.util.HashSet<>();
		tupleExpr.visit(new AbstractSimpleQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(org.eclipse.rdf4j.query.algebra.BindingSetAssignment assignment) {
				names.addAll(assignment.getAssuredBindingNames());
			}
		});
		return names;
	}

	private static boolean bindsAnyName(TupleExpr tupleExpr, Set<String> names) {
		if (names.isEmpty()) {
			return false;
		}
		for (String name : tupleExpr.getBindingNames()) {
			if (names.contains(name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsBindingSetAssignment(TupleExpr tupleExpr) {
		boolean[] contains = { false };
		tupleExpr.visit(new AbstractSimpleQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(org.eclipse.rdf4j.query.algebra.BindingSetAssignment assignment) {
				contains[0] = true;
			}
		});
		return contains[0];
	}

	private static boolean referencesExtensionAlias(TupleExpr filterArg, Set<String> conditionVars) {
		boolean[] references = { false };
		filterArg.visit(new AbstractSimpleQueryModelVisitor<RuntimeException>() {
			@Override
			public void meet(org.eclipse.rdf4j.query.algebra.Extension extension) {
				for (org.eclipse.rdf4j.query.algebra.ExtensionElem element : extension.getElements()) {
					if (conditionVars.contains(element.getName())) {
						references[0] = true;
						return;
					}
				}
				super.meet(extension);
			}
		});
		return references[0];
	}

	private static boolean bindsNewConditionVar(TupleExpr otherArg, Set<String> conditionVars,
			TupleExpr filterArg) {
		if (conditionVars.isEmpty()) {
			return false;
		}
		Set<String> filterArgBindings = filterArg.getBindingNames();
		for (String name : otherArg.getBindingNames()) {
			if (conditionVars.contains(name) && !filterArgBindings.contains(name)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isHoistableCondition(ValueExpr condition) {
		if (condition instanceof Exists) {
			return true;
		}
		if (condition instanceof Not not) {
			return isHoistableCondition(not.getArg());
		}
		if (condition instanceof And and) {
			return isHoistableCondition(and.getLeftArg()) && isHoistableCondition(and.getRightArg());
		}
		if (condition instanceof Compare compare) {
			return compare.getLeftArg()instanceof Var left && !left.hasValue()
					&& compare.getRightArg()instanceof Var right && !right.hasValue();
		}
		return false;
	}
}
