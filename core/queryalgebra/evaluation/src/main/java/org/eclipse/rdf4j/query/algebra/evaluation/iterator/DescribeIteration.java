/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;

/**
 * Iteration that implements a simplified version of Symmetric Concise Bounded Description (omitting reified
 * statements).
 *
 * @author Jeen Broekstra
 * @see <a href="http://www.w3.org/Submission/CBD/#alternatives">Concise Bounded Description - alternatives</a>
 */
@Deprecated(since = "4.1.0")
public class DescribeIteration extends LookAheadIteration<BindingSet, QueryEvaluationException> {

	protected final static String VARNAME_SUBJECT = "subject";

	protected final static String VARNAME_PREDICATE = "predicate";

	protected final static String VARNAME_OBJECT = "object";

	private final List<String> describeExprNames;

	private final EvaluationStrategy strategy;

	private Value startValue;

	private final Queue<BNode> nodeQueue = new ArrayDeque<>();

	private final Set<BNode> processedNodes = new HashSet<>();

	private CloseableIteration<BindingSet, QueryEvaluationException> currentDescribeExprIter;

	private enum Mode {
		OUTGOING_LINKS,
		INCOMING_LINKS
	}

	private Mode currentMode = Mode.OUTGOING_LINKS;

	private final Iteration<BindingSet, QueryEvaluationException> sourceIter;

	public DescribeIteration(Iteration<BindingSet, QueryEvaluationException> sourceIter, EvaluationStrategy strategy,
			Set<String> describeExprNames, BindingSet parentBindings) {
		this.strategy = strategy;
		this.sourceIter = sourceIter;
		this.describeExprNames = new ArrayList<>(describeExprNames);
		this.parentBindings = parentBindings;
	}

	private BindingSet currentBindings;

	private int describeExprsIndex;

	protected BindingSet parentBindings;

	private void resetCurrentDescribeExprIter() throws QueryEvaluationException {
		while (currentDescribeExprIter == null) {
			if (currentBindings == null && startValue == null) {
				if (sourceIter.hasNext()) {
					currentBindings = sourceIter.next();
				} else {
					// no more bindings, therefore no more results to return.
					return;
				}
			}

			if (startValue == null) {
				String nextValueExpr = describeExprNames.get(describeExprsIndex++);
				if (nextValueExpr != null) {
					startValue = currentBindings.getValue(nextValueExpr);
					if (describeExprsIndex == describeExprNames.size()) {
						// reached the end of the list of valueExprs, reset to
						// read next value from source iterator if any.
						currentBindings = null;
						describeExprsIndex = 0;
					}
					currentMode = Mode.OUTGOING_LINKS;
				}
			}

			switch (currentMode) {
			case OUTGOING_LINKS:
				currentDescribeExprIter = createNextIteration(startValue, null);
				if (!currentDescribeExprIter.hasNext()) {
					// start value has no outgoing links.
					currentDescribeExprIter.close();
					currentDescribeExprIter = null;
					currentMode = Mode.INCOMING_LINKS;
				}
				break;
			case INCOMING_LINKS:
				currentDescribeExprIter = createNextIteration(null, startValue);
				if (!currentDescribeExprIter.hasNext()) {
					// no incoming links for this start value.
					currentDescribeExprIter.close();
					currentDescribeExprIter = null;
					startValue = null;
					currentMode = Mode.OUTGOING_LINKS;
				}
				break;
			}
		} // end while
	}

	@Override
	protected BindingSet getNextElement() throws QueryEvaluationException {
		resetCurrentDescribeExprIter();
		if (currentDescribeExprIter == null) {
			return null;
		}

		while (!currentDescribeExprIter.hasNext() && !nodeQueue.isEmpty()) {
			// process next node in queue
			BNode nextNode = nodeQueue.poll();
			currentDescribeExprIter.close();
			switch (currentMode) {
			case OUTGOING_LINKS:
				currentDescribeExprIter = createNextIteration(nextNode, null);
				break;
			case INCOMING_LINKS:
				currentDescribeExprIter = createNextIteration(null, nextNode);
				break;

			}
			processedNodes.add(nextNode);

			if (nodeQueue.isEmpty() && !currentDescribeExprIter.hasNext()) {
				// we have hit a blank node that has no further expansion. reset to
				// initialize next in value expression queue.
				currentDescribeExprIter.close();
				currentDescribeExprIter = null;

				if (currentMode == Mode.OUTGOING_LINKS) {
					currentMode = Mode.INCOMING_LINKS;
				} else {
					// done with this valueExpr, reset to initialize next in value
					// expression queue.
					currentMode = Mode.OUTGOING_LINKS;
					startValue = null;
				}

				resetCurrentDescribeExprIter();
				if (currentDescribeExprIter == null) {
					return null;
				}
			}

		}

		if (currentDescribeExprIter.hasNext()) {
			BindingSet bs = currentDescribeExprIter.next();

			String varname = currentMode == Mode.OUTGOING_LINKS ? VARNAME_OBJECT : VARNAME_SUBJECT;

			Value v = bs.getValue(varname);
			if (v instanceof BNode) {
				if (!processedNodes.contains(v)) { // duplicate/cycle detection
					nodeQueue.add((BNode) v);
				}
			}

			if (!currentDescribeExprIter.hasNext() && nodeQueue.isEmpty()) {
				currentDescribeExprIter.close();
				currentDescribeExprIter = null;

				if (currentMode == Mode.OUTGOING_LINKS) {
					currentMode = Mode.INCOMING_LINKS;
				} else {
					// done with this valueExpr, reset to initialize next in value
					// expression queue.
					currentMode = Mode.OUTGOING_LINKS;
					startValue = null;
				}
			}

			return bs;
		}

		return null;
	}

	protected CloseableIteration<BindingSet, QueryEvaluationException> createNextIteration(Value subject, Value object)
			throws QueryEvaluationException {
		if (subject == null && object == null) {
			return new EmptyIteration<>();
		}

		Var subjVar = new Var(VARNAME_SUBJECT, subject);
		Var predVar = new Var(VARNAME_PREDICATE);
		Var objVar = new Var(VARNAME_OBJECT, object);

		StatementPattern pattern = new StatementPattern(subjVar, predVar, objVar);
		return strategy.evaluate(pattern, parentBindings);
	}

}
