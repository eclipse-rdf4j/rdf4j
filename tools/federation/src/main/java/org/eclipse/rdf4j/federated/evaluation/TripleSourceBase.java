/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import java.util.function.Supplier;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.ExclusiveTupleExpr;
import org.eclipse.rdf4j.federated.algebra.FilterValueExpr;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.iterator.CloseDependentConnectionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.ConsumingIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringInsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.FilteringIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.GraphToBindingSetConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.InsertBindingsIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TripleSourceBase implements TripleSource {
	private static final Logger log = LoggerFactory.getLogger(TripleSourceBase.class);

	protected final FederationContext federationContext;
	protected final Monitoring monitoringService;
	protected final Endpoint endpoint;

	public TripleSourceBase(FederationContext federationContext, Endpoint endpoint) {
		this.federationContext = federationContext;
		this.monitoringService = federationContext.getMonitoringService();
		this.endpoint = endpoint;
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, BindingSet queryBindings, QueryType queryType, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {
			final String baseURI = queryInfo.getBaseURI();
			switch (queryType) {
			case SELECT:
				monitorRemoteRequest();
				TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery, baseURI);
				applyBindings(tQuery, queryBindings);
				applyMaxExecutionTimeUpperBound(tQuery);
				configureInference(tQuery, queryInfo);
				if (queryInfo.getResultHandler().isPresent()) {
					// pass through result to configured handler, and return an empty iteration as marker result
					tQuery.evaluate(queryInfo.getResultHandler().get());
					resultHolder.set(new EmptyIteration<>());
				} else {
					resultHolder.set(tQuery.evaluate());
				}
				return;
			case CONSTRUCT:
				monitorRemoteRequest();
				GraphQuery gQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, preparedQuery, baseURI);
				applyBindings(gQuery, queryBindings);
				applyMaxExecutionTimeUpperBound(gQuery);
				configureInference(gQuery, queryInfo);
				resultHolder.set(new GraphToBindingSetConversionIteration(gQuery.evaluate()));
				return;
			case ASK:
				monitorRemoteRequest();
				boolean hasResults;
				try (RepositoryConnection _conn = conn) {
					BooleanQuery bQuery = _conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedQuery, baseURI);
					applyBindings(bQuery, queryBindings);
					applyMaxExecutionTimeUpperBound(bQuery);
					configureInference(bQuery, queryInfo);
					hasResults = bQuery.evaluate();
				}
				resultHolder.set(booleanToBindingSetIteration(hasResults));
				return;
			default:
				throw new UnsupportedOperationException("Operation not supported for query type " + queryType);
			}
		});
	}

	private void applyBindings(Operation operation, BindingSet queryBindings) {
		if (queryBindings == null) {
			return;
		}
		for (Binding b : queryBindings) {
			operation.setBinding(b.getName(), b.getValue());
		}
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, BindingSet bindings, FilterValueExpr filterExpr, QueryInfo queryInfo)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {

			TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery, null);
			applyMaxExecutionTimeUpperBound(query);
			configureInference(query, queryInfo);

			// evaluate the query
			monitorRemoteRequest();
			CloseableIteration<BindingSet, QueryEvaluationException> res = query.evaluate();
			resultHolder.set(res);

			// apply filter and/or insert original bindings
			if (filterExpr != null) {
				if (bindings.size() > 0) {
					res = new FilteringInsertBindingsIteration(filterExpr, bindings, res,
							queryInfo.getStrategy());
				} else {
					res = new FilteringIteration(filterExpr, res, queryInfo.getStrategy());
				}
				if (!res.hasNext()) {
					Iterations.closeCloseable(res);
					conn.close();
					resultHolder.set(new EmptyIteration<>());
					return;
				}
			} else if (bindings.size() > 0) {
				res = new InsertBindingsIteration(res, bindings);
			}

			resultHolder.set(new ConsumingIteration(res, federationContext.getConfig().getConsumingIterationMax()));

		});
	}

	@Override
	public boolean hasStatements(Resource subj,
			IRI pred, Value obj, QueryInfo queryInfo, Resource... contexts) throws RepositoryException {
		try (RepositoryConnection conn = endpoint.getConnection()) {
			return conn.hasStatement(subj, pred, obj, queryInfo.getIncludeInferred(), contexts);
		}
	}

	@Override
	public boolean hasStatements(ExclusiveTupleExpr group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		monitorRemoteRequest();
		String preparedAskQuery = QueryStringUtil.askQueryString(group, bindings, group.getQueryInfo().getDataset());
		try (RepositoryConnection conn = endpoint.getConnection()) {
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedAskQuery);
			configureInference(query, group.getQueryInfo());
			applyMaxExecutionTimeUpperBound(query);
			return query.evaluate();
		}
	}

	protected void monitorRemoteRequest() {
		monitoringService.monitorRemoteRequest(endpoint);
	}

	private CloseableIteration<BindingSet, QueryEvaluationException> booleanToBindingSetIteration(boolean hasResult) {
		if (hasResult) {
			return new SingleBindingSetIteration(EmptyBindingSet.getInstance());
		}
		return new EmptyIteration<>();
	}

	/**
	 * Set includeInferred depending on {@link QueryInfo#getIncludeInferred()}
	 *
	 * @param query
	 * @param queryInfo
	 */
	protected void configureInference(Query query, QueryInfo queryInfo) {

		try {
			query.setIncludeInferred(queryInfo.getIncludeInferred());
		} catch (Exception e) {
			log.debug("Failed to set include inferred: " + e.getMessage());
			log.trace("Details:", e);
		}
	}

	/**
	 * Apply an upper bound of the maximum execution time using
	 * {@link FedXUtil#applyMaxQueryExecutionTime(Operation, FederationContext)}.
	 *
	 * @param operation the operation
	 */
	protected void applyMaxExecutionTimeUpperBound(Operation operation) {
		FedXUtil.applyMaxQueryExecutionTime(operation, federationContext);
	}

	private <T> CloseableIteration<T, QueryEvaluationException> closeConn(RepositoryConnection dependentConn,
			CloseableIteration<T, QueryEvaluationException> inner) {
		return new CloseDependentConnectionIteration<>(inner, dependentConn);
	}

	/**
	 * Convenience method to perform an operation on a {@link RepositoryConnection}. This method takes care for closing
	 * resources as well error handling. The resulting iteration has to be supplied to the {@link ResultHolder}.
	 *
	 * @param operation the {@link ConnectionOperation}
	 * @return the resulting iteration
	 */
	protected <T> CloseableIteration<T, QueryEvaluationException> withConnection(ConnectionOperation<T> operation) {

		ResultHolder<T> resultHolder = new ResultHolder<>();
		RepositoryConnection conn = endpoint.getConnection();
		try {

			operation.perform(conn, resultHolder);

			CloseableIteration<T, QueryEvaluationException> res = resultHolder.get();

			// do not wrap Empty and Pass-through Iterations
			if (res instanceof EmptyIteration) {
				conn.close();
				return res;
			}

			return closeConn(conn, res);

		} catch (Throwable t) {
			// handle all other exception case
			Iterations.closeCloseable(resultHolder.get());
			conn.close();
			throw ExceptionUtil.traceExceptionSource(endpoint, t, "");
		}
	}

	/**
	 * Interface defining the operation to be perform on the connection
	 *
	 * <p>
	 * Typical pattern
	 * </p>
	 *
	 * <pre>
	 * CloseableIteration&lt;BindingSet, QueryEvaluationException&gt; res = withConnection((conn, resultHolder) -> {
	 *  	// do something with conn
	 * 		resultHolder.set(...)
	 * });
	 *
	 * </pre>
	 *
	 * @author Andreas Schwarte
	 *
	 * @param <T>
	 * @see TripleSourceBase#withConnection(ConnectionOperation)
	 */
	protected interface ConnectionOperation<T> {
		void perform(RepositoryConnection conn, ResultHolder<T> resultHolder);
	}

	/**
	 * Holder for a result iteration to be used with {@link TripleSourceBase#withConnection(ConnectionOperation)}. Note
	 * that the result holder should also be set with temporary results to properly allow error handling.
	 *
	 * @author Andreas Schwarte
	 *
	 * @param <T>
	 */
	protected static class ResultHolder<T> implements Supplier<CloseableIteration<T, QueryEvaluationException>> {

		protected CloseableIteration<T, QueryEvaluationException> result;

		public void set(CloseableIteration<T, QueryEvaluationException> result) {
			this.result = result;
		}

		@Override
		public CloseableIteration<T, QueryEvaluationException> get() {
			return result;
		}

	}
}
