/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx.evaluation;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

import com.fluidops.fedx.algebra.FilterTuple;
import com.fluidops.fedx.algebra.FilterValueExpr;
import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.evaluation.iterator.BoundJoinConversionIteration;
import com.fluidops.fedx.evaluation.iterator.BoundJoinVALUESConversionIteration;
import com.fluidops.fedx.evaluation.iterator.FilteringIteration;
import com.fluidops.fedx.exception.ExceptionUtil;
import com.fluidops.fedx.util.QueryStringUtil;


/**
 * Implementation of a federation evaluation strategy which provides some
 * special optimizations for SPARQL (remote) endpoints. In addition to the
 * optimizations from {@link SparqlFederationEvalStrategy} this implementation
 * uses the SPARQL 1.1 VALUES operator for the bound-join evaluation (with a
 * fallback to the pure SPARQL 1.0 UNION version).
 * 
 * @author Andreas Schwarte
 * @see BoundJoinConversionIteration
 * @since 3.0
 */
public class SparqlFederationEvalStrategyWithValues extends SparqlFederationEvalStrategy {

	
	public SparqlFederationEvalStrategyWithValues() {
		
	}
	
	
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateBoundJoinStatementPattern(
			StatementTupleExpr stmt, List<BindingSet> bindings)
			throws QueryEvaluationException {
		
		// we can omit the bound join handling
		if (bindings.size()==1)
			return evaluate(stmt, bindings.get(0));
				
		FilterValueExpr filterExpr = null;
		if (stmt instanceof FilterTuple)
			filterExpr = ((FilterTuple)stmt).getFilterExpr();
		
		AtomicBoolean isEvaluated = new AtomicBoolean(false);
		String preparedQuery = QueryStringUtil.selectQueryStringBoundJoinVALUES((StatementPattern)stmt, bindings, filterExpr, isEvaluated);
		
		CloseableIteration<BindingSet, QueryEvaluationException> result = null;
		try {
			result = evaluateAtStatementSources(preparedQuery, stmt.getStatementSources(), stmt.getQueryInfo());
						
			// apply filter and/or convert to original bindings
			if (filterExpr != null && !isEvaluated.get()) {
				result = new BoundJoinVALUESConversionIteration(result, bindings); // apply conversion
				result = new FilteringIteration(filterExpr, result); // apply filter
				if (!result.hasNext())
					return new EmptyIteration<BindingSet, QueryEvaluationException>();
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
