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
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.federated.algebra.FedXZeroLengthPath;
import org.eclipse.rdf4j.federated.algebra.StatementSource;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

public class FedXPathIteration extends LookAheadIteration<BindingSet> {

	// Should never be seen by code outside of this iterator
	private static final String END = "$end_from_path_iteration";
	private static final String START = "$start_from_path_iteration";

	/**
	 * Required as we can't prepare the queries yet.
	 */
	private final EvaluationStrategy strategy;
	private final QueryInfo queryInfo;

	private long currentLength;

	private CloseableIteration<BindingSet> currentIter;

	private final BindingSet bindings;

	private final Scope scope;

	private final Var startVar;

	private final Var endVar;

	private final boolean startVarFixed;

	private final boolean endVarFixed;

	private final Queue<BindingSet> valueQueue;

	private final Set<BindingSet> reportedValues;

	private final Set<BindingSet> unreportedValues;

	private final TupleExpr pathExpression;

	private final Var contextVar;

	private ValuePair currentVp;

	private static final String JOINVAR_PREFIX = "intermediate_join_";

	private final Set<String> namedIntermediateJoins = new HashSet<>();

	private final CollectionFactory collectionFactory;
	/**
	 * Instead of depending on hash codes not colliding we instead make sure that each element is unique per iteration.
	 * Which is why this is a static volatile field. As more than one path iteration can be present in the same query.
	 */
	private static volatile int PATH_ITERATOR_ID_GENERATOR = 0;

	/**
	 * Using the ++ to increment the volatile shared id generator, the id in this iterator must remain constant during
	 * execution.
	 */
	private final int pathIteratorId = PATH_ITERATOR_ID_GENERATOR++;
	private final String endVarName = "END_" + JOINVAR_PREFIX + pathIteratorId;

	public FedXPathIteration(EvaluationStrategy strategy, Scope scope, Var startVar,
			TupleExpr pathExpression, Var endVar, Var contextVar, long minLength, BindingSet bindings,
			QueryInfo queryInfo)
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

		this.collectionFactory = strategy.getCollectionFactory().get();

		this.queryInfo = queryInfo;

