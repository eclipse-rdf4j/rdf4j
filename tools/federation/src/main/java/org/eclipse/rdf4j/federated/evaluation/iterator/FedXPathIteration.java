/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.iterator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.algebra.FedXZeroLengthPath;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

/**
 * A iteration to evaluate property path expressions.
 *
 * @see PathIteration
 */
public class FedXPathIteration extends LookAheadIteration<BindingSet> {

	/*
	 * IMPL NOTE:
	 *
	 * This is technically almost a 1:1 copy of org.eclipse.rdf4j.query.algebra.evaluation.iterator.PathIteration.
	 * Reusing or extending PathIteration requires refactoring in its constructor initialization.
	 *
	 * The main difference is in keeping track of QueryInfo and creating a FedXZeroLengthPath in #createIteration for
	 * zero length path expressions.
	 */

	/**
	 *
	 */
	private final EvaluationStrategy strategy;

	private long currentLength;

	private CloseableIteration<BindingSet> currentIter;

	private final BindingSet bindings;

	private final Scope scope;

	private final Var startVar;

	private final Var endVar;

	private final boolean startVarFixed;

	private final boolean endVarFixed;

	private final Queue<ValuePair> valueQueue;

	private final Set<ValuePair> reportedValues;

	private final Set<ValuePair> unreportedValues;

	private final TupleExpr pathExpression;

	private final Var contextVar;

	private ValuePair currentVp;

	private static final String JOINVAR_PREFIX = "intermediate_join_";

	private final Set<String> namedIntermediateJoins = new HashSet<>();

	private final QueryInfo queryInfo;

