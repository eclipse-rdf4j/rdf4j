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
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.And;
import org.eclipse.rdf4j.query.algebra.BinaryValueOperator;
import org.eclipse.rdf4j.query.algebra.Bound;
import org.eclipse.rdf4j.query.algebra.FunctionCall;
import org.eclipse.rdf4j.query.algebra.If;
import org.eclipse.rdf4j.query.algebra.Or;
import org.eclipse.rdf4j.query.algebra.ProjectionElemList;
import org.eclipse.rdf4j.query.algebra.Regex;
import org.eclipse.rdf4j.query.algebra.UnaryValueOperator;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.ValueExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.function.FunctionRegistry;
import org.eclipse.rdf4j.query.algebra.evaluation.function.numeric.Rand;
import org.eclipse.rdf4j.query.algebra.evaluation.function.rdfterm.STRUUID;
import org.eclipse.rdf4j.query.algebra.evaluation.function.rdfterm.UUID;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A query optimizer that optimizes constant value expressions.
 *
 * @author James Leigh
 * @author Arjohn Kampman
 * 
 * @deprecated since 4.1.0. Use {@link org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ConstantOptimizer} instead.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
public class ConstantOptimizer extends org.eclipse.rdf4j.query.algebra.evaluation.optimizer.ConstantOptimizer
		implements QueryOptimizer {

	@Deprecated(forRemoval = true, since = "4.1.0")
	protected static final Logger logger = LoggerFactory.getLogger(ConstantOptimizer.class);

	@Deprecated(forRemoval = true, since = "4.1.0")
	protected final EvaluationStrategy strategy;

	public ConstantOptimizer(EvaluationStrategy strategy) {
		super(strategy);
		this.strategy = null;
	}

	@Deprecated(forRemoval = true, since = "4.1.0")
	protected class ConstantVisitor extends VarNameCollector {

		final List<ProjectionElemList> projElemLists = new ArrayList<>();

		@Override
		public void meet(ProjectionElemList projElems) {
			super.meet(projElems);
			projElemLists.add(projElems);
		}

		@Override
		public void meet(Or or) {
			or.visitChildren(this);

			try {
				if (isConstant(or.getLeftArg()) && isConstant(or.getRightArg())) {
					boolean value = strategy.isTrue(or, EmptyBindingSet.getInstance());
					or.replaceWith(new ValueConstant(BooleanLiteral.valueOf(value)));
				} else if (isConstant(or.getLeftArg())) {
					boolean leftIsTrue = strategy.isTrue(or.getLeftArg(), EmptyBindingSet.getInstance());
					if (leftIsTrue) {
						or.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
					} else {
						or.replaceWith(or.getRightArg());
					}
				} else if (isConstant(or.getRightArg())) {
					boolean rightIsTrue = strategy.isTrue(or.getRightArg(), EmptyBindingSet.getInstance());
					if (rightIsTrue) {
						or.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
					} else {
						or.replaceWith(or.getLeftArg());
					}
				}
			} catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			} catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		@Override
		public void meet(And and) {
			and.visitChildren(this);

			try {
				if (isConstant(and.getLeftArg()) && isConstant(and.getRightArg())) {
					boolean value = strategy.isTrue(and, EmptyBindingSet.getInstance());
					and.replaceWith(new ValueConstant(BooleanLiteral.valueOf(value)));
				} else if (isConstant(and.getLeftArg())) {
					boolean leftIsTrue = strategy.isTrue(and.getLeftArg(), EmptyBindingSet.getInstance());
					if (leftIsTrue) {
						and.replaceWith(and.getRightArg());
					} else {
						and.replaceWith(new ValueConstant(BooleanLiteral.FALSE));
					}
				} else if (isConstant(and.getRightArg())) {
					boolean rightIsTrue = strategy.isTrue(and.getRightArg(), EmptyBindingSet.getInstance());
					if (rightIsTrue) {
						and.replaceWith(and.getLeftArg());
					} else {
						and.replaceWith(new ValueConstant(BooleanLiteral.FALSE));
					}
				}
			} catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			} catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		@Override
		protected void meetBinaryValueOperator(BinaryValueOperator binaryValueOp) {
			super.meetBinaryValueOperator(binaryValueOp);

			if (isConstant(binaryValueOp.getLeftArg()) && isConstant(binaryValueOp.getRightArg())) {
				try {
					Value value = strategy.evaluate(binaryValueOp, EmptyBindingSet.getInstance());
					binaryValueOp.replaceWith(new ValueConstant(value));
				} catch (ValueExprEvaluationException e) {
					// TODO: incompatible values types(?), remove the affected part
					// of the query tree
					logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
				} catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		@Override
		protected void meetUnaryValueOperator(UnaryValueOperator unaryValueOp) {
			super.meetUnaryValueOperator(unaryValueOp);

			if (isConstant(unaryValueOp.getArg())) {
				try {
					Value value = strategy.evaluate(unaryValueOp, EmptyBindingSet.getInstance());
					unaryValueOp.replaceWith(new ValueConstant(value));
				} catch (ValueExprEvaluationException e) {
					// TODO: incompatible values types(?), remove the affected part
					// of the query tree
					logger.debug("Failed to evaluate UnaryValueOperator with a constant argument", e);
				} catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		@Override
		public void meet(FunctionCall functionCall) {
			super.meet(functionCall);

			List<ValueExpr> args = functionCall.getArgs();

			if (args.isEmpty()) {
				/*
				 * SPARQL has two types of zero-arg function. One are proper 'constant' functions like NOW() which
				 * generate a single value for the entire query and which can be safely optimized to a constant. Other
				 * functions, like RAND(), UUID() and STRUUID(), are a special case: they are expected to yield a new
				 * value on every call, and can therefore not be replaced by a constant.
				 */
				if (!isConstantZeroArgFunction(functionCall)) {
					return;
				}
			} else {
				for (ValueExpr arg : args) {
					if (!isConstant(arg)) {
						return;
					}
				}
			}

			// All arguments are constant

			try {
				Value value = strategy.evaluate(functionCall, EmptyBindingSet.getInstance());
				functionCall.replaceWith(new ValueConstant(value));
			} catch (ValueExprEvaluationException e) {
				// TODO: incompatible values types(?), remove the affected part of
				// the query tree
				logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
			} catch (QueryEvaluationException e) {
				logger.error("Query evaluation exception caught", e);
			}
		}

		/**
		 * Determines if the provided zero-arg function is a function that should return a constant value for the entire
		 * query execution (e.g NOW()), or if it should generate a new value for every call (e.g. RAND()).
		 *
		 * @param functionCall a zero-arg function call.
		 * @return <code>true<code> iff the provided function returns a constant value for the query execution, <code>false</code>
		 *         otherwise.
		 */
		private boolean isConstantZeroArgFunction(FunctionCall functionCall) {
			Function function = FunctionRegistry.getInstance()
					.get(functionCall.getURI())
					.orElseThrow(() -> new QueryEvaluationException(
							"Unable to find function with the URI: " + functionCall.getURI()));

			// we treat constant functions as the 'regular case' and make
			// exceptions for specific SPARQL built-in functions that require
			// different treatment.
			if (function instanceof Rand || function instanceof UUID || function instanceof STRUUID) {
				return false;
			}

			return true;
		}

		@Override
		public void meet(Bound bound) {
			super.meet(bound);

			if (bound.getArg().hasValue()) {
				// variable is always bound
				bound.replaceWith(new ValueConstant(BooleanLiteral.TRUE));
			}
		}

		@Override
		public void meet(If node) {
			super.meet(node);

			if (isConstant(node.getCondition())) {
				try {
					if (strategy.isTrue(node.getCondition(), EmptyBindingSet.getInstance())) {
						node.replaceWith(node.getResult());
					} else {
						node.replaceWith(node.getAlternative());
					}
				} catch (ValueExprEvaluationException e) {
					logger.debug("Failed to evaluate UnaryValueOperator with a constant argument", e);
				} catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		/**
		 * Override meetBinaryValueOperator
		 */
		@Override
		public void meet(Regex node) {
			super.meetNode(node);

			if (isConstant(node.getArg()) && isConstant(node.getPatternArg()) && isConstant(node.getFlagsArg())) {
				try {
					Value value = strategy.evaluate(node, EmptyBindingSet.getInstance());
					node.replaceWith(new ValueConstant(value));
				} catch (ValueExprEvaluationException e) {
					logger.debug("Failed to evaluate BinaryValueOperator with two constant arguments", e);
				} catch (QueryEvaluationException e) {
					logger.error("Query evaluation exception caught", e);
				}
			}
		}

		private boolean isConstant(ValueExpr expr) {
			return expr instanceof ValueConstant || expr instanceof Var && ((Var) expr).hasValue();
		}
	}

	@Deprecated(forRemoval = true, since = "4.1.0")
	protected class VarNameCollector extends AbstractQueryModelVisitor<RuntimeException> {

		final Set<String> varNames = new HashSet<>();

		@Override
		public void meet(Var var) {
			if (!var.isAnonymous()) {
				varNames.add(var.getName());
			}
		}
	}
}
