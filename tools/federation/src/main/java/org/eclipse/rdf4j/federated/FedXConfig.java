/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.Optional;

import org.eclipse.rdf4j.federated.cache.SourceSelectionCache;
import org.eclipse.rdf4j.federated.cache.SourceSelectionMemoryCache;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.TaskWrapper;
import org.eclipse.rdf4j.federated.evaluation.iterator.ConsumingIteration;
import org.eclipse.rdf4j.federated.monitoring.QueryLog;
import org.eclipse.rdf4j.federated.monitoring.QueryPlanLog;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Query;

import com.google.common.cache.CacheBuilderSpec;

/**
 * Configuration class for FedX
 *
 * @author Andreas Schwarte
 */
public class FedXConfig {

	public static FedXConfig DEFAULT_CONFIG = new FedXConfig();

	private int joinWorkerThreads = 20;

	private int unionWorkerThreads = 20;

	private int leftJoinWorkerThreads = 10;

	private int boundJoinBlockSize = 15;

	private int enforceMaxQueryTime = 30;

	private boolean enableServiceAsBoundJoin = true;

	private boolean enableMonitoring = false;

	private boolean isLogQueryPlan = false;

	private boolean isLogQueries = false;

	private boolean debugQueryPlan = false;

	private boolean includeInferredDefault = true;

	private String sourceSelectionCacheSpec = null;

	private TaskWrapper taskWrapper = null;

	private String prefixDeclarations = null;

	private int consumingIterationMax = 1000;

	/* factory like setters */

	/**
	 * Set whether the query plan shall be debugged. See {@link #isDebugQueryPlan()}.
	 *
	 * <p>
	 * Can be set after federation construction and initialize.
	 * </p>
	 *
	 * @param flag
	 * @return the current config
	 */
	public FedXConfig withDebugQueryPlan(boolean flag) {
		this.debugQueryPlan = flag;
		return this;
	}

	/**
	 * Set whether to log queries. See {@link #isLogQueries()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param flag
	 * @return the current config
	 */
	public FedXConfig withLogQueries(boolean flag) {
		this.isLogQueries = flag;
		return this;
	}

	/**
	 * Set enforce max query time. See {@link #getEnforceMaxQueryTime()}.
	 *
	 * <p>
	 * Can be set after federation construction and initialize.
	 * </p>
	 *
	 * @param enforceMaxQueryTime time in seconds, 0 to disable
	 * @return the current config
	 */
	public FedXConfig withEnforceMaxQueryTime(int enforceMaxQueryTime) {
		this.enforceMaxQueryTime = enforceMaxQueryTime;
		return this;
	}

	/**
	 * Set the default value supplied to {@link Query#setIncludeInferred(boolean)}
	 *
	 * @param flag
	 * @return the current config
	 */
	public FedXConfig withIncludeInferredDefault(boolean flag) {
		this.includeInferredDefault = flag;
		return this;
	}

	/**
	 * Enable monitoring. See {@link #isEnableMonitoring()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param flag
	 * @return the current config
	 */
	public FedXConfig withEnableMonitoring(boolean flag) {
		this.enableMonitoring = flag;
		return this;
	}

	/**
	 * Set the bound join block size. See {@link #getBoundJoinBlockSize()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param boundJoinBlockSize
	 * @return the current config
	 */
	public FedXConfig withBoundJoinBlockSize(int boundJoinBlockSize) {
		this.boundJoinBlockSize = boundJoinBlockSize;
		return this;
	}

	/**
	 * Set the number of join worker threads. See {@link #getJoinWorkerThreads()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param joinWorkerThreads
	 * @return the current config
	 */
	public FedXConfig withJoinWorkerThreads(int joinWorkerThreads) {
		this.joinWorkerThreads = joinWorkerThreads;
		return this;
	}

	/**
	 * Set the number of left join worker threads. See {@link #getLeftJoinWorkerThreads()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param leftJoinWorkerThreads
	 * @return the current config
	 */
	public FedXConfig withLeftJoinWorkerThreads(int leftJoinWorkerThreads) {
		this.leftJoinWorkerThreads = leftJoinWorkerThreads;
		return this;
	}