		// This is all necessary for optimized collections to be usable. This only becomes important on very large
		// stores with large intermediary results.
		this.reportedValues = collectionFactory.createSetOfBindingSets(ValuePair::new, FedXPathIteration::getHas,
				FedXPathIteration::getGet, FedXPathIteration::getSet);
		this.unreportedValues = collectionFactory.createSetOfBindingSets(ValuePair::new, FedXPathIteration::getHas,
				FedXPathIteration::getGet, FedXPathIteration::getSet);
		this.valueQueue = collectionFactory.createBindingSetQueue(ValuePair::new, FedXPathIteration::getHas,
				FedXPathIteration::getGet, FedXPathIteration::getSet);
		createIteration();
	}

	/**
	 * Used to turn a method call into a direct field access
	 *
	 * @param s the name of the variable to see if it is in the bindingset
	 * @return the value of the start or end or if asked for a different field a null.
	 */
	@InternalUseOnly
	public static final BiConsumer<Value, MutableBindingSet> getSet(String s) {
		switch (s) {
		case START:
			return (v, vp) -> ((ValuePair) vp).startValue = v;
		case END:
			return (v, vp) -> ((ValuePair) vp).endValue = v;
		default:
			return (v, vp) -> {
				throw new IllegalStateException("A value is being asked to be set where we never expected one");
			};
		}
	}

	/**
	 * Used to turn a method call into a direct field access
	 *
	 * @param s the name of the variable to see if it is in the bindingset
	 * @return the value of the start or end, if asked for a different field throw an illegalstate exception
	 */
	public static final Function<BindingSet, Value> getGet(String s) {
		switch (s) {
		case START:
			return (vp) -> ((ValuePair) vp).startValue;
		case END:
			return (vp) -> ((ValuePair) vp).endValue;
		default:
			return (vp) -> {
				throw new IllegalStateException("A value is being asked to be set where we never expected one");
			};
		}
	};

	/**
	 * Used to turn a method call into a direct field access
	 *
	 * @param s the name of the variable to see if it is in the bindingset
	 * @return true if start or end is not null, if asked for a different field throw an illegalstate exception
	 */
	public static final Predicate<BindingSet> getHas(String s) {
		switch (s) {
		case START:
			return (vp) -> ((ValuePair) vp).startValue != null;
		case END:
			return (vp) -> ((ValuePair) vp).endValue != null;
		default:
			return (vp) -> {
				throw new IllegalStateException("A value is being asked to be set where we never expected one");
			};
		}
	};

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

			// We are done but let the close deal with clearing up resources.
			// That method knows how to do it in the cheapest way possible.
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
			v2 = nextElement.getValue(endVarName);
		} else if (startVarFixed && endVarFixed && currentLength == 2) {
			v1 = getVarValue(startVar, startVarFixed, nextElement);
			v2 = nextElement.getValue(varNameAtPathLengthOf(currentLength - 1));
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
		if (currentIter != null) {
			currentIter.close();
		}
		collectionFactory.close();
	}

	/**
	 * @param valueQueue2
	 * @param vp
	 */
	protected boolean addToQueue(Queue<BindingSet> valueQueue2, ValuePair vp) throws QueryEvaluationException {
		return valueQueue2.add(vp);
	}

	/**
	 * @param valueSet
	 * @param vp
	 */
	protected boolean add(Set<BindingSet> valueSet, ValuePair vp) throws QueryEvaluationException {
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
				String varName = varNameAtPathLengthOf(currentLength);
				Var replacement = createAnonVar(varName, null, true);

				VarReplacer replacer = new VarReplacer(endVar, replacement, 0, false);
				pathExprClone.visit(replacer);
			}
			currentIter = this.strategy.evaluate(pathExprClone, bindings);
			currentLength++;
		} else {

			currentVp = (ValuePair) valueQueue.poll();

			if (currentVp != null) {

				TupleExpr pathExprClone = pathExpression.clone();

				if (startVarFixed && endVarFixed) {

					Value v = currentVp.getEndValue();
					Var startReplacement = createAnonVar(varNameAtPathLengthOf(currentLength), v,
							false);
					Var endReplacement = createAnonVar(endVarName, null, false);

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

					String varName = varNameAtPathLengthOf(currentLength);
					Var replacement = createAnonVar(varName, v, true);

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

	private String varNameAtPathLengthOf(long atLength) {
		return JOINVAR_PREFIX + atLength + "_" + pathIteratorId;
	}

	protected boolean isUnbound(Var var, BindingSet bindings) {
		if (var == null) {
			return false;
		} else {
			return bindings.hasBinding(var.getName()) && bindings.getValue(var.getName()) == null;
		}
	}

	/**
	 * A specialized BingingSet that can only hold the start and end values of a Path. Minimizing unneeded memory use,
	 * and allows specialization in the sets required to answer this part of a query.
	 */
	public static class ValuePair implements MutableBindingSet {
		private static final long serialVersionUID = 1L;

		private Value startValue;

		private Value endValue;

		public ValuePair() {

		}

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

		@Override
		public Iterator<Binding> iterator() {
			Binding sb = new SimpleBinding(START, startValue);
			Binding eb = new SimpleBinding(END, endValue);
			return List.of(sb, eb).iterator();
		}

		@Override
		public Set<String> getBindingNames() {
			return Set.of(START, END);
		}

		@Override
		public Binding getBinding(String bindingName) {
			switch (bindingName) {
			case START:
				return new SimpleBinding(START, startValue);
			case END:
				return new SimpleBinding(END, endValue);
			default:
				return null;
			}
		}

		@Override
		public boolean hasBinding(String bindingName) {
			switch (bindingName) {
			case START:
				return true;
			case END:
				return false;
			default:
				return false;
			}
		}

		@Override
		public Value getValue(String bindingName) {
			switch (bindingName) {
			case START:
				return startValue;
			case END:
				return endValue;
			default:
				return null;
			}
		}

		@Override
		public int size() {
			return 2;
		}

		@Override
		public void addBinding(Binding binding) {
			switch (binding.getName()) {
			case START:
				startValue = binding.getValue();
				break;
			case END:
				endValue = binding.getValue();
				break;
			}
		}

		@Override
		public void setBinding(String name, Value value) {
			switch (name) {
			case START:
				startValue = value;
				break;
			case END:
				endValue = value;
				break;
			}

		}

		@Override
		public void setBinding(Binding binding) {
			switch (binding.getName()) {
			case START:
				startValue = binding.getValue();
				break;
			case END:
				endValue = binding.getValue();
				break;
			}
		}
	}

	private class VarReplacer extends AbstractQueryModelVisitor<QueryEvaluationException> {

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
				String varName = "anon_replace_" + var.getName() + index;
				Var replacementVar = createAnonVar(varName, null, true);
				QueryModelNode parent = var.getParentNode();
				parent.replaceChildNode(var, replacementVar);
			}
		}

	}

	private Var createAnonVar(String varName, Value v, boolean anonymous) {
		namedIntermediateJoins.add(varName);
		return new Var(varName, v, anonymous, false);
	}

}
