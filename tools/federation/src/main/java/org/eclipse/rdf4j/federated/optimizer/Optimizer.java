/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.federated.FedX;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.SingleSourceQuery;
import org.eclipse.rdf4j.federated.cache.Cache;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.structures.FedXDataset;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.QueryRoot;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.ConstantOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.DisjunctiveConstraintOptimizer;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Optimizer {

	private static final Logger logger = LoggerFactory.getLogger(Optimizer.class);

	public static TupleExpr optimize(TupleExpr parsed, Dataset dataset, BindingSet bindings,
			FederationEvalStrategy strategy, QueryInfo queryInfo) throws SailException {
		FederationContext federationContext = queryInfo.getFederationContext();
		List<Endpoint> members;
		if (dataset instanceof FedXDataset) {
			// run the query against a selected set of endpoints
			FedXDataset ds = (FedXDataset) dataset;
			members = federationContext.getEndpointManager().getEndpoints(ds.getEndpoints());
		} else {
			// evaluate against entire federation
			FedX fed = federationContext.getFederation();
			members = fed.getMembers();
		}

		// if the federation has a single member only, evaluate the entire query there
		if (members.size() == 1 && queryInfo.getQuery() != null)
			return new SingleSourceQuery(parsed, members.get(0), queryInfo);

		// Clone the tuple expression to allow for more aggressive optimizations
		TupleExpr query = new QueryRoot(parsed.clone());

		Cache cache = federationContext.getCache();

		if (logger.isTraceEnabled())
			logger.trace("Query before Optimization: " + query);

		/* original sesame optimizers */
		new ConstantOptimizer(strategy).optimize(query, dataset, bindings); // maybe remove this optimizer later

		new DisjunctiveConstraintOptimizer().optimize(query, dataset, bindings);

		/*
		 * TODO add some generic optimizers: - FILTER ?s=1 && ?s=2 => EmptyResult - Remove variables that are not
		 * occuring in query stmts from filters
		 */

		/* custom optimizers, execute only when needed */

		GenericInfoOptimizer info = new GenericInfoOptimizer(queryInfo);

		// collect information and perform generic optimizations
		info.optimize(query);

		// Source Selection: all nodes are annotated with their source
		SourceSelection sourceSelection = new SourceSelection(members, cache, queryInfo);
		sourceSelection.doSourceSelection(info.getStatements());

		// if the query has a single relevant source (and if it is no a SERVICE query), evaluate at this source only
		Set<Endpoint> relevantSources = sourceSelection.getRelevantSources();
		if (relevantSources.size() == 1 && !info.hasService())
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