	/**
	 * Set the number of union worker threads. See {@link #getUnionWorkerThreads()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param unionWorkerThreads
	 * @return the current config
	 */
	public FedXConfig withUnionWorkerThreads(int unionWorkerThreads) {
		this.unionWorkerThreads = unionWorkerThreads;
		return this;
	}

	/**
	 * Set the optional prefix declarations file. See {@link #getPrefixDeclarations()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param prefixFile
	 * @return config
	 */
	public FedXConfig withPrefixDeclarations(String prefixFile) {
		this.prefixDeclarations = prefixFile;
		return this;
	}

	/**
	 * Whether to log the query plan with {@link QueryPlanLog}. See {@link #isLogQueryPlan()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param flag
	 * @return the current config
	 */
	public FedXConfig withLogQueryPlan(boolean flag) {
		this.isLogQueryPlan = flag;
		return this;
	}

	/**
	 * Whether external SERVICE clauses are evaluated using bound join (i.e. with the VALUES clause). Default
	 * <i>true</i>
	 *
	 * @param flag
	 * @return the current config.
	 */
	public FedXConfig withEnableServiceAsBoundJoin(boolean flag) {
		this.enableServiceAsBoundJoin = flag;
		return this;
	}

	/**
	 * The cache specification for the {@link SourceSelectionMemoryCache}. If not set explicitly, the
	 * {@link SourceSelectionMemoryCache#DEFAULT_CACHE_SPEC} is used.
	 *
	 * @param cacheSpec the {@link CacheBuilderSpec} for the {@link SourceSelectionCache}
	 * @return the current config
	 * @see SourceSelectionMemoryCache
	 */
	public FedXConfig withSourceSelectionCacheSpec(String cacheSpec) {
		this.sourceSelectionCacheSpec = cacheSpec;
		return this;
	}

	/**
	 * Sets a {@link TaskWrapper} which may be used for wrapping any background {@link Runnable}s. If no such wrapper is
	 * explicitly configured, the unmodified task is returned. See {@link TaskWrapper} for more information.
	 *
	 * @param taskWrapper the {@link TaskWrapper}
	 * @return the current config
	 * @see TaskWrapper
	 */
	public FedXConfig withTaskWrapper(TaskWrapper taskWrapper) {
		this.taskWrapper = taskWrapper;
		return this;
	}

	/**
	 * The (maximum) number of join worker threads used in the {@link ControlledWorkerScheduler} for join operations.
	 * Default is 20.
	 *
	 * @return the number of join worker threads
	 */
	public int getJoinWorkerThreads() {
		return joinWorkerThreads;
	}

	/**
	 * The (maximum) number of union worker threads used in the {@link ControlledWorkerScheduler} for join operations.
	 * Default is 20
	 *
	 * @return number of union worker threads
	 */
	public int getUnionWorkerThreads() {
		return unionWorkerThreads;
	}

	/**
	 * The (maximum) number of left join worker threads used in the {@link ControlledWorkerScheduler} for join
	 * operations. Default is 10.
	 *
	 * @return the number of left join worker threads
	 */
	public int getLeftJoinWorkerThreads() {
		return leftJoinWorkerThreads;
	}

	/**
	 * The block size for a bound join, i.e. the number of bindings that are integrated in a single subquery. Default is
	 * 15.
	 *
	 * @return the bound join block size
	 */
	public int getBoundJoinBlockSize() {
		return boundJoinBlockSize;
	}

	/**
	 * Returns a flag indicating whether vectored evaluation using the VALUES clause shall be applied for SERVICE
	 * expressions.
	 *
	 * Default: false
	 *
	 * Note: for todays endpoints it is more efficient to disable vectored evaluation of SERVICE.
	 *
	 * @return whether SERVICE expressions are evaluated using bound joins
	 */
	public boolean getEnableServiceAsBoundJoin() {
		return enableServiceAsBoundJoin;
	}