	public FedXPathIteration(EvaluationStrategy strategy, Scope scope, Var startVar, TupleExpr pathExpression,
			Var endVar, Var contextVar, long minLength, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {

		this.strategy = strategy;
		this.scope = scope;
		this.startVar = startVar;
		this.endVar = endVar;

		this.startVarFixed = startVar.hasValue() || bindings.hasBinding(startVar.getName());
		this.endVarFixed = endVar.hasValue() || bindings.hasBinding(endVar.getName());

		this.pathExpression = pathExpression;
		this.contextVar = contextVar;

		this.currentLength = minLength;
		this.bindings = bindings;

		CollectionFactory collectionFactory = strategy.getCollectionFactory().get();
		this.reportedValues = collectionFactory.createSet();
		this.unreportedValues = collectionFactory.createSet();
		this.valueQueue = collectionFactory.createQueue();

		this.queryInfo = queryInfo;

		createIteration();

	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		again: while (true) {
			while (currentIter != null && !currentIter.hasNext()) {
				currentIter.close();
				createIteration();
				// stop condition: if the iter is an EmptyIteration
				if (currentIter == null) {
					break;
				}
			}

			while (currentIter != null && currentIter.hasNext()) {
				BindingSet potentialNextElement = currentIter.next();
				QueryBindingSet nextElement;
				// if it is not a compatible type of BindingSet
				if (potentialNextElement instanceof QueryBindingSet) {
					nextElement = (QueryBindingSet) potentialNextElement;
				} else {
					nextElement = new QueryBindingSet(potentialNextElement);
				}

				if (!startVarFixed && !endVarFixed && currentVp != null) {
					Value startValue = currentVp.getStartValue();

					if (startValue != null) {
						nextElement = new QueryBindingSet(nextElement);
						addBinding(nextElement, startVar.getName(), startValue);
					}
				}

				ValuePair vp = valuePairFromStartAndEnd(nextElement);

				if (!isCyclicPath(vp)) {

					if (reportedValues.contains(vp)) {
						// new arbitrary-length path semantics: filter out
						// duplicates
						if (currentIter.hasNext()) {
							continue;
						} else {
							// if the current iter is exhausted, we need to check
							// that no further paths of greater length still exists.
							continue again;
						}
					}

					if (startVarFixed && endVarFixed) {
						Value endValue = getVarValue(endVar, endVarFixed, nextElement);
						if (endValue.equals(vp.endValue)) {
							add(reportedValues, vp);
							if (!vp.startValue.equals(vp.endValue)) {
								addToQueue(valueQueue, vp);
							}
							if (!nextElement.hasBinding(startVar.getName())) {
								addBinding(nextElement, startVar.getName(), vp.startValue);
							}
							if (!nextElement.hasBinding(endVar.getName())) {
								addBinding(nextElement, endVar.getName(), vp.endValue);
							}
							return removeIntermediateJoinVars(nextElement);
						} else {
							if (add(unreportedValues, vp)) {
								if (!vp.startValue.equals(vp.endValue)) {
									addToQueue(valueQueue, vp);
								}
							}
							continue again;
						}
					} else {
						add(reportedValues, vp);
						if (!vp.startValue.equals(vp.endValue)) {
							addToQueue(valueQueue, vp);
						}
						if (!nextElement.hasBinding(startVar.getName())) {
							addBinding(nextElement, startVar.getName(), vp.startValue);
						}
						if (!nextElement.hasBinding(endVar.getName())) {
							addBinding(nextElement, endVar.getName(), vp.endValue);
						}
						return removeIntermediateJoinVars(nextElement);
					}
				} else {
					continue again;
				}
			}

			// if we're done, throw away the cached lists of values to avoid
			// hogging resources
			reportedValues.clear();
			unreportedValues.clear();
			valueQueue.clear();
			return null;
		}
	}

	private BindingSet removeIntermediateJoinVars(QueryBindingSet nextElement) {
		nextElement.removeAll(namedIntermediateJoins);
		return nextElement;
	}

	private ValuePair valuePairFromStartAndEnd(MutableBindingSet nextElement) {
		Value v1, v2;

		if (startVarFixed && endVarFixed && currentLength > 2) {
			v1 = getVarValue(startVar, startVarFixed, nextElement);
			v2 = nextElement.getValue("END_" + JOINVAR_PREFIX + this.hashCode());
		} else if (startVarFixed && endVarFixed && currentLength == 2) {
			v1 = getVarValue(startVar, startVarFixed, nextElement);
			v2 = nextElement.getValue(JOINVAR_PREFIX + (currentLength - 1) + "_" + this.hashCode());
		} else {
			v1 = getVarValue(startVar, startVarFixed, nextElement);
			v2 = getVarValue(endVar, endVarFixed, nextElement);
		}
		return new ValuePair(v1, v2);
	}

	private void addBinding(MutableBindingSet bs, String name, Value value) {
		bs.addBinding(name, value);
	}

	@Override
	protected void handleClose() throws QueryEvaluationException {
		if (currentIter != null)
			currentIter.close();

	}

	/**
	 * @param valueQueue2
	 * @param vp
	 */
	protected boolean addToQueue(Queue<ValuePair> valueQueue2, ValuePair vp) throws QueryEvaluationException {
		return valueQueue2.add(vp);
	}

	/**
	 * @param valueSet
	 * @param vp
	 */
	protected boolean add(Set<ValuePair> valueSet, ValuePair vp) throws QueryEvaluationException {
		return valueSet.add(vp);
	}

	private Value getVarValue(Var var, boolean fixedValue, BindingSet bindingSet) {
		Value v;
		if (fixedValue) {
			v = var.getValue();
			if (v == null) {
				v = this.bindings.getValue(var.getName());
			}
		} else {
			v = bindingSet.getValue(var.getName());
		}

		return v;
	}

	private boolean isCyclicPath(ValuePair vp) {
		if (currentLength <= 2) {
			return false;
		}

		return reportedValues.contains(vp);

	}

	private void createIteration() throws QueryEvaluationException {

		if (isUnbound(startVar, bindings) || isUnbound(endVar, bindings)) {
			// the variable must remain unbound for this solution see https://www.w3.org/TR/sparql11-query/#assignment
			currentIter = null;
		} else if (currentLength == 0L) {

			// For the federation we need to path through statement sources and query info

			// determine statement sources relevant in the current path scope
			var statementSources = new ArrayList<StatementSource>();
			if (pathExpression instanceof StatementTupleExpr) {
				statementSources.addAll(((StatementTupleExpr) pathExpression).getStatementSources());
			}

			FedXZeroLengthPath zlp = new FedXZeroLengthPath(scope, startVar.clone(), endVar.clone(),
					contextVar != null ? contextVar.clone() : null, queryInfo, statementSources);

			currentIter = this.strategy.evaluate(zlp, bindings);
			currentLength++;
		} else if (currentLength == 1) {
			TupleExpr pathExprClone = pathExpression.clone();

			if (startVarFixed && endVarFixed) {
				Var replacement = createAnonVar(JOINVAR_PREFIX + currentLength + "_" + this.hashCode());

				VarReplacer replacer = new VarReplacer(endVar, replacement, 0, false);
				pathExprClone.visit(replacer);
			}
			currentIter = this.strategy.evaluate(pathExprClone, bindings);
			currentLength++;
		} else {

			currentVp = valueQueue.poll();

			if (currentVp != null) {

				TupleExpr pathExprClone = pathExpression.clone();

				if (startVarFixed && endVarFixed) {
					Value v = currentVp.getEndValue();

					Var startReplacement = createAnonVar(JOINVAR_PREFIX + currentLength + "_" + this.hashCode(), v,
							false);
					Var endReplacement = createAnonVar("END_" + JOINVAR_PREFIX + this.hashCode(), null, false);

					VarReplacer replacer = new VarReplacer(startVar, startReplacement, 0, false);
					pathExprClone.visit(replacer);

					replacer = new VarReplacer(endVar, endReplacement, 0, false);
					pathExprClone.visit(replacer);
				} else {
					Var toBeReplaced;
					Value v;
					if (!endVarFixed) {
						toBeReplaced = startVar;
						v = currentVp.getEndValue();
					} else {
						toBeReplaced = endVar;
						v = currentVp.getStartValue();
					}

					Var replacement = createAnonVar(JOINVAR_PREFIX + currentLength + "_" + this.hashCode(), v, false);

					VarReplacer replacer = new VarReplacer(toBeReplaced, replacement, 0, false);
					pathExprClone.visit(replacer);
				}

				currentIter = this.strategy.evaluate(pathExprClone, bindings);
			} else {
				currentIter = null;
			}
			currentLength++;

		}
	}

	protected boolean isUnbound(Var var, BindingSet bindings) {
		if (var == null) {
			return false;
		} else {
			return bindings.hasBinding(var.getName()) && bindings.getValue(var.getName()) == null;
		}
	}

	protected static class ValuePair {

		private final Value startValue;

		private final Value endValue;

		public ValuePair(Value startValue, Value endValue) {
			this.startValue = startValue;
			this.endValue = endValue;
		}

		/**
		 * @return Returns the startValue.
		 */
		public Value getStartValue() {
			return startValue;
		}

		/**
		 * @return Returns the endValue.
		 */
		public Value getEndValue() {
			return endValue;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((endValue == null) ? 0 : endValue.hashCode());
			result = prime * result + ((startValue == null) ? 0 : startValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ValuePair)) {
				return false;
			}
			ValuePair other = (ValuePair) obj;
			if (endValue == null) {
				if (other.endValue != null) {
					return false;
				}
			} else if (!endValue.equals(other.endValue)) {
				return false;
			}
			if (startValue == null) {
				if (other.startValue != null) {
					return false;
				}
			} else if (!startValue.equals(other.startValue)) {
				return false;
			}
			return true;
		}
	}

