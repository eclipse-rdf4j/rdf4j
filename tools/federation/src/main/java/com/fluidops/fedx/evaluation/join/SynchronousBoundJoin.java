/*
 * Copyright (C) 2018 Veritas Technologies LLC.
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
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.algebra.CheckStatementPattern;
import com.fluidops.fedx.algebra.IndependentJoinGroup;
import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.structures.QueryInfo;



/**
 * Execute the nested loop join in a synchronous fashion, using grouped requests,
 * i.e. group bindings into one SPARQL request using the UNION operator
 * 
 * @author Andreas Schwarte
 */
public class SynchronousBoundJoin extends SynchronousJoin {

	private static final Logger log = LoggerFactory.getLogger(SynchronousBoundJoin.class);
	
	
	public SynchronousBoundJoin(FederationEvalStrategy strategy,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			TupleExpr rightArg, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		super(strategy, leftIter, rightArg, bindings, queryInfo);
	}

	
	
	@Override
	protected void handleBindings() throws Exception {
		
		// XXX use something else as second check, e.g. an empty interface
		if (! ((rightArg instanceof StatementPattern) || (rightArg instanceof IndependentJoinGroup) )) {
			log.warn("Right argument is not a StatementPattern nor a IndependentJoinGroup. Fallback on SynchronousJoin implementation: " + rightArg.getClass().getCanonicalName());
			super.handleBindings();	// fallback
			return;
		}
		
		int nBindingsCfg = Config.getConfig().getBoundJoinBlockSize();	
		int totalBindings = 0;		// the total number of bindings
		StatementTupleExpr stmt = (StatementTupleExpr)rightArg;
		
		
		
		// optimization: if there is no free variable, we can avoid the bound-join
		// first item is always sent in a non-bound way
		
		// TODO independent join group!!!! (see ControlledWorkerBoundJoin)
		if (rightArg instanceof IndependentJoinGroup)
			throw new FedXRuntimeException("Synchronous Bound joins does not support Independent join group yet");
		
		boolean hasFreeVars = true;
		if (!closed && leftIter.hasNext()) {
			BindingSet b = leftIter.next();
			totalBindings++;
			hasFreeVars = stmt.hasFreeVarsFor(b);
			if (!hasFreeVars)
				stmt = new CheckStatementPattern(stmt);
			rightQueue.put( strategy.evaluate(stmt, b) );
		}
		
		int nBindings;
		List<BindingSet> bindings = null;
		while (!closed && leftIter.hasNext()) {
			
			/*
			 * XXX idea:
			 * 
			 * make nBindings dependent on the number of intermediate results of the left argument.
			 * 
			 * If many intermediate results, increase the number of bindings. This will result in less
			 * remote SPARQL requests.
			 * 
			 */
			if (totalBindings>10)
				nBindings = nBindingsCfg;
			else
				nBindings = 3;

			bindings = new ArrayList<BindingSet>(nBindings);
			
			int count=0;
			while (count < nBindings && leftIter.hasNext()) {
				bindings.add(leftIter.next());
				count++;
			}
			
			totalBindings += count;		
			
			if (hasFreeVars) {
				addResult( strategy.evaluateBoundJoinStatementPattern(stmt, bindings) );
			} else {
				addResult( strategy.evaluateGroupedCheck((CheckStatementPattern)stmt, bindings) );
			}
			
		}
		
		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindings + " results.");
		}
						
	}
}
