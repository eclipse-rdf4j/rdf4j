/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.structures;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ParallelTask;
import org.eclipse.rdf4j.federated.util.QueryStringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
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
	private final QueryType queryType;
	private final long maxExecutionTimeMs;
	private final long start;
	private final boolean includeInferred;

	private final FederationContext federationContext;

	protected boolean done = false;

	protected Set<ParallelTask<?>> scheduledSubtasks = ConcurrentHashMap.newKeySet();

	public QueryInfo(String query, QueryType queryType, boolean incluedInferred,
			FederationContext federationContext) {
		this(query, queryType, 0, incluedInferred, federationContext);
	}

	/**
	 * 
	 * @param query
	 * @param queryType
	 * @param maxExecutionTime  the maximum explicit query time in seconds, if 0 use
	 *                          {@link org.eclipse.rdf4j.federated.FedXConfig#getEnforceMaxQueryTime()}
	 * @param includeInferred   whether to include inferred statements
	 * @param federationContext the {@link FederationContext}
	 */
	public QueryInfo(String query, QueryType queryType, int maxExecutionTime, boolean includeInferred,
			FederationContext federationContext) {
		super();
		this.queryID = federationContext.getQueryManager().getNextQueryId();

		this.federationContext = federationContext;

		this.query = query;
		this.queryType = queryType;

		int _maxExecutionTime = maxExecutionTime <= 0 ? federationContext.getConfig().getEnforceMaxQueryTime()
				: maxExecutionTime;
		this.maxExecutionTimeMs = _maxExecutionTime * 1000;
		this.includeInferred = includeInferred;
		this.start = System.currentTimeMillis();
	}

	public QueryInfo(Resource subj, IRI pred, Value obj, boolean includeInferred,
			FederationContext federationContext) {
		this(QueryStringUtil.toString(subj, (IRI) pred, obj), QueryType.GET_STATEMENTS, includeInferred,
				federationContext);
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
			throw new QueryEvaluationException("Query is aborted or closed, cannot accept new tasks");
		}
		scheduledSubtasks.add(task);
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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryInfo other = (QueryInfo) obj;
		if (queryID == null) {
			if (other.queryID != null)
				return false;
		} else if (!queryID.equals(other.queryID))
			return false;
		return true;
	}

}
