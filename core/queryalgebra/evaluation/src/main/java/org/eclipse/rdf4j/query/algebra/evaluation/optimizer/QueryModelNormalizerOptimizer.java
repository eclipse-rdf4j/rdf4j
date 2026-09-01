/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.UnaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractSimpleQueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.VarNameCollector;

/**
 * A query optimizer that (partially) normalizes query models to a canonical form. Note: this implementation does not
 * yet cover all query node types.
 *
 * @author Arjohn Kampman
 */
public class QueryModelNormalizerOptimizer extends AbstractSimpleQueryModelVisitor<RuntimeException>
		implements QueryOptimizer {

	public QueryModelNormalizerOptimizer() {
		super(false);
	}

	@Override
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(this);
	}

	@Override
	public void meet(Join join) {
		super.meet(join);

		TupleExpr leftArg = join.getLeftArg();
		TupleExpr rightArg = join.getRightArg();

		if (leftArg instanceof EmptySet || rightArg instanceof EmptySet) {
			join.replaceWith(new EmptySet());
		} else if (leftArg instanceof SingletonSet) {
			join.replaceWith(rightArg);
		} else if (rightArg instanceof SingletonSet) {
			join.replaceWith(leftArg);
		} else if (leftArg instanceof Union union) {
			// sort unions above joins
			Join leftJoin = new Join(union.getLeftArg(), rightArg.clone());
			Join rightJoin = new Join(union.getRightArg(), rightArg.clone());
			Union newUnion = new Union(leftJoin, rightJoin);
			newUnion.setVariableScopeChange(union.isVariableScopeChange());
			join.replaceWith(newUnion);
			newUnion.visit(this);
		} else if (rightArg instanceof Union union) {
			// sort unions above joins
			Join leftJoin = new Join(leftArg.clone(), union.getLeftArg());
			Join rightJoin = new Join(leftArg.clone(), union.getRightArg());
			Union newUnion = new Union(leftJoin, rightJoin);
			newUnion.setVariableScopeChange(union.isVariableScopeChange());
			join.replaceWith(newUnion);
			newUnion.visit(this);
		} else if (leftArg instanceof LeftJoin leftJoin && isWellDesigned((LeftJoin) leftArg)) {
			// sort left join above normal joins
			join.replaceWith(leftJoin);
			join.setLeftArg(leftJoin.getLeftArg());
			leftJoin.setLeftArg(join);
			leftJoin.visit(this);
		} else if (rightArg instanceof LeftJoin leftJoin && isWellDesigned((LeftJoin) rightArg)) {
			// sort left join above normal joins
			join.replaceWith(leftJoin);
			join.setRightArg(leftJoin.getLeftArg());
			leftJoin.setLeftArg(join);
			leftJoin.visit(this);
		} else if (rightArg instanceof Extension extension && leftArg instanceof BindingSetAssignment assignment
				&& canPushAssignmentThroughScopedExtension(assignment, extension)) {
			// A scope-changed Extension forces a hash join whose build side is evaluated fully
			// unbound. A constant VALUES relation can be pushed inside the scope when the BIND
			// expressions cannot observe its bindings, turning the unbound evaluation into bound
			// probes.
			join.replaceWith(extension);
			join.setRightArg(extension.getArg());
			extension.setArg(join);
			extension.visit(this);
		} else if (leftArg instanceof Extension extension && rightArg instanceof BindingSetAssignment assignment
				&& canPushAssignmentThroughScopedExtension(assignment, extension)) {
			join.replaceWith(extension);
			join.setLeftArg(extension.getArg());
			extension.setArg(join);
			extension.visit(this);
		}
	}

	/**
	 * A constant VALUES relation may move through a variable-scope-changing Extension when (1) it needs no input
	 * bindings, (2) its binding names are disjoint from the names the Extension assigns, (3) every BIND expression is
	 * insulated from the pushed bindings, and (4) the Extension argument is plain join/pattern/filter algebra whose
	 * bound-join evaluation is equivalent to unbound evaluation plus a join.
	 */
	private static boolean canPushAssignmentThroughScopedExtension(BindingSetAssignment assignment,
			Extension extension) {
		if (!extension.isVariableScopeChange() || assignment.isVariableScopeChange()) {
			return false;
		}
		Set<String> assignmentNames = assignment.getBindingNames();
		if (assignmentNames.isEmpty() || !extension.getArg().getBindingNames().containsAll(assignmentNames)) {
			return false;
		}
		for (ExtensionElem element : extension.getElements()) {
			if (assignmentNames.contains(element.getName())) {
				return false;
			}
			Set<String> expressionNames = new HashSet<>(VarNameCollector.process(element.getExpr()));
			expressionNames.retainAll(assignmentNames);
			if (!expressionNames.isEmpty()) {
				return false;
			}
		}
		return isPlainJoinAlgebra(extension.getArg());
	}

	private static boolean isPlainJoinAlgebra(TupleExpr tupleExpr) {
		if (tupleExpr instanceof org.eclipse.rdf4j.query.algebra.StatementPattern) {
			return true;
		}
		if (tupleExpr instanceof BindingSetAssignment assignmentArg) {
			return !assignmentArg.isVariableScopeChange();
		}
		if (tupleExpr instanceof Join joinArg && !joinArg.isVariableScopeChange()) {
			return isPlainJoinAlgebra(joinArg.getLeftArg()) && isPlainJoinAlgebra(joinArg.getRightArg());
		}
		if (tupleExpr instanceof Filter filterArg && !filterArg.isVariableScopeChange()) {
			return !(filterArg.getCondition() instanceof org.eclipse.rdf4j.query.algebra.SubQueryValueOperator)
					&& isPlainJoinAlgebra(filterArg.getArg());
		}
		return false;
	}

	@Override
	public void meet(LeftJoin leftJoin) {
		super.meet(leftJoin);

		TupleExpr leftArg = leftJoin.getLeftArg();
		TupleExpr rightArg = leftJoin.getRightArg();
		ValueExpr condition = leftJoin.getCondition();

		if (leftArg instanceof EmptySet) {
			leftJoin.replaceWith(leftArg);
		} else if (rightArg instanceof EmptySet) {
			leftJoin.replaceWith(leftArg);
		} else if (rightArg instanceof SingletonSet) {
			leftJoin.replaceWith(leftArg);
		} else if (condition instanceof ValueConstant) {
			boolean conditionValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(((ValueConstant) condition).getValue())
					.orElse(false);

			if (!conditionValue) {
				// Constraint is always false
				leftJoin.replaceWith(leftArg);
			} else {
				leftJoin.setCondition(null);
			}
		}
	}

	@Override
	public void meet(Union union) {
		super.meet(union);

		TupleExpr leftArg = union.getLeftArg();
		TupleExpr rightArg = union.getRightArg();

		if (leftArg instanceof EmptySet) {
			union.replaceWith(rightArg);
		} else if (rightArg instanceof EmptySet) {
			union.replaceWith(leftArg);
		}
	}

	@Override
	public void meet(Difference difference) {
		super.meet(difference);

		TupleExpr leftArg = difference.getLeftArg();
		TupleExpr rightArg = difference.getRightArg();

		if (leftArg instanceof EmptySet) {
			difference.replaceWith(leftArg);
		} else if (rightArg instanceof EmptySet) {
			difference.replaceWith(leftArg);
		}
	}

	@Override
	public void meet(Intersection intersection) {
		super.meet(intersection);

		TupleExpr leftArg = intersection.getLeftArg();
		TupleExpr rightArg = intersection.getRightArg();

		if (leftArg instanceof EmptySet || rightArg instanceof EmptySet) {
			intersection.replaceWith(new EmptySet());
		}
	}

	@Override
	protected void meetUnaryTupleOperator(UnaryTupleOperator node) {
		super.meetUnaryTupleOperator(node);

		if (node.getArg() instanceof EmptySet) {
			node.replaceWith(node.getArg());
		}
	}

	@Override
	public void meet(Filter node) {
		super.meet(node);

		TupleExpr arg = node.getArg();
		ValueExpr condition = node.getCondition();

		if (arg instanceof EmptySet) {
			// see #meetUnaryTupleOperator
		} else if (condition instanceof ValueConstant) {
			boolean conditionValue = QueryEvaluationUtility
					.getEffectiveBooleanValue(((ValueConstant) condition).getValue())
					.orElse(false);

			if (!conditionValue) {
				// Constraint is always false
				node.replaceWith(new EmptySet());
			} else {
				node.replaceWith(arg);
			}
		}
	}

	@Override
	public void meet(Or or) {
		super.meet(or);

		if (or.getLeftArg().equals(or.getRightArg())) {
			or.replaceWith(or.getLeftArg());
		}
	}

	@Override
	public void meet(And and) {
		super.meet(and);

		if (and.getLeftArg().equals(and.getRightArg())) {
			and.replaceWith(and.getLeftArg());
		}
	}

	/**
	 * Checks whether the left join is "well designed" as defined in section 4.2 of "Semantics and Complexity of
	 * SPARQL", 2006, Jorge Pérez et al.
	 */
	private boolean isWellDesigned(LeftJoin leftJoin) {
		VarNameCollector optionalVarCollector = new VarNameCollector();
		leftJoin.getRightArg().visit(optionalVarCollector);
		if (leftJoin.hasCondition()) {
			leftJoin.getCondition().visit(optionalVarCollector);
		}

		Set<String> leftBindingNames = leftJoin.getLeftArg().getBindingNames();
		Set<String> problemVars = retainAll(optionalVarCollector.getVarNames(), leftBindingNames);

		if (problemVars.isEmpty()) {
			return true;
		}

		return checkAgainstParent(leftJoin, problemVars);
	}

	private Set<String> retainAll(Set<String> problemVars, Set<String> leftBindingNames) {
		if (!leftBindingNames.isEmpty() && !problemVars.isEmpty()) {
			if (leftBindingNames.size() > problemVars.size()) {
				for (String problemVar : problemVars) {
					if (leftBindingNames.contains(problemVar)) {
						HashSet<String> ret = new HashSet<>(problemVars);
						ret.removeAll(leftBindingNames);
						return ret;
					}
				}
			} else {
				for (String leftBindingName : leftBindingNames) {
					if (problemVars.contains(leftBindingName)) {
						HashSet<String> ret = new HashSet<>(problemVars);
						ret.removeAll(leftBindingNames);
						return ret;
					}
				}
			}
		}
		return problemVars;
	}

	private boolean checkAgainstParent(LeftJoin leftJoin, Set<String> problemVars) {
		// If any of the problematic variables are bound in the parent
		// expression then the left join is not well designed
		BindingCollector bindingCollector = new BindingCollector();
		QueryModelNode node = leftJoin;
		QueryModelNode parent;
		while ((parent = node.getParentNode()) != null) {
			bindingCollector.setNodeToIgnore(node);
			parent.visitChildren(bindingCollector);
			node = parent;
		}

		Set<String> bindingNames = bindingCollector.getBindingNames();

		for (String problemVar : problemVars) {
			if (bindingNames.contains(problemVar)) {
				return false;
			}
		}

		return true;
	}

	private static class BindingCollector extends AbstractQueryModelVisitor<RuntimeException> {

		private QueryModelNode nodeToIgnore;

		private final Set<String> bindingNames = new HashSet<>();

		public void setNodeToIgnore(QueryModelNode node) {
			this.nodeToIgnore = node;
		}

		public Set<String> getBindingNames() {
			return bindingNames;
		}

		@Override
		protected void meetNode(QueryModelNode node) {
			if (node instanceof TupleExpr tupleExpr && node != nodeToIgnore) {
				bindingNames.addAll(tupleExpr.getBindingNames());
			}
		}
	}
}
