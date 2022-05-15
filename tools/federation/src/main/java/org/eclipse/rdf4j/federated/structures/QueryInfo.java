/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.structures;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.PassThroughTupleExpr;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Structure to maintain query information during evaluation, is attached to algebra nodes. Each instance is uniquely
 * attached to the query.
 *
 * The queryId can be used to abort tasks belonging to a particular evaluation.
 *
 * @author Andreas Schwarte
 *
 */
public class QueryInfo {

	private static final Logger log = LoggerFactory.getLogger(QueryInfo.class);

	protected static final AtomicInteger NEXT_QUERY_ID = new AtomicInteger(1); // static id count

	private final BigInteger queryID;
	private final String query;
	private final String baseURI;

	private final QueryType queryType;
	private final long maxExecutionTimeMs;
	private final long start;
	private final boolean includeInferred;
	private final Dataset dataset;

	private TupleQueryResultHandler resultHandler = null;

	private final FederationContext federationContext;

	private final FederationEvalStrategy strategy;

	protected boolean done = false;

	protected Set<ParallelTask<?>> scheduledSubtasks = ConcurrentHashMap.newKeySet();

	/**
	 *
	 * @param query
	 * @param queryType
	 * @param maxExecutionTime  the maximum explicit query time in seconds, if 0 use
	 *                          {@link org.eclipse.rdf4j.federated.FedXConfig#getEnforceMaxQueryTime()}
	 * @param includeInferred   whether to include inferred statements
	 * @param federationContext the {@link FederationContext}
	 * @param dataset           the {@link Dataset}
	 */
	public QueryInfo(String query, String baseURI, QueryType queryType, int maxExecutionTime, boolean includeInferred,
			FederationContext federationContext, FederationEvalStrategy strategy, Dataset dataset) {
		super();
		this.queryID = federationContext.getQueryManager().getNextQueryId();

		this.federationContext = federationContext;

		this.query = query;
		this.baseURI = baseURI;
		this.queryType = queryType;
		this.dataset = dataset;

		int _maxExecutionTime = maxExecutionTime <= 0 ? federationContext.getConfig().getEnforceMaxQueryTime()
				: maxExecutionTime;
		this.maxExecutionTimeMs = _maxExecutionTime * 1000;
		this.includeInferred = includeInferred;
		this.start = System.currentTimeMillis();

		this.strategy = strategy;
	}

	public QueryInfo(Resource subj, IRI pred, Value obj, int maxExecutionTime, boolean includeInferred,
			FederationContext federationContext, FederationEvalStrategy strategy, Dataset dataset) {
		this(QueryStringUtil.toString(subj, pred, obj), null, QueryType.GET_STATEMENTS, maxExecutionTime,
				includeInferred,
				federationContext, strategy, dataset);
	}

	public BigInteger getQueryID() {
		return queryID;
	}

	public String getQuery() {
		return query;
	}

	public QueryType getQueryType() {
		return queryType;
	}

	public boolean getIncludeInferred() {
		return includeInferred;
	}

	public Dataset getDataset() {
		return dataset;
	}

	/**
	 * @return the baseURI
	 */
	public String getBaseURI() {
		return baseURI;
	}

	/**
	 *
	 * @return the {@link FederationEvalStrategy} active in the current query context
	 */
	public FederationEvalStrategy getStrategy() {
		return this.strategy;
	}

	/**
	 *
	 * @return the {@link FederationContext} in which this query is executed
	 */
	public FederationContext getFederationContext() {
		return this.federationContext;
	}

	/**
	 *
	 * @return the maximum remaining time in ms until the query runs into a timeout. If negative, timeout has been
	 *         reached
	 */
	public long getMaxRemainingTimeMS() {
		if (maxExecutionTimeMs <= 0) {
			return Long.MAX_VALUE;
		}
		// compute max remaining time
		// Note: return 1ms as a timeout to properly get this handled in executors
		long runningTime = System.currentTimeMillis() - start;
		long maxTime = maxExecutionTimeMs - runningTime;
		if (log.isTraceEnabled()) {
			log.trace("Applying max remaining time: " + maxTime);
		}
		return maxTime;
	}

	/**
	 * Register a new scheduled task for this query.
	 *
	 * @param task
	 * @throws QueryEvaluationException if the query has been aborted or closed
	 */
	public synchronized void registerScheduledTask(ParallelTask<?> task) throws QueryEvaluationException {
		if (done) {
			task.cancel();
			throw new QueryEvaluationException("Query is aborted or closed, cannot accept new tasks");
		}
		scheduledSubtasks.add(task);
	}

	/**
	 * Returns a {@link TupleQueryResultHandler} if this query is executed using.
	 * {@link TupleQuery#evaluate(TupleQueryResultHandler)}.
	 *
	 * @return the {@link TupleQueryResultHandler} that can be used for pass through
	 * @see PassThroughTupleExpr
	 */
	public Optional<TupleQueryResultHandler> getResultHandler() {
		return Optional.ofNullable(resultHandler);
	}

	/**
	 * Set the {@link TupleQueryResultHandler} if the query is executed using
	 * {@link TupleQuery#evaluate(TupleQueryResultHandler)} allowing for passing through results to the handler.
	 *
	 * @param resultHandler the {@link TupleQueryResultHandler}
	 */
	public void setResultHandler(TupleQueryResultHandler resultHandler) {
		this.resultHandler = resultHandler;
	}

	/**
	 * Mark the query as aborted and abort all scheduled (future) tasks known at this point in time. Also do not accept
	 * any new scheduled tasks
	 *
	 */
	public synchronized void abort() {
		if (done) {
			return;
		}
		done = true;

		abortScheduledTasks();
	}

	/**
	 * Close this query. If exists, all scheduled (future) tasks known at this point in time are aborted. Also do not
	 * accept any new scheduled tasks
	 *
	 */
	public synchronized void close() {

		if (done) {
			return;
		}
		done = true;

		abortScheduledTasks();
	}

	/**
	 * Abort any scheduled future tasks
	 */
	protected void abortScheduledTasks() {

		for (ParallelTask<?> task : scheduledSubtasks) {
			task.cancel();
		}

		scheduledSubtasks.clear();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queryID == null) ? 0 : queryID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		QueryInfo other = (QueryInfo) obj;
		if (queryID == null) {
			if (other.queryID != null) {
				return false;
			}
		} else if (!queryID.equals(other.queryID)) {
			return false;
		}
		return true;
	}

}
