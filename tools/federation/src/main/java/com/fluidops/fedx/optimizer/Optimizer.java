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
package com.fluidops.fedx.optimizer;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.EndpointManager;
import com.fluidops.fedx.FedX;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.algebra.SingleSourceQuery;
import com.fluidops.fedx.cache.Cache;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.structures.FedXDataset;
import com.fluidops.fedx.structures.QueryInfo;



public class Optimizer {

	private static final Logger logger = LoggerFactory.getLogger(Optimizer.class);
	
	
	public static TupleExpr optimize(TupleExpr parsed, Dataset dataset, BindingSet bindings, 
			FederationEvalStrategy strategy, QueryInfo queryInfo) throws SailException
	{
		List<Endpoint> members;
		if (dataset instanceof FedXDataset) {
			// run the query against a selected set of endpoints
			FedXDataset ds = (FedXDataset)dataset;
			members = EndpointManager.getEndpointManager().getEndpoints(ds.getEndpoints());
		} else {
			// evaluate against entire federation
			FedX fed = FederationManager.getInstance().getFederation();
			members = fed.getMembers();
		}
		
		// if the federation has a single member only, evaluate the entire query there
		if (members.size()==1 && queryInfo.getQuery()!=null)
			return new SingleSourceQuery(parsed, members.get(0), queryInfo);			
		
		// Clone the tuple expression to allow for more aggressive optimizations
		TupleExpr query = new QueryRoot(parsed.clone());
		
		Cache cache = FederationManager.getInstance().getCache();

		if (logger.isTraceEnabled())
			logger.trace("Query before Optimization: " + query);
		
		
		/* original sesame optimizers */
		new ConstantOptimizer(strategy).optimize(query, dataset, bindings);		// maybe remove this optimizer later

		new DisjunctiveConstraintOptimizer().optimize(query, dataset, bindings);

		
		/*
		 * TODO
		 * add some generic optimizers: 
		 *  - FILTER ?s=1 && ?s=2 => EmptyResult
		 *  - Remove variables that are not occuring in query stmts from filters
		 */
		
		
		/* custom optimizers, execute only when needed*/
			
		GenericInfoOptimizer info = new GenericInfoOptimizer(queryInfo);
		
		// collect information and perform generic optimizations
		info.optimize(query);
		
		// Source Selection: all nodes are annotated with their source
		SourceSelection sourceSelection = new SourceSelection(members, cache, queryInfo);
		sourceSelection.doSourceSelection(info.getStatements());
				
		// if the query has a single relevant source (and if it is no a SERVICE query), evaluate at this source only
		Set<Endpoint> relevantSources = sourceSelection.getRelevantSources();
		if (relevantSources.size()==1 && !info.hasService())
			return new SingleSourceQuery(query, relevantSources.iterator().next(), queryInfo);		
		
		if (info.hasService())
			new ServiceOptimizer(queryInfo).optimize(query);
		

		// optimize unions, if available
		if (info.hasUnion)
			new UnionOptimizer(queryInfo).optimize(query);
		
		// optimize statement groups and join order
		new StatementGroupOptimizer(queryInfo).optimize(query);

		// potentially push limits (if applicable)
		if (info.hasLimit()) {
			new LimitOptimizer().optimize(query);
		}

		// optimize Filters, if available
		// Note: this is done after the join order is determined to ease filter pushing
		if (info.hasFilter())
			new FilterOptimizer().optimize(query);
		
		if (logger.isTraceEnabled())
			logger.trace("Query after Optimization: " + query);

		return query;
	}	

}