	/**
	 * Get the maximum query time in seconds used for query evaluation. Applied if {@link QueryManager} is used to
	 * create queries.
	 * <p>
	 * <p>
	 * Set to 0 to disable query timeouts.
	 * </p>
	 *
	 * The timeout is also applied for individual fine-granular join or union operations as a max time.
	 * </p>
	 *
	 * @return the maximum query time in seconds
	 */
	public int getEnforceMaxQueryTime() {
		return enforceMaxQueryTime;
	}

	/**
	 *
	 * @return the default for {@link Operation#getIncludeInferred()}
	 */
	public boolean getIncludeInferredDefault() {
		return includeInferredDefault;
	}

	/**
	 * Flag to enable/disable monitoring features. Default=false.
	 *
	 * @return whether monitoring is enabled
	 */
	public boolean isEnableMonitoring() {
		return enableMonitoring;
	}

	/**
	 * Flag to enable/disable query plan logging via {@link QueryPlanLog}. Default=false The {@link QueryPlanLog}
	 * facility allows to retrieve the query execution plan from a variable local to the executing thread.
	 *
	 * @return whether the query plan shall be logged
	 */
	public boolean isLogQueryPlan() {
		return isLogQueryPlan;
	}

	/**
	 * Flag to enable/disable query logging via {@link QueryLog}. Default=false The {@link QueryLog} facility allows to
	 * log all queries to a file. See {@link QueryLog} for details.
	 *
	 * Requires {@link #isEnableMonitoring()} to be active.
	 *
	 * @return whether queries are logged
	 */
	public boolean isLogQueries() {
		return isLogQueries;
	}

	/**
	 * Returns the path to a property file containing prefix declarations as "namespace=prefix" pairs (one per line).
	 * <p>
	 * Default: no prefixes are replaced. Note that prefixes are only replaced when using the {@link QueryManager} to
	 * create/evaluate queries.
	 *
	 * Example:
	 *
	 * <code>
	 * foaf=http://xmlns.com/foaf/0.1/
	 * rdf=http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 * =http://mydefaultns.org/
	 * </code>
	 *
	 * @return the location of the prefix declarations or <code>null</code> if not configured
	 */
	public String getPrefixDeclarations() {
		return prefixDeclarations;
	}

	/**
	 * Returns the configured {@link CacheBuilderSpec} (if any) for the {@link SourceSelectionMemoryCache}. If not
	 * defined, the {@link SourceSelectionMemoryCache#DEFAULT_CACHE_SPEC} is used.
	 *
	 * @return the {@link CacheBuilderSpec} or <code>null</code>
	 */
	public String getSourceSelectionCacheSpec() {
		return this.sourceSelectionCacheSpec;
	}

	/**
	 * The debug mode for query plan. If enabled, the query execution plan is printed to stdout
	 *
	 * @return whether the query plan is printed to std out
	 */
	public boolean isDebugQueryPlan() {
		return debugQueryPlan;
	}

	/**
	 * Returns a {@link TaskWrapper} which may be used for wrapping any background {@link Runnable}s. If no such wrapper
	 * is explicitly configured, the unmodified task is returned. See {@link TaskWrapper} for more information.
	 *
	 * @return the {@link TaskWrapper}, an empty {@link Optional} if none is explicitly configured
	 */
	public Optional<TaskWrapper> getTaskWrapper() {
		return Optional.ofNullable(taskWrapper);
	}

	/**
	 * Set the max number of results to be consumed by {@link ConsumingIteration}. See
	 * {@link #getConsumingIterationMax()}.
	 *
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 *
	 * @param max
	 * @return the current config
	 */
	public FedXConfig withConsumingIterationMax(int max) {
		this.consumingIterationMax = max;
		return this;
	}

	/**
	 * Returns the max number of results to be consumed by {@link ConsumingIteration}
	 */
	public int getConsumingIterationMax() {
		return consumingIterationMax;
	}
}
