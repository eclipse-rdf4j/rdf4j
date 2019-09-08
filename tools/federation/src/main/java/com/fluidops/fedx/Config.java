/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package com.fluidops.fedx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.cache.MemoryCache;
import com.fluidops.fedx.endpoint.Endpoint;
import com.fluidops.fedx.endpoint.provider.ProviderUtil;
import com.fluidops.fedx.evaluation.FederationEvalStrategy;
import com.fluidops.fedx.evaluation.SailFederationEvalStrategy;
import com.fluidops.fedx.evaluation.SparqlFederationEvalStrategy;
import com.fluidops.fedx.evaluation.SparqlFederationEvalStrategyWithValues;
import com.fluidops.fedx.evaluation.concurrent.ControlledWorkerScheduler;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.monitoring.QueryLog;
import com.fluidops.fedx.monitoring.QueryPlanLog;
import com.fluidops.fedx.util.FileUtil;


/**
 * Configuration properties for FedX based on a properties file. Prior to using this configuration
 * {@link #initialize(File)} must be invoked with the location of the properties file.
 * 
 * @author Andreas Schwarte
 *
 */
public class Config {

	protected static final Logger log = LoggerFactory.getLogger(Config.class);
	
	private static Config instance = null;
	
	public static Config getConfig() {
		if (!isInitialized())
			throw new FedXRuntimeException("Config not initialized. Call Config.initialize() first.");
		return instance;
	}
	
	protected static void reset() {
		instance=null;
	}
	
	/**
	 * Initialize the configuration with default settings.
	 */
	public static void initialize() throws FedXException {
		initialize((File) null);
	}

	/**
	 * Initialize the configuration with the specified properties file.
	 * 
	 * @param fedxConfig the optional location of the properties file. If
	 *                   <code>null</code>, the default configuration is used.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static void initialize(String fedxConfig) throws FedXException {
		File file = fedxConfig != null ? new File(fedxConfig) : null;
		initialize(file);
	}

	/**
	 * Initialize the configuration with the specified properties file.
	 * 
	 * @param fedxConfig the optional location of the properties file. If
	 *                   <code>null</code>, the default configuration is used.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static synchronized void initialize(File fedxConfig) throws FedXException {
		if (isInitialized())
			throw new FedXRuntimeException("Config is already initialized.");
		instance = new Config();
		instance.init(fedxConfig);
	}
	
	static synchronized boolean isInitialized() {
		return instance != null;
	}

	
	private Properties props;
	private Config() {
		props = new Properties();
	}
	
	private void init(File configFile) throws FedXException {
		if (configFile==null) {
			log.debug("No configuration file specified. Using default config initialization.");
			return;
		}

		if (!configFile.isFile()) {
			throw new FedXException("FedX config file does not exist: " + configFile);
		}
		log.info("FedX Configuration initialized from file '" + configFile + "'.");
		try (FileInputStream in = new FileInputStream(configFile)) {
			props.load( in );
		} catch (IOException e) {
			throw new FedXException("Failed to initialize FedX configuration with " + configFile + ": " + e.getMessage());
		}
	}
	
	public String getProperty(String propertyName) {
		return props.getProperty(propertyName);
	}
	
	public String getProperty(String propertyName, String def) {
		return props.getProperty(propertyName, def);
	}
	
	/**
	 * the base directory for any location used in fedx, e.g. for repositories
	 * 
	 * @return the base directory (default: unspecified, callers should assume the
	 *         execution directory)
	 * @see FileUtil
	 */
	public String getBaseDir() {
		return props.getProperty("baseDir");
	}
	
	/**
	 * The location of the dataConfig.
	 * 
	 * @return the data config location (relative to {@link #getBaseDir()})
	 */
	public String getDataConfig() {
		return props.getProperty("dataConfig");
	}
	
	
	/**
	 * The location of the cache, i.e. currently used in {@link MemoryCache}
	 * 
	 * @return the cache location
	 */
	public String getCacheLocation() {
		return props.getProperty("cacheLocation", "cache.db");
	}
	
	/**
	 * The (maximum) number of join worker threads used in the {@link ControlledWorkerScheduler}
	 * for join operations. Default is 20.
	 * 
	 * @return the number of join worker threads
	 */
	public int getJoinWorkerThreads() {
		return Integer.parseInt( props.getProperty("joinWorkerThreads", "20"));
	}
	
	/**
	 * The (maximum) number of union worker threads used in the {@link ControlledWorkerScheduler}
	 * for join operations. Default is 20
	 * 
	 * @return number of union worker threads
	 */
	public int getUnionWorkerThreads() {
		return Integer.parseInt( props.getProperty("unionWorkerThreads", "20"));
	}
	
	/**
	 * The (maximum) number of left join worker threads used in the
	 * {@link ControlledWorkerScheduler} for join operations. Default is 10.
	 * 
	 * @return the number of left join worker threads
	 */
	public int getLeftJoinWorkerThreads() {
		return Integer.parseInt(props.getProperty("leftJoinWorkerThreads", "10"));
	}

	/**
	 * The block size for a bound join, i.e. the number of bindings that are
	 * integrated in a single subquery. Default is 15.
	 * 
	 * @return the bound join block size
	 */
	public int getBoundJoinBlockSize() {
		return Integer.parseInt( props.getProperty("boundJoinBlockSize", "15"));
	}
	
	/**
	 * Get the maximum query time in seconds used for query evaluation. Applied in CLI
	 * or in general if {@link QueryManager} is used to create queries.<p>
	 * <p>
	 * Set to 0 to disable query timeouts.</p>
	 * 
	 * The timeout is also applied for individual fine-granular join or union
	 * operations as a max time.</p>
	 * 
	 * @return the maximum query time in seconds
	 */
	public int getEnforceMaxQueryTime() {
		return Integer.parseInt( props.getProperty("enforceMaxQueryTime", "30"));
	}
	
