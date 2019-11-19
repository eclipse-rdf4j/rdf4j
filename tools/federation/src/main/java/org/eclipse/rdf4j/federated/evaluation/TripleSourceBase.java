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
import org.eclipse.rdf4j.federated.algebra.ExclusiveGroup;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.evaluation.iterator.CloseDependentConnectionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.GraphToBindingSetConversionIteration;
import org.eclipse.rdf4j.federated.evaluation.iterator.SingleBindingSetIteration;
import org.eclipse.rdf4j.federated.exception.ExceptionUtil;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;
import org.eclipse.rdf4j.federated.structures.QueryType;
import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
	protected final FederationEvalStrategy strategy;

	public TripleSourceBase(FederationContext federationContext, Endpoint endpoint) {
		this.federationContext = federationContext;
		this.monitoringService = federationContext.getMonitoringService();
		this.endpoint = endpoint;
		this.strategy = federationContext.getStrategy();
	}

	@Override
	public CloseableIteration<BindingSet, QueryEvaluationException> getStatements(
			String preparedQuery, QueryType queryType)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		return withConnection((conn, resultHolder) -> {
			switch (queryType) {
			case SELECT:
				monitorRemoteRequest();
				TupleQuery tQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, preparedQuery);
				applyMaxExecutionTimeUpperBound(tQuery);
				disableInference(tQuery);
				resultHolder.set(tQuery.evaluate());
				return;
			case CONSTRUCT:
				monitorRemoteRequest();
				GraphQuery gQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, preparedQuery);
				applyMaxExecutionTimeUpperBound(gQuery);
				disableInference(gQuery);
				resultHolder.set(new GraphToBindingSetConversionIteration(gQuery.evaluate()));
				return;
			case ASK:
				monitorRemoteRequest();
				boolean hasResults = false;
				try (RepositoryConnection _conn = conn) {
					BooleanQuery bQuery = _conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedQuery);
					applyMaxExecutionTimeUpperBound(bQuery);
					disableInference(bQuery);
					hasResults = bQuery.evaluate();
				}
				resultHolder.set(booleanToBindingSetIteration(hasResults));
				return;
			default:
				throw new UnsupportedOperationException("Operation not supported for query type " + queryType);
			}
		});
	}

	@Override
	public boolean hasStatements(Resource subj,
			IRI pred, Value obj, Resource... contexts) throws RepositoryException {
		try (RepositoryConnection conn = endpoint.getConnection()) {
			return conn.hasStatement(subj, pred, obj, false, contexts);
		}
	}

	@Override
	public boolean hasStatements(ExclusiveGroup group, BindingSet bindings)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException {

		monitorRemoteRequest();
		String preparedAskQuery = QueryStringUtil.askQueryString(group, bindings);
		try (RepositoryConnection conn = endpoint.getConnection()) {
			BooleanQuery query = conn.prepareBooleanQuery(QueryLanguage.SPARQL, preparedAskQuery);
			disableInference(query);
			applyMaxExecutionTimeUpperBound(query);
			return query.evaluate();
		}
	}

	protected void monitorRemoteRequest() {
		monitoringService.monitorRemoteRequest(endpoint);
	}

	private CloseableIteration<BindingSet, QueryEvaluationException> booleanToBindingSetIteration(boolean hasResult) {
		if (hasResult)
			return new SingleBindingSetIteration(EmptyBindingSet.getInstance());
		return new EmptyIteration<>();
	}

	/**
	 * Set includeInference to disabled explicitly.
	 * 
	 * @param query
	 */
	protected void disableInference(Query query) {
		// set includeInferred to false explicitly
		try {
			query.setIncludeInferred(false);
		} catch (Exception e) {
			log.debug("Failed to set include inferred: " + e.getMessage());
			log.trace("Details:", e);
		}
	}

	/**
	 * Apply an upper bound of the maximum execution time using {@link FedXUtil#applyMaxQueryExecutionTime(Operation)}.
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

			// do not wrap Empty Iterations
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
	protected static interface ConnectionOperation<T> {
		public void perform(RepositoryConnection conn, ResultHolder<T> resultHolder);
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
