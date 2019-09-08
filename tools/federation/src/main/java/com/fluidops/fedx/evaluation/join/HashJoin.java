/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.evaluation.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration;

import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.iterator.LazyMutableClosableIteration;
import com.fluidops.fedx.optimizer.JoinOrderOptimizer;
import com.fluidops.fedx.structures.QueryInfo;

/**
 * Operator for a hash join of tuple expressions.
 * 
 * @author Andreas Schwarte
 * @since 6.0
 */
public class HashJoin extends JoinExecutorBase<BindingSet> {


	public HashJoin(FederationEvalStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			TupleExpr rightArg, Set<String> joinVars, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(strategy, leftIter, rightArg, bindings, queryInfo);
		setJoinVars(joinVars);
	}

	@Override
	protected void handleBindings() throws Exception {

		// the total number of bindings
		int totalBindingsLeft = 0;
		int totalBindingsRight = 0;
		Collection<String> rightFreeVars = JoinOrderOptimizer.getFreeVars(rightArg);
		Set<String> joinVars = getJoinVars();
		

		// evaluate the right join argument
		// Note: wrapped in lazy mutable iteration for repetitive reading
		try (LazyMutableClosableIteration rightArgIter = new LazyMutableClosableIteration(
				strategy.evaluate(rightArg, bindings))) {
			
			while (!closed && leftIter.hasNext()) {

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
				while (!closed && rightArgIter.hasNext()) {
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
	 * See {@link #join(Collection, Collection, Set, Collection)} and
	 * {@link #addResult(CloseableIteration)}.
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
	 * Perform a hash join of bindings from the left block with those of the right
	 * block.
	 * <p>
	 * This method keeps the merged bindings in the results, if the join variables
	 * match and if all previously resolved bindings hold.
	 * </p>
	 * 
	 * @param leftBlock
	 * @param rightBlock
	 * @param joinVariables
	 * @param freeVariablesRight
	 * @return the merged binding result
	 */
	static CloseableIteration<BindingSet, QueryEvaluationException> join(Collection<BindingSet> leftBlock,
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
						mergedBindings.addBinding(b);
					}
					for (Binding b : right) {
						mergedBindings.addBinding(b);
					}
					res.add(mergedBindings);
				}
			}
		}

		return new CollectionIteration<>(res);
	}

}