	class VarReplacer extends AbstractQueryModelVisitor<QueryEvaluationException> {

		private final Var toBeReplaced;

		private final Var replacement;

		private final long index;

		private final boolean replaceAnons;

		public VarReplacer(Var toBeReplaced, Var replacement, long index, boolean replaceAnons) {
			this.toBeReplaced = toBeReplaced;
			this.replacement = replacement;
			this.index = index;
			this.replaceAnons = replaceAnons;
		}

		@Override
		public void meet(Var var) {
			if (toBeReplaced.equals(var) || (toBeReplaced.isAnonymous() && var.isAnonymous()
					&& (toBeReplaced.hasValue() && toBeReplaced.getValue().equals(var.getValue())))) {
				QueryModelNode parent = var.getParentNode();
				parent.replaceChildNode(var, replacement.clone());
			} else if (replaceAnons && var.isAnonymous() && !var.hasValue()) {
				Var replacementVar = createAnonVar("anon_replace_" + var.getName() + index);
				QueryModelNode parent = var.getParentNode();
				parent.replaceChildNode(var, replacementVar);
			}
		}

	}

	private Var createAnonVar(String varName, Value v, boolean anonymous) {
		namedIntermediateJoins.add(varName);
		return new Var(varName, null, anonymous, false);
	}

	public Var createAnonVar(String varName) {
		return createAnonVar(varName, null, true);
	}

}
