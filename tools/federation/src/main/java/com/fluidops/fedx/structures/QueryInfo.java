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
package com.fluidops.fedx.structures;

import java.math.BigInteger;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.QueryManager;
import com.fluidops.fedx.evaluation.concurrent.ParallelTask;
import com.fluidops.fedx.util.QueryStringUtil;



/**
 * Structure to maintain query information during evaluation, is attached to algebra nodes. 
 * Each instance is uniquely attached to the query.
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
	
	protected boolean done = false;

	protected Set<ParallelTask<?>> scheduledSubtasks = ConcurrentHashMap.newKeySet();

	public QueryInfo(String query, QueryType queryType) {
		this(query, queryType, 0);
	}

	/**
	 * 
	 * @param query
	 * @param queryType
	 * @param maxExecutionTime the maximum explicit query time in seconds, if 0 use
	 *                         {@link Config#getEnforceMaxQueryTime()}
	 */
	public QueryInfo(String query, QueryType queryType, int maxExecutionTime) {
		super();
		this.queryID = QueryManager.getNextQueryId();

		this.query = query;
		this.queryType = queryType;

		int _maxExecutionTime = maxExecutionTime <= 0 ? Config.getConfig().getEnforceMaxQueryTime() : maxExecutionTime;
		this.maxExecutionTimeMs = _maxExecutionTime * 1000;
		this.start = System.currentTimeMillis();
	}

	public QueryInfo(Resource subj, IRI pred, Value obj)
	{
		this(QueryStringUtil.toString(subj, (IRI) pred, obj), QueryType.GET_STATEMENTS);
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

	/**
	 * 
	 * @return the maximum remaining time in ms until the query runs into a timeout. If negative, timeout has been reached
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
	 * Mark the query as aborted and abort all scheduled (future) tasks known at
	 * this point in time. Also do not accept any new scheduled tasks
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
	 * Close this query. If exists, all scheduled (future) tasks known at this point
	 * in time are aborted. Also do not accept any new scheduled tasks
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
