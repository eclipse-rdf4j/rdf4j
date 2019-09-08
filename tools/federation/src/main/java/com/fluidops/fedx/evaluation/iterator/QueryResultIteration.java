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
package com.fluidops.fedx.evaluation.iterator;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.structures.QueryInfo;


/**
 * An iteration which wraps the final result and in case of exceptions aborts query evaluation
 * for the corresponding query in fedx (potentially subqueries are still running, and jobs are
 * scheduled). 
 * 
 * If some external component calls close() on this iteration AND if the corresponding query
 * is still running, the query is aborted within FedX. An example case would be Sesame's 
 * QueryInteruptIterations, which is used to enforce maxQueryTime.
 * 
 * If the query is finished, the FederationManager is notified that the query is done, and the
 * query is removed from the set of running queries.
 * 
 * @author Andreas Schwarte
 *
 */
public class QueryResultIteration extends AbstractCloseableIteration<BindingSet, QueryEvaluationException>
{

	// TODO apply this class and provide test case
	
	protected final CloseableIteration<BindingSet, QueryEvaluationException> inner;
	protected final QueryInfo queryInfo;
	
	public QueryResultIteration(
			CloseableIteration<BindingSet, QueryEvaluationException> inner, QueryInfo queryInfo) {
		super();
		this.inner = inner;
		this.queryInfo = queryInfo;
	}
	
	
	@Override	
	public boolean hasNext() throws QueryEvaluationException {
		if (inner.hasNext())
			return true;
		else {
			// inform the query manager that this query is done
			FederationManager.getInstance().getQueryManager().finishQuery(queryInfo);
			return false;
		}
	}

	@Override
	public BindingSet next() throws QueryEvaluationException {
		try {
			BindingSet next = inner.next();
			if (next==null)
				FederationManager.getInstance().getQueryManager().finishQuery(queryInfo);
			return next;
		} catch (QueryEvaluationException e){
			abortQuery();
			throw e;
		}
	}

	@Override
	public void remove() throws QueryEvaluationException {
		inner.remove();		
	}

	
	@Override
	protected void handleClose() throws QueryEvaluationException {
		inner.close();
		abortQuery();
	}
	

	/**
	 * Abort the query in the schedulers if it is still running.
	 */
	protected void abortQuery() {
		QueryManager qm = FederationManager.getInstance().getQueryManager();
		if (qm.isRunning(queryInfo))
			qm.abortQuery(queryInfo);
	}
}
