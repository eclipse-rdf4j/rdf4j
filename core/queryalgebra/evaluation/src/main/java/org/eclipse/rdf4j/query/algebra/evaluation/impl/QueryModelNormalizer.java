/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.EmptySet;
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
import org.eclipse.rdf4j.query.algebra.helpers.VarNameCollector;

/**
 * A query optimizer that (partially) normalizes query models to a canonical form. Note: this implementation does not
 * yet cover all query node types.
 *
 * @author Arjohn Kampman
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class QueryModelNormalizer extends AbstractQueryModelVisitor<RuntimeException> implements QueryOptimizer {

	public QueryModelNormalizer() {
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
		} else if (leftArg instanceof Union) {
			// sort unions above joins
			Union union = (Union) leftArg;
			Join leftJoin = new Join(union.getLeftArg(), rightArg.clone());
			Join rightJoin = new Join(union.getRightArg(), rightArg.clone());
			Union newUnion = new Union(leftJoin, rightJoin);
			newUnion.setVariableScopeChange(union.isVariableScopeChange());
			join.replaceWith(newUnion);
			newUnion.visit(this);
		} else if (rightArg instanceof Union) {
			// sort unions above joins
			Union union = (Union) rightArg;
			Join leftJoin = new Join(leftArg.clone(), union.getLeftArg());
			Join rightJoin = new Join(leftArg.clone(), union.getRightArg());
			Union newUnion = new Union(leftJoin, rightJoin);
			newUnion.setVariableScopeChange(union.isVariableScopeChange());
			join.replaceWith(newUnion);
			newUnion.visit(this);
		} else if (leftArg instanceof LeftJoin && isWellDesigned(((LeftJoin) leftArg))) {
			// sort left join above normal joins
			LeftJoin leftJoin = (LeftJoin) leftArg;
			join.replaceWith(leftJoin);
			join.setLeftArg(leftJoin.getLeftArg());
			leftJoin.setLeftArg(join);
			leftJoin.visit(this);
		} else if (rightArg instanceof LeftJoin && isWellDesigned(((LeftJoin) rightArg))) {
			// sort left join above normal joins
			LeftJoin leftJoin = (LeftJoin) rightArg;
			join.replaceWith(leftJoin);
			join.setRightArg(leftJoin.getLeftArg());
			leftJoin.setLeftArg(join);
			leftJoin.visit(this);
		}
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
		} else if (leftArg instanceof SingletonSet && rightArg instanceof SingletonSet) {
			difference.replaceWith(new EmptySet());
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

		Set<String> problemVars = optionalVarCollector.getVarNames();
		problemVars.removeAll(leftJoin.getLeftArg().getBindingNames());

		if (problemVars.isEmpty()) {
			return true;
		}

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

		problemVars.retainAll(bindingCollector.getBindingNames());

		return problemVars.isEmpty();
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
			if (node instanceof TupleExpr && node != nodeToIgnore) {
				TupleExpr tupleExpr = (TupleExpr) node;
				bindingNames.addAll(tupleExpr.getBindingNames());
			}
		}
	}
}
