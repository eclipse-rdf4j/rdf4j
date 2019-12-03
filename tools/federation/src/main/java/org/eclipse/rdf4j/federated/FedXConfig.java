/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.SailFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategyWithValues;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.monitoring.QueryLog;
import org.eclipse.rdf4j.federated.monitoring.QueryPlanLog;

/**
 * Configuration class for FedX
 * 
 * @author Andreas Schwarte
 */
public class FedXConfig {

	public static FedXConfig DEFAULT_CONFIG = new FedXConfig();

	private String cacheLocation = "cache.db";

	private int joinWorkerThreads = 20;

	private int unionWorkerThreads = 20;

	private int leftJoinWorkerThreads = 10;

	private int boundJoinBlockSize = 15;

	private int enforceMaxQueryTime = 30;

	private boolean enableServiceAsBoundJoin = false;

	private boolean enableMonitoring = false;

	private boolean isLogQueryPlan = false;

	private boolean isLogQueries = false;

	private boolean debugQueryPlan = false;

	private boolean enableJmx = false;

	private Class<? extends FederationEvalStrategy> sailEvaluationStrategy = SailFederationEvalStrategy.class;

	private Class<? extends FederationEvalStrategy> sparqlEvaluationStrategy = SparqlFederationEvalStrategyWithValues.class;

	private String prefixDeclarations = null;

	/* factory like setters */

	/**
	 * Set whether the query plan shall be debugged. See {@link #isDebugQueryPlan()).
	 * 
	 * <p>Can be set after federation construction and initialize.</p>
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
	 * Set the {@link FederationEvalStrategy} for SPARQL federations. See {@link #getSPARQLEvaluationStrategy()}.
	 * 
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 * 
	 * @param sparqlEvaluationStrategy
	 * @return the current config
	 */
	public FedXConfig withSparqlEvaluationStrategy(Class<? extends FederationEvalStrategy> sparqlEvaluationStrategy) {
		this.sparqlEvaluationStrategy = sparqlEvaluationStrategy;
		return this;
	}

	/**
	 * Set the {@link FederationEvalStrategy} for SAIL federations. See {@link #getSailEvaluationStrategy()}.
	 * 
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 * 
	 * @param sailEvaluationStrategy
	 * @return the current config
	 */
	public FedXConfig withSailEvaluationStrategy(Class<? extends FederationEvalStrategy> sailEvaluationStrategy) {
		this.sailEvaluationStrategy = sailEvaluationStrategy;
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
	 * Define the cache location. See {@link #getCacheLocation()}.
	 * 
	 * <p>
	 * Can only be set before federation initialization.
	 * </p>
	 * 
	 * @param cacheLocation
	 * @return the current config
	 */
	public FedXConfig withCacheLocation(String cacheLocation) {
		this.cacheLocation = cacheLocation;
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
	 * @return
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
	 * The location of the cache, i.e. currently used in {@link MemoryCache}
	 * 
	 * @return the cache location
	 */
	public String getCacheLocation() {
		return this.cacheLocation;
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
	 * Required {@link Config#isEnableMonitoring()} to be active.
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
	 * Returns the class of the {@link FederationEvalStrategy} implementation that is used in the case of SAIL
	 * implementations, e.g. for native stores.
	 * 
	 * Default {@link SailFederationEvalStrategy}
	 * 
	 * @return the evaluation strategy class
	 */
	public Class<? extends FederationEvalStrategy> getSailEvaluationStrategy() {
		return sailEvaluationStrategy;
	}

	/**
	 * Returns the class of the {@link FederationEvalStrategy} implementation that is used in the case of SPARQL
	 * implementations, e.g. SPARQL repository or remote repository.
	 * 
	 * Default {@link SparqlFederationEvalStrategyWithValues}
	 * 
	 * Alternative implementation: {@link SparqlFederationEvalStrategy}
	 * 
	 * @return the evaluation strategy class
	 */
	public Class<? extends FederationEvalStrategy> getSPARQLEvaluationStrategy() {
		return sparqlEvaluationStrategy;
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
	 * Flag to enable/disable JMX monitoring. Default=false
	 * 
	 * @return whether JMX is enabled
	 */
	public boolean isEnableJMX() {
		return enableJmx;
	}
}
