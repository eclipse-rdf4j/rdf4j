/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.StatementPattern.Scope;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.ZeroLengthPath;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;

public class PathIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	/**
	 * 
	 */
	private final StrictEvaluationStrategy evaluationStrategyImpl;

	private long currentLength;

	private CloseableIteration<BindingSet, QueryEvaluationException> currentIter;

	private BindingSet bindings;

	private Scope scope;

	private Var startVar;

	private Var endVar;

	private final boolean startVarFixed;

	private final boolean endVarFixed;

	private Queue<ValuePair> valueQueue;

	private final Set<ValuePair> reportedValues;

	private final Set<ValuePair> unreportedValues;;

	private TupleExpr pathExpression;

	private Var contextVar;

	private ValuePair currentVp;

	private static final String JOINVAR_PREFIX = "intermediate-join-";

	public PathIteration(StrictEvaluationStrategy evaluationStrategyImpl, Scope scope, Var startVar,
			TupleExpr pathExpression, Var endVar, Var contextVar, long minLength, BindingSet bindings)
		throws QueryEvaluationException
	{
		this.evaluationStrategyImpl = evaluationStrategyImpl;
		this.scope = scope;
		this.startVar = startVar;
		this.endVar = endVar;

		this.startVarFixed = startVar.hasValue() || bindings.hasBinding(startVar.getName());
		this.endVarFixed = endVar.hasValue() || bindings.hasBinding(endVar.getName());

		this.pathExpression = pathExpression;
		this.contextVar = contextVar;

		this.currentLength = minLength;
		this.bindings = bindings;

		this.reportedValues = makeSet();
		this.unreportedValues = makeSet();
		this.valueQueue = makeQueue();

		createIteration();
	}

	@Override
	protected BindingSet getNextElement()
		throws QueryEvaluationException
	{
		again: while (true) {
			while (!currentIter.hasNext()) {
				Iterations.closeCloseable(currentIter);
				createIteration();
				// stop condition: if the iter is an EmptyIteration
				if (currentIter instanceof EmptyIteration<?, ?>) {
					break;
				}
			}

			while (currentIter.hasNext()) {
				BindingSet nextElement = currentIter.next();

				if (!startVarFixed && !endVarFixed && currentVp != null) {
					Value startValue = currentVp.getStartValue();

					if (startValue != null) {
						nextElement = new QueryBindingSet(nextElement);
						((QueryBindingSet)nextElement).addBinding(startVar.getName(), startValue);
					}
				}

				Value v1, v2;

				if (startVarFixed && endVarFixed && currentLength > 2) {
					v1 = getVarValue(startVar, startVarFixed, nextElement);
					v2 = nextElement.getValue("END_" + JOINVAR_PREFIX + this.hashCode());
				}
				else if (startVarFixed && endVarFixed && currentLength == 2) {
					v1 = getVarValue(startVar, startVarFixed, nextElement);
					v2 = nextElement.getValue(
							JOINVAR_PREFIX + (currentLength - 1) + "-" + this.hashCode());
				}
				else {
					v1 = getVarValue(startVar, startVarFixed, nextElement);
					v2 = getVarValue(endVar, endVarFixed, nextElement);
				}

				if (!isCyclicPath(v1, v2)) {

					ValuePair vp = new ValuePair(v1, v2);
					if (reportedValues.contains(vp)) {
						// new arbitrary-length path semantics: filter out
						// duplicates
						if (currentIter.hasNext()) {
							continue;
						}
						else {
							// if the current iter is exhausted, we need to check
							// that no further paths of greater length still exists.
							continue again;
						}
					}

					if (startVarFixed && endVarFixed) {
						Value endValue = getVarValue(endVar, endVarFixed, nextElement);
						if (endValue.equals(v2)) {
							add(reportedValues, vp);
							if (!v1.equals(v2)) {
								addToQueue(valueQueue, vp);
							}
							if (!nextElement.hasBinding(startVar.getName())) {
								((QueryBindingSet)nextElement).addBinding(startVar.getName(), v1);
							}
							if (!nextElement.hasBinding(endVar.getName())) {
								((QueryBindingSet)nextElement).addBinding(endVar.getName(), v2);
							}
							return nextElement;
						}
						else {
							if (add(unreportedValues, vp)) {
								if (!v1.equals(v2)) {
									addToQueue(valueQueue, vp);
								}
							}
							continue again;
						}
					}
					else {
						add(reportedValues, vp);
						if (!v1.equals(v2)) {
							addToQueue(valueQueue, vp);
						}
						if (!nextElement.hasBinding(startVar.getName())) {
							((QueryBindingSet)nextElement).addBinding(startVar.getName(), v1);
						}
						if (!nextElement.hasBinding(endVar.getName())) {
							((QueryBindingSet)nextElement).addBinding(endVar.getName(), v2);
						}
						return nextElement;
					}
				}
				else {
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

	@Override
	protected void handleClose()
		throws QueryEvaluationException
	{
		try {
			super.handleClose();
		}
		finally {
			Iterations.closeCloseable(currentIter);
		}

	}

	/**
	 * @param valueQueue2
	 * @param vp
	 */
	protected boolean addToQueue(Queue<ValuePair> valueQueue2, ValuePair vp)
		throws QueryEvaluationException
	{
		return valueQueue2.add(vp);
	}

	/**
	 * @param valueSet
	 * @param vp
	 */
	protected boolean add(Set<ValuePair> valueSet, ValuePair vp)
		throws QueryEvaluationException
	{
		return valueSet.add(vp);
	}

	private Value getVarValue(Var var, boolean fixedValue, BindingSet bindingSet) {
		Value v;
		if (fixedValue) {
			v = var.getValue();
			if (v == null) {
				v = this.bindings.getValue(var.getName());
			}
		}
		else {
			v = bindingSet.getValue(var.getName());
		}

		return v;
	}

	private boolean isCyclicPath(Value v1, Value v2) {
		if (currentLength <= 2) {
			return false;
		}

		return reportedValues.contains(new ValuePair(v1, v2));

	}

	private void createIteration()
		throws QueryEvaluationException
	{

		if (currentLength == 0L) {
			ZeroLengthPath zlp = new ZeroLengthPath(scope, startVar, endVar, contextVar);
			currentIter = this.evaluationStrategyImpl.evaluate(zlp, bindings);
			currentLength++;
		}
		else if (currentLength == 1) {
			TupleExpr pathExprClone = pathExpression.clone();

			if (startVarFixed && endVarFixed) {
				Var replacement = createAnonVar(
						JOINVAR_PREFIX + currentLength + "-" + this.hashCode());

				VarReplacer replacer = new VarReplacer(endVar, replacement, 0, false);
				pathExprClone.visit(replacer);
			}
			currentIter = this.evaluationStrategyImpl.evaluate(pathExprClone, bindings);
			currentLength++;
		}
		else {

			currentVp = valueQueue.poll();

			if (currentVp != null) {

				TupleExpr pathExprClone = pathExpression.clone();

				if (startVarFixed && endVarFixed) {

					Var startReplacement = createAnonVar(
							JOINVAR_PREFIX + currentLength + "-" + this.hashCode());
					Var endReplacement = createAnonVar("END_" + JOINVAR_PREFIX + this.hashCode());
					startReplacement.setAnonymous(false);
					endReplacement.setAnonymous(false);

					Value v = currentVp.getEndValue();
					startReplacement.setValue(v);

					VarReplacer replacer = new VarReplacer(startVar, startReplacement, 0, false);
					pathExprClone.visit(replacer);

					replacer = new VarReplacer(endVar, endReplacement, 0, false);
					pathExprClone.visit(replacer);
				}
				else {
					Var toBeReplaced;
					Value v;
					if (!endVarFixed) {
						toBeReplaced = startVar;
						v = currentVp.getEndValue();
					}
					else {
						toBeReplaced = endVar;
						v = currentVp.getStartValue();
					}

					Var replacement = createAnonVar(
							JOINVAR_PREFIX + currentLength + "-" + this.hashCode());
					replacement.setValue(v);

					VarReplacer replacer = new VarReplacer(toBeReplaced, replacement, 0, false);
					pathExprClone.visit(replacer);
				}

				currentIter = this.evaluationStrategyImpl.evaluate(pathExprClone, bindings);
			}
			else {
				currentIter = new EmptyIteration<BindingSet, QueryEvaluationException>();
			}
			currentLength++;

		}
	}

	protected Set<ValuePair> makeSet() {
		return new HashSet<ValuePair>(64, 0.9f);
	}

	protected Queue<ValuePair> makeQueue() {
		return new ArrayDeque<ValuePair>();
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
			ValuePair other = (ValuePair)obj;
			if (endValue == null) {
				if (other.endValue != null) {
					return false;
				}
			}
			else if (!endValue.equals(other.endValue)) {
				return false;
			}
			if (startValue == null) {
				if (other.startValue != null) {
					return false;
				}
			}
			else if (!startValue.equals(other.startValue)) {
				return false;
			}
			return true;
		}
	}

	class VarReplacer extends AbstractQueryModelVisitor<QueryEvaluationException> {

		private Var toBeReplaced;

		private Var replacement;

		private long index;

		private boolean replaceAnons;

		public VarReplacer(Var toBeReplaced, Var replacement, long index, boolean replaceAnons) {
			this.toBeReplaced = toBeReplaced;
			this.replacement = replacement;
			this.index = index;
			this.replaceAnons = replaceAnons;
		}

		@Override
		public void meet(Var var) {
			if (toBeReplaced.equals(var) || (toBeReplaced.isAnonymous() && var.isAnonymous()
					&& (toBeReplaced.hasValue() && toBeReplaced.getValue().equals(var.getValue()))))
			{
				QueryModelNode parent = var.getParentNode();
				parent.replaceChildNode(var, replacement);
				replacement.setParentNode(parent);
			}
			else if (replaceAnons && var.isAnonymous() && !var.hasValue()) {
				Var replacementVar = createAnonVar("anon-replace-" + var.getName() + index);
				QueryModelNode parent = var.getParentNode();
				parent.replaceChildNode(var, replacementVar);
				replacementVar.setParentNode(parent);
			}
		}

	}

	public Var createAnonVar(String varName) {
		Var var = new Var(varName);
		var.setAnonymous(true);
		return var;
	}
}