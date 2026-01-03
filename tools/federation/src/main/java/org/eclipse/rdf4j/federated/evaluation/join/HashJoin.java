/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.iterator.LazyMutableClosableIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.QueryAlgebraUtil;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;

/**
 * Operator for a hash join of tuple expressions.
 *
 * @author Andreas Schwarte
 * @since 6.0
 */
@Deprecated(forRemoval = true)
public class HashJoin extends JoinExecutorBase<BindingSet> {

	private final QueryEvaluationStep rightPrepared;

	public HashJoin(FederationEvalStrategy strategy,
			CloseableIteration<BindingSet> leftIter,
			TupleExpr rightArg, QueryEvaluationStep rightPrepared, Set<String> joinVars, BindingSet bindings,
			QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(strategy, leftIter, rightArg, bindings, queryInfo);
		setJoinVars(joinVars);
		this.rightPrepared = rightPrepared;
	}

	@Override
	protected void handleBindings() throws Exception {

		// the total number of bindings
		int totalBindingsLeft = 0;
		int totalBindingsRight = 0;
		Collection<String> rightFreeVars = QueryAlgebraUtil.getFreeVars(rightArg);
		Set<String> joinVars = getJoinVars();

		// evaluate the right join argument
		// Note: wrapped in lazy mutable iteration for repetitive reading
		try (LazyMutableClosableIteration rightArgIter = new LazyMutableClosableIteration(
				rightPrepared.evaluate(bindings))) {

			while (!isClosed() && leftIter.hasNext()) {

				int blockSizeL = 10;
				if (totalBindingsLeft > 20) {
					blockSizeL = 100;
				}
				List<BindingSet> leftBlock = new ArrayList<>(blockSizeL);
				for (int i = 0; i < blockSizeL && leftIter.hasNext(); i++) {
					leftBlock.add(leftIter.next());
					totalBindingsLeft++;
				}

				int blockSizeR = 10;
				while (!isClosed() && rightArgIter.hasNext()) {
					if (totalBindingsRight > 20) {
						blockSizeR = 100;
					}
					Collection<BindingSet> rightBlock = new ArrayList<>(blockSizeR);
					for (int i = 0; i < blockSizeR && rightArgIter.hasNext(); i++) {
						rightBlock.add(rightArgIter.next());
						totalBindingsRight++;
					}

					performJoin(leftBlock, rightBlock, joinVars, rightFreeVars);
				}

				rightArgIter.resetCursor();
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindingsLeft + " results.");
		}
	}

	/**
	 * Perform the join and add the result to this cursor.
	 * <p>
	 * See {@link #join(Collection, Collection, Set, Collection)} and {@link #addResult(CloseableIteration)}.
	 * </p>
	 *
	 * @param leftBlock
	 * @param rightBlock
	 * @param joinVariables
	 * @param freeVariablesRight
	 */
	protected void performJoin(Collection<BindingSet> leftBlock, Collection<BindingSet> rightBlock,
			Set<String> joinVariables, Collection<String> freeVariablesRight) {
		addResult(join(leftBlock, rightBlock, joinVariables, freeVariablesRight));
	}

	/**
	 * Perform a hash join of bindings from the left block with those of the right block.
	 * <p>
	 * This method keeps the merged bindings in the results, if the join variables match and if all previously resolved
	 * bindings hold.
	 * </p>
	 *
	 * @param leftBlock
	 * @param rightBlock
	 * @param joinVariables
	 * @param freeVariablesRight
	 * @return the merged binding result
	 */
	static CloseableIteration<BindingSet> join(Collection<BindingSet> leftBlock,
			Collection<BindingSet> rightBlock, Set<String> joinVariables, Collection<String> freeVariablesRight) {
		List<BindingSet> res = new LinkedList<>();

		for (BindingSet left : leftBlock) {

			for (BindingSet right : rightBlock) {

				boolean match = false;
				// check join variables: must be equal in both operands
				for (String joinVariable : joinVariables) {
					Value leftValue = left.getValue(joinVariable);
					Value rightValue = right.getValue(joinVariable);

					// join match
					if (leftValue != null && leftValue.equals(rightValue)) {
						match = true;
						break;
					}
				}

				// check other free variables of right expression
				// => must be available in bindings
				for (String freeVariable : freeVariablesRight) {
					if (joinVariables.contains(freeVariable)) {
						continue; // skip
					}
					Value leftValue = left.getValue(freeVariable);
					Value rightValue = right.getValue(freeVariable);
					if (leftValue != null && leftValue.equals(rightValue)) {
						match = true;
					}
				}

				if (match) {
					// emit a merged binding set
					MapBindingSet mergedBindings = new MapBindingSet();
					for (Binding b : left) {
						mergedBindings.setBinding(b);
					}
					for (Binding b : right) {
						mergedBindings.setBinding(b);
					}
					res.add(mergedBindings);
				}
			}
		}

		return new CollectionIteration<>(res);
	}

}
