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
package com.fluidops.fedx.evaluation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.repository.RepositoryException;

import com.fluidops.fedx.algebra.CheckStatementPattern;
import com.fluidops.fedx.algebra.ExclusiveGroup;
import com.fluidops.fedx.algebra.FilterTuple;
import com.fluidops.fedx.algebra.FilterValueExpr;
import com.fluidops.fedx.algebra.IndependentJoinGroup;
import com.fluidops.fedx.algebra.StatementSource;
import com.fluidops.fedx.algebra.StatementTupleExpr;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;
import com.fluidops.fedx.evaluation.iterator.BoundJoinConversionIteration;
import com.fluidops.fedx.evaluation.iterator.FilteringIteration;
import com.fluidops.fedx.evaluation.iterator.GroupedCheckConversionIteration;
import com.fluidops.fedx.evaluation.iterator.IndependentJoingroupBindingsIteration;
import com.fluidops.fedx.evaluation.iterator.IndependentJoingroupBindingsIteration3;
import com.fluidops.fedx.evaluation.join.ControlledWorkerJoin;
import com.fluidops.fedx.structures.QueryInfo;
import com.fluidops.fedx.util.QueryAlgebraUtil;
import com.fluidops.fedx.util.QueryStringUtil;


/**
 * Implementation of a federation evaluation strategy which provides some
 * special optimizations for Native (local) Sesame repositories. The most
 * important optimization is to use prepared Queries that are already 
 * created in the internal representation used by Sesame. This is necessary
 * to avoid String parsing overhead.  
 * 
 * Joins are executed using {@link ControlledWorkerJoin}
 * @author Andreas Schwarte
 *
 */
public class SailFederationEvalStrategy extends FederationEvalStrategy {

	
	public SailFederationEvalStrategy() {
		
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
		
		Boolean isEvaluated = false;
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQueryBoundUnion((StatementPattern)stmt, bindings, filterExpr, isEvaluated);
		
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery, stmt.getStatementSources(), stmt.getQueryInfo());
						
		// apply filter and/or convert to original bindings
		if (filterExpr!=null && !isEvaluated) {
			result = new BoundJoinConversionIteration(result, bindings);		// apply conversion
			result = new FilteringIteration(filterExpr, result);				// apply filter
			if (!result.hasNext())	
				return new EmptyIteration<BindingSet, QueryEvaluationException>();
		} else {
			result = new BoundJoinConversionIteration(result, bindings);
		}
			
		return result;		
	}

	
	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateGroupedCheck(
			CheckStatementPattern stmt, List<BindingSet> bindings)
			throws QueryEvaluationException  {

		if (bindings.size()==1)
			return stmt.evaluate(bindings.get(0));
		
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQueryStringBoundCheck(stmt.getStatementPattern(), bindings);
				
		CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery, stmt.getStatementSources(), stmt.getQueryInfo());
		
		return new GroupedCheckConversionIteration(result, bindings); 	
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateIndependentJoinGroup(
			IndependentJoinGroup joinGroup, BindingSet bindings)
			throws QueryEvaluationException {
			
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQueryIndependentJoinGroup(joinGroup, bindings);
		
		try {
			List<StatementSource> statementSources = joinGroup.getMembers().get(0).getStatementSources();	// TODO this is only correct for the prototype (=> different endpoints)
			CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery, statementSources, joinGroup.getQueryInfo());
						
			// return only those elements which evaluated positively at the endpoint
			result = new IndependentJoingroupBindingsIteration(result, bindings);
			
			return result;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}

	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateIndependentJoinGroup(
			IndependentJoinGroup joinGroup, List<BindingSet> bindings)
			throws QueryEvaluationException  {
				
		String preparedQuery = QueryStringUtil.selectQueryStringIndependentJoinGroup(joinGroup, bindings);
		
		try {
			List<StatementSource> statementSources = joinGroup.getMembers().get(0).getStatementSources();	// TODO this is only correct for the prototype (=> different endpoints)
			CloseableIteration<BindingSet, QueryEvaluationException> result = evaluateAtStatementSources(preparedQuery, statementSources, joinGroup.getQueryInfo());
						
			// return only those elements which evaluated positively at the endpoint
//			result = new IndependentJoingroupBindingsIteration2(result, bindings);
			result = new IndependentJoingroupBindingsIteration3(result, bindings);
			
			return result;
		} catch (Exception e) {
			throw new QueryEvaluationException(e);
		}
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> executeJoin(
			ControlledWorkerScheduler<BindingSet> joinScheduler,
			CloseableIteration<BindingSet, QueryEvaluationException> leftIter,
			TupleExpr rightArg, Set<String> joinVars, BindingSet bindings, QueryInfo queryInfo)
			throws QueryEvaluationException {
		
		ControlledWorkerJoin join = new ControlledWorkerJoin(joinScheduler, this, leftIter, rightArg, bindings, queryInfo);
		join.setJoinVars(joinVars);
		executor.execute(join);
		return join;
	}


	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> evaluateExclusiveGroup(
			ExclusiveGroup group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		// simple thing: use a prepared query
		TripleSource tripleSource = group.getOwnedEndpoint().getTripleSource();
		AtomicBoolean isEvaluated = new AtomicBoolean(false);
		TupleExpr preparedQuery = QueryAlgebraUtil.selectQuery(group, bindings, group.getFilterExpr(), isEvaluated);
		return tripleSource.getStatements(preparedQuery, bindings,
				(isEvaluated.get() ? null : group.getFilterExpr()));
	
		// other option (which might be faster for sesame native stores): join over the statements
		// TODO implement this and evaluate if it is faster ..

	}	
		
	
}
