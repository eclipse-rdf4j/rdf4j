/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.FilterTuple;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.algebra.StatementTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.iterator.BoundJoinConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.BoundJoinVALUESConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * Implementation of a federation evaluation strategy which provides some special optimizations for SPARQL (remote)
 * endpoints. In addition to the optimizations from {@link SparqlFederationEvalStrategy} this implementation uses the
 * SPARQL 1.1 VALUES operator for the bound-join evaluation (with a fallback to the pure SPARQL 1.0 UNION version).
 * 
 * @author Andreas Schwarte
 * @see BoundJoinConversionIteration
 * @since 3.0
 */
public class SparqlFederationEvalStrategyWithValues extends SparqlFederationEvalStrategy {

	public SparqlFederationEvalStrategyWithValues(FederationContext federationContext) {
		super(federationContext);
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern(
			StatementTupleExpr stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {

		// we can omit the bound join handling
		if (bindings.size() == 1)
			return evaluate(stmt, bindings.get(0));

		FilterValueExpr filterExpr = null;
		if (stmt instanceof FilterTuple)
			filterExpr = ((FilterTuple) stmt).getFilterExpr();

		AtomicBoolean isEvaluated = new AtomicBoolean(false);
		String preparedQuery = QueryStringUtil.selectQueryStringBoundJoinVALUES((StatementPattern) stmt, bindings,
				filterExpr, isEvaluated);

		CloseableIteration<BindingSet, QueryEvaluationException> result = null;
		try {
			result = evaluateAtStatementSources(preparedQuery, stmt.getStatementSources(), stmt.getQueryInfo());

			// apply filter and/or convert to original bindings
			if (filterExpr != null && !isEvaluated.get()) {
				result = new BoundJoinVALUESConversionIteration(result, bindings); // apply conversion
				result = new FilteringIteration(filterExpr, result, this); // apply filter
				if (!result.hasNext())
					return new EmptyIteration<>();
			} else {
				result = new BoundJoinVALUESConversionIteration(result, bindings);
			}

			return result;
		} catch (Throwable t) {
			Iterations.closeCloseable(result);
			throw ExceptionUtil.toQueryEvaluationException(t);
		}
	}

}