	/**
	 * Flag to enable/disable monitoring features. Default=false.
	 * 
	 * @return whether monitoring is enabled
	 */
	public boolean isEnableMonitoring() {
		return Boolean.parseBoolean( props.getProperty("enableMonitoring", "false"));	
	}
	
	/**
	 * Flag to enable/disable JMX monitoring. Default=false
	 * 
	 * @return whether JMX is enabled
	 */
	public boolean isEnableJMX() {
		return Boolean.parseBoolean( props.getProperty("monitoring.enableJMX", "false"));	
	}
	
	/**
	 * Flag to enable/disable query plan logging via {@link QueryPlanLog}. Default=false
	 * The {@link QueryPlanLog} facility allows to retrieve the query execution plan
	 * from a variable local to the executing thread.
	 * 
	 * @return whether the query plan shall be logged
	 */
	public boolean isLogQueryPlan() {
		return Boolean.parseBoolean( props.getProperty("monitoring.logQueryPlan", "false"));	
	}
	
	/**
	 * Flag to enable/disable query logging via {@link QueryLog}. Default=false The
	 * {@link QueryLog} facility allows to log all queries to a file. See
	 * {@link QueryLog} for details.
	 * 
	 * Required {@link Config#isEnableMonitoring()} to be active.
	 * 
	 * @return whether queries are logged
	 */
	public boolean isLogQueries() {
		return Boolean.parseBoolean( props.getProperty("monitoring.logQueries", "false"));	
	}
	
	/**
	 * Returns the path to a property file containing prefix declarations as 
	 * "namespace=prefix" pairs (one per line).<p> Default: no prefixes are 
	 * replaced. Note that prefixes are only replaced when using the CLI
	 * or the {@link QueryManager} to create/evaluate queries.
	 * 
	 * Example:
	 * 
	 * <code>
	 * foaf=http://xmlns.com/foaf/0.1/
	 * rdf=http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 * =http://mydefaultns.org/
	 * </code>
	 * 			
	 * @return the location of the prefix declarations
	 */
	public String getPrefixDeclarations() {
		return props.getProperty("prefixDeclarations");
	}
	
	/**
	 * Returns the fully qualified class name of the {@link FederationEvalStrategy} implementation
	 * that is used in the case of SAIL implementations, e.g. for native stores. 
	 * 
	 * Default {@link SailFederationEvalStrategy}
	 * 
	 * @return the evaluation strategy class
	 */
	public String getSailEvaluationStrategy() {
		return props.getProperty("sailEvaluationStrategy", SailFederationEvalStrategy.class.getName());
	}
	
	/**
	 * Returns the fully qualified class name of the {@link FederationEvalStrategy}
	 * implementation that is used in the case of SPARQL implementations, e.g.
	 * SPARQL repository or remote repository.
	 * 
	 * Default {@link SparqlFederationEvalStrategyWithValues}
	 * 
	 * Alternative implementation: {@link SparqlFederationEvalStrategy}
	 * 
	 * @return the evaluation strategy class
	 */
	public String getSPARQLEvaluationStrategy() {
		return props.getProperty("sparqlEvaluationStrategy", SparqlFederationEvalStrategyWithValues.class.getName());
	}
	
	/**
	 * Whether to use a singleton {@link RepositoryConnection} per {@link Endpoint}.
	 * Default: false
	 * 
	 * If not set, a fresh {@link RepositoryConnection} is used for each triple
	 * store interaction.
	 * 
	 * @return indicator whether a singleton connection shall be used per endpoint
	 */
	public boolean useSingletonConnectionPerEndpoint() {
		return Boolean.parseBoolean(props.getProperty("endpoint.useSingletonConnection", "false"));
	}

	/**
	 * Returns a flag indicating whether vectored evaluation using the VALUES clause
	 * shall be applied for SERVICE expressions.
	 * 
	 * Default: false
	 * 
	 * Note: for todays endpoints it is more efficient to disable vectored
	 * evaluation of SERVICE.
	 * 
	 * @return whether SERVICE expressions are evaluated using bound joins
	 */
	public boolean getEnableServiceAsBoundJoin() {
		return Boolean.parseBoolean(props.getProperty("optimizer.enableServiceAsBoundJoin", "false"));
	}
	
	/**
	 * If enabled, repository connections are validated by {@link ProviderUtil#checkConnectionIfConfigured(org.eclipse.rdf4j.repository.Repository)}
	 * prior to adding the endpoint to the federation. If validation fails, an error is thrown to the user.
	 * 
	 * @return whether repository connections are validated
	 */
	public boolean isValidateRepositoryConnections() {
		return Boolean.parseBoolean( props.getProperty("validateRepositoryConnections", "true"));
	}
	
	/**
	 * The debug mode for worker scheduler, the scheduler prints usage stats regularly
	 * if enabled
	 * 
	 * @return
	 * 		whether the debug mode for the scheduler is enabled
	 */
	@Deprecated
	public boolean isDebugWorkerScheduler() {
		return Boolean.parseBoolean( props.getProperty("debugWorkerScheduler", "false"));
	}
	
	/**
	 * The debug mode for query plan. If enabled, the query execution plan is
	 * printed to stdout
	 * 
	 * @return
	 * 		whether the query plan is printed to std out
	 */
	public boolean isDebugQueryPlan() {
		return Boolean.parseBoolean(props.getProperty("debugQueryPlan", "false"));
	}
	
	/**
	 * Set some property at runtime
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		props.setProperty(key, value);
	}
}
