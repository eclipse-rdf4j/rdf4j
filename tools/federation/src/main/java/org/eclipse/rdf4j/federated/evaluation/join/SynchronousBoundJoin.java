/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.join;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.algebra.CheckStatementPattern;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute the nested loop join in a synchronous fashion, using grouped requests, i.e. group bindings into one SPARQL
 * request using the UNION operator
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
		if (!((rightArg instanceof StatementPattern))) {
			log.warn(
					"Right argument is not a StatementPattern. Fallback on SynchronousJoin implementation: "
							+ rightArg.getClass().getCanonicalName());
			super.handleBindings(); // fallback
			return;
		}

		int nBindingsCfg = this.queryInfo.getFederationContext().getConfig().getBoundJoinBlockSize();
		int totalBindings = 0; // the total number of bindings
		StatementTupleExpr stmt = (StatementTupleExpr) rightArg;

		// optimization: if there is no free variable, we can avoid the bound-join
		// first item is always sent in a non-bound way

		boolean hasFreeVars = true;
		if (!isClosed() && leftIter.hasNext()) {
			BindingSet b = leftIter.next();
			totalBindings++;
			hasFreeVars = stmt.hasFreeVarsFor(b);
			if (!hasFreeVars) {
				stmt = new CheckStatementPattern(stmt, queryInfo);
			}
			addResult(strategy.evaluate(stmt, b));
		}

		int nBindings;
		List<BindingSet> bindings;
		while (!isClosed() && leftIter.hasNext()) {

			/*
			 * XXX idea:
			 *
			 * make nBindings dependent on the number of intermediate results of the left argument.
			 *
			 * If many intermediate results, increase the number of bindings. This will result in less remote SPARQL
			 * requests.
			 *
			 */
			if (totalBindings > 10) {
				nBindings = nBindingsCfg;
			} else {
				nBindings = 3;
			}

			bindings = new ArrayList<>(nBindings);

			int count = 0;
			while (!isClosed() && count < nBindings && leftIter.hasNext()) {
				bindings.add(leftIter.next());
				count++;
			}

			totalBindings += count;
			if (isClosed())
				return;
			if (hasFreeVars) {
				addResult(strategy.evaluateBoundJoinStatementPattern(stmt, bindings));
			} else {
				addResult(strategy.evaluateGroupedCheck((CheckStatementPattern) stmt, bindings));
			}

		}

		if (log.isDebugEnabled()) {
			log.debug("JoinStats: left iter of " + getDisplayId() + " had " + totalBindings + " results.");
		}

	}
}
