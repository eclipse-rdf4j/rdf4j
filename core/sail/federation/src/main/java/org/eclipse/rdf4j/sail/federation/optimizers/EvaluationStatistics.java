/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.optimizers;

import java.util.List;

import org.eclipse.rdf4j.query.algebra.BinaryTupleOperator;
import org.eclipse.rdf4j.query.algebra.EmptySet;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.SingletonSet;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.sail.federation.algebra.NaryJoin;

/**
 * Supplies various query model statistics to the query engine/optimizer.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class EvaluationStatistics {

	protected CardinalityCalculator calculator;

	private final Object lock = new Object();

	public double getCardinality(TupleExpr expr) {
		synchronized (lock) {
			if (calculator == null) {
				calculator = createCardinalityCalculator();
			}
			expr.visit(calculator);
			return calculator.getCardinality();
		}
	}

	protected CardinalityCalculator createCardinalityCalculator() {
		return new CardinalityCalculator();
	}

	/*-----------------------------------*
	 * Inner class CardinalityCalculator *
	 *-----------------------------------*/

	protected static class CardinalityCalculator extends AbstractQueryModelVisitor<RuntimeException> {

		protected double cardinality;

		public double getCardinality() {
			return cardinality;
		}

		@Override
		public void meet(EmptySet node) {
			cardinality = 0;
		}

		@Override
		public void meet(SingletonSet node) {
			cardinality = 1;
		}

		@Override
		public void meet(StatementPattern pattern) {
			cardinality = getCardinality(pattern);
		}

		protected double getCardinality(StatementPattern pattern) {
			List<Var> vars = pattern.getVarList();
			int constantVarCount = countConstantVars(vars);
			double unboundVarFactor = (double)(vars.size() - constantVarCount) / vars.size();
			return Math.pow(1000.0, unboundVarFactor);
		}

		protected int countConstantVars(Iterable<Var> vars) {
			int constantVarCount = 0;

			for (Var var : vars) {
				if (var.hasValue()) {
					constantVarCount++;
				}
			}

			return constantVarCount;
		}

		@Override
		public void meetOther(QueryModelNode node) {
			if (node instanceof NaryJoin) {
				meetMultiJoin((NaryJoin)node);
			}
			else {
				super.meetOther(node);
			}
		}

		public void meetMultiJoin(NaryJoin node) {
			double cost = 1;
			for (TupleExpr arg : node.getArgs()) {
				arg.visit(this);
				cost *= this.cardinality;
			}
			cardinality = cost;
		}

		@Override
		public void meet(Join node) {
			double cost = 1;
			for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(), // NOPMD
					node.getRightArg() })
			{
				arg.visit(this);
				cost *= this.cardinality;
			}
			cardinality = cost;
		}

		@Override
		public void meet(LeftJoin node) {
			node.getLeftArg().visit(this);
			double leftArgCost = this.cardinality;

			node.getRightArg().visit(this);
			cardinality *= leftArgCost;
		}

		@Override
		protected void meetBinaryTupleOperator(BinaryTupleOperator node) {
			double cost = 0;
			for (TupleExpr arg : new TupleExpr[] { node.getLeftArg(), // NOPMD
					node.getRightArg() })
			{
				arg.visit(this);
				cost += cardinality;
			}
			cardinality = cost;
		}
	}
}
