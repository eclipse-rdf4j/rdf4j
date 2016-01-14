/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.Difference;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Intersection;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;

/**
 * A query optimizer that prunes query model trees by removing superfluous parts
 * and/or by reducing complex parts with simpler parts.
 * 
 * @author Arjohn Kampman
 */
public class QueryModelPruner implements QueryOptimizer {

	/**
	 * Applies generally applicable optimizations: path expressions are sorted
	 * from more to less specific.
	 */
	public void optimize(TupleExpr tupleExpr, Dataset dataset, BindingSet bindings) {
		tupleExpr.visit(new TreeSanitizer());
	}

	protected class TreeSanitizer extends AbstractQueryModelVisitor<RuntimeException> {

		@Override
		public void meetOther(QueryModelNode node)
			throws RuntimeException
		{
			if (node instanceof NaryJoin) {
				meetMultiJoin((NaryJoin)node);
			}
			else {
				super.meetOther(node);
			}
		}

		public void meetMultiJoin(NaryJoin join) {
			super.meetOther(join);
			for (TupleExpr arg : join.getArgs()) {
				if (arg instanceof SingletonSet) {
					join.removeArg(arg);
				}
				else if (arg instanceof EmptySet) {
					join.replaceWith(new EmptySet()); // NOPMD
					return;
				}
			}
			if (join.getNumberOfArguments() == 1) {
				join.replaceWith(join.getArg(0));
			}
		}

		@Override
		public void meet(Join join) {
			super.meet(join);

			TupleExpr leftArg = join.getLeftArg();
			TupleExpr rightArg = join.getRightArg();

			if (leftArg instanceof EmptySet || rightArg instanceof EmptySet) {
				join.replaceWith(new EmptySet());
			}
			else if (leftArg instanceof SingletonSet) {
				join.replaceWith(rightArg);
			}
			else if (rightArg instanceof SingletonSet) {
				join.replaceWith(leftArg);
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
			}
			else if (rightArg instanceof EmptySet) {
				leftJoin.replaceWith(leftArg);
			}
			else if (rightArg instanceof SingletonSet) {
				leftJoin.replaceWith(leftArg);
			}
			else if (condition instanceof ValueConstant) {
				boolean conditionValue;
				try {
					conditionValue = QueryEvaluationUtil.getEffectiveBooleanValue(((ValueConstant)condition).getValue());
				}
				catch (ValueExprEvaluationException e) {
					conditionValue = false;
				}
				if (conditionValue) {
					leftJoin.setCondition(null);
				}
				else {
					// Constraint is always false
					leftJoin.replaceWith(leftArg);
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
			}
			else if (rightArg instanceof EmptySet) {
				union.replaceWith(leftArg);
			}
			else if (leftArg instanceof SingletonSet && rightArg instanceof SingletonSet) {
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
			}
			else if (rightArg instanceof EmptySet) {
				difference.replaceWith(leftArg);
			}
			else if (leftArg instanceof SingletonSet && rightArg instanceof SingletonSet) {
				difference.replaceWith(new EmptySet());
			}
		}

		@Override
		public void meet(Intersection intersection) {
			super.meet(intersection);
			TupleExpr leftArg = intersection.getLeftArg();
			TupleExpr rightArg = intersection.getRightArg();
			if (leftArg instanceof EmptySet || rightArg instanceof EmptySet) {
				intersection.replaceWith(leftArg);
			}
		}
	}
}