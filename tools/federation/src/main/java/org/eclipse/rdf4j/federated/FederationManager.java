/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.federated.cache.Cache;
import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.endpoint.EndpointType;
import org.eclipse.rdf4j.federated.evaluation.DelegateFederatedServiceResolver;
import org.eclipse.rdf4j.federated.evaluation.FederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategyFactory;
import org.eclipse.rdf4j.federated.evaluation.SailFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.NamingThreadFactory;
import org.eclipse.rdf4j.federated.evaluation.concurrent.Scheduler;
import org.eclipse.rdf4j.federated.evaluation.union.ControlledWorkerUnion;
import org.eclipse.rdf4j.federated.evaluation.union.SynchronousWorkerUnion;
import org.eclipse.rdf4j.federated.evaluation.union.WorkerUnionBase;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.federated.monitoring.Monitoring;
import org.eclipse.rdf4j.federated.monitoring.MonitoringFactory;
import org.eclipse.rdf4j.federated.monitoring.MonitoringUtil;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.federated.util.Version;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The FederationManager manages all modules necessary for the runtime behavior. This includes for instance the
 * federation layer instance, cache and statistics. It is Singleton and there can only be on federation instance at a
 * time.
 * <p>
 * 
 * The factory {@link FedXFactory} provides various functions for initialization of FedX and should be used as the entry
 * point for any application using FedX.
 * <p>
 * 
 * <pre>
 * Config.initialize(fedxConfig);
 * List&ltEndpoint&gt members = ...			// e.g. use EndpointFactory methods
 * Repository repo = FedXFactory.initializeFederation(endpoints);
 * ReositoryConnection conn = repo.getConnection();
 * 
 * // Do something with the connection, e.g. query evaluation
 * repo.shutDown();
 * </pre>
 * 
 * @author Andreas Schwarte
 *
 */
public class FederationManager {

	private static final Logger log = LoggerFactory.getLogger(FederationManager.class);

	/**
	 * The Federation type definition: Local, Remote, Hybrid
	 * 
	 * @author Andreas Schwarte
	 */
	public static enum FederationType {
		LOCAL,
		REMOTE,
		HYBRID;
	}

	/**
	 * The singleton instance of the federation manager
	 */
	private static FederationManager instance = null;

	/**
	 * Initialize the Singleton {@link FederationManager} instance with the provided information. The
	 * {@link FederationManager} remains initialized until {@link #shutDown()} is invoked, usually this is done by
	 * invoking {@link Repository#shutDown()}:
	 * 
	 * @param members initialize the federation with a list of repository members, null and empty lists are allowed
	 * @param cache   the cache instance to be used
	 * @return the initialized {@link Repository} representing the federation. Needs to be shut down by the caller
	 */
	static synchronized FedXRepository initialize(List<Endpoint> members, Cache cache) {
		if (instance != null)
			throw new FedXRuntimeException("FederationManager already initialized.");

		log.info("Initializing federation manager ...");
		log.info("FedX Version Information: " + Version.getVersionInfo().getVersionString());

		monitoring = MonitoringFactory.createMonitoring();

		DelegateFederatedServiceResolver.initialize();

		ExecutorService ex = Executors.newCachedThreadPool(new NamingThreadFactory("FedX Executor"));
		FedX federation = new FedX(members);

		FedXRepository repo = new FedXRepository(federation);

		instance = new FederationManager(federation, cache, ex, repo);

		if (Config.getConfig().isEnableJMX()) {
			try {
				MonitoringUtil.initializeJMXMonitoring();
			} catch (Exception e1) {
				log.error("JMX monitoring could not be initialized: " + e1.getMessage());
			}
		}

		// initialize prefix declarations, if any
		String prefixFile = Config.getConfig().getPrefixDeclarations();
		if (prefixFile != null) {
			QueryManager qm = instance.getQueryManager();
			Properties props = new Properties();
			try (FileInputStream fin = new FileInputStream(new File(prefixFile))) {
				props.load(fin);
			} catch (IOException e) {
				throw new FedXRuntimeException("Error loading prefix properties: " + e.getMessage());
			}

			for (String ns : props.stringPropertyNames()) {
				qm.addPrefixDeclaration(ns, props.getProperty(ns)); // register namespace/prefix pair
			}
		}

		return repo;
	}

	/**
	 * Return the initialized {@link FederationManager} instance.
	 * 
	 * @return the federation manager
	 */
	public static FederationManager getInstance() {
		if (instance == null)
			throw new FedXRuntimeException("FederationManager has not been initialized yet, call #initialize() first.");
		return instance;
	}

	/**
	 * Returns true if the {@link FederationManager} is initialized.
	 * 
	 * @return true or false;
	 */
	public static boolean isInitialized() {
		return instance != null;
	}

	static Monitoring monitoring;

	public static Monitoring getMonitoringService() {
		if (!isInitialized())
			throw new IllegalStateException("Monitoring service can only be used if FedX is initialized.");
		return monitoring;
	}

	/* Instance variables */
	protected FedX federation;
	protected Cache cache;
	protected ExecutorService executor;
	protected FederationEvalStrategy strategy;
	protected FederationType type;
	protected ControlledWorkerScheduler<BindingSet> joinScheduler;
	protected ControlledWorkerScheduler<BindingSet> leftJoinScheduler;
	protected ControlledWorkerScheduler<BindingSet> unionScheduler;

	private FederationManager(FedX federation, Cache cache, ExecutorService executor,
			Repository repo) {
		this.federation = federation;
		this.cache = cache;
		this.executor = executor;
		QueryManager.instance = new QueryManager(this, repo); // initialize the singleton query manager
	}

	public FedX getFederation() {
		return federation;
	}

	/**
	 * Reset the {@link Scheduler} instances, i.e. abort all running threads and create a new scheduler instance.
	 */
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Scheduler for join and union are reset.");
		}

		if (joinScheduler != null)
			joinScheduler.abort();
		joinScheduler = new ControlledWorkerScheduler<BindingSet>(Config.getConfig().getJoinWorkerThreads(),
				"Join Scheduler");

		if (unionScheduler != null)
			unionScheduler.abort();
		unionScheduler = new ControlledWorkerScheduler<BindingSet>(Config.getConfig().getUnionWorkerThreads(),
				"Union Scheduler");

		if (leftJoinScheduler != null)
			leftJoinScheduler.abort();
		leftJoinScheduler = new ControlledWorkerScheduler<BindingSet>(Config.getConfig().getLeftJoinWorkerThreads(),
				"Left Join Scheduler");

	}

	public Cache getCache() {
		return cache;
	}

	public Executor getExecutor() {
		return executor;
	}

	public Monitoring getMonitoring() {
		return monitoring;
	}

	public FederationEvalStrategy getStrategy() {
		return strategy;
	}

	public ControlledWorkerScheduler<BindingSet> getJoinScheduler() {
		return joinScheduler;
	}

	public ControlledWorkerScheduler<BindingSet> getLeftJoinScheduler() {
		return leftJoinScheduler;
	}

	public ControlledWorkerScheduler<BindingSet> getUnionScheduler() {
		return unionScheduler;
	}

	public FederationType getFederationType() {
		return type;
	}

	/**
	 * 
	 * @return the singleton query manager
	 */
	public QueryManager getQueryManager() {
		// the singleton querymanager
		return QueryManager.getInstance();
	}

	/**
	 * Add the specified endpoint to the federation. The endpoint must be initialized and the federation must not
	 * contain a member with the same endpoint location.
	 * 
	 * @param e              the initialized endpoint
	 * @param updateStrategy optional parameter, to determine if strategy is to be updated, default=true
	 * 
	 * @throws FedXRuntimeException if the endpoint is not initialized, or if the federation has already a member with
	 *                              the same location
	 */
	public void addEndpoint(Endpoint e, boolean... updateStrategy) throws FedXRuntimeException {
		log.info("Adding endpoint " + e.getId() + " to federation ...");

		/* check if endpoint is initialized */
		if (!e.isInitialized()) {
			try {
				e.initialize();
			} catch (RepositoryException e1) {
				throw new FedXRuntimeException(
						"Provided endpoint was not initialized and could not be initialized: " + e1.getMessage(), e1);
			}
		}

		/* check for duplicate before adding: heuristic => same location */
		for (Endpoint member : federation.getMembers())
			if (member.getEndpoint().equals(e.getEndpoint()))
				throw new FedXRuntimeException("Adding failed: there exists already an endpoint with location "
						+ e.getEndpoint() + " (eid=" + member.getId() + ")");

		federation.addMember(e);
		EndpointManager.getEndpointManager().addEndpoint(e);

		if (updateStrategy == null || updateStrategy.length == 0
				|| (updateStrategy.length == 1 && updateStrategy[0] == true))
			updateStrategy();
	}

	/**
	 * Add the specified endpoints to the federation and take care for updating all structures.
	 * 
	 * @param endpoints a list of initialized endpoints to add
	 */
	public void addAll(List<Endpoint> endpoints) {
		log.info("Adding " + endpoints.size() + " endpoints to the federation.");

		for (Endpoint e : endpoints) {
			addEndpoint(e, false);
		}

		updateStrategy();
	}

	/**
	 * Remove the specified endpoint from the federation.
	 * 
	 * @param e              the endpoint
	 * @param updateStrategy optional parameter, to determine if strategy is to be updated, default=true
	 */
	public void removeEndpoint(Endpoint e, boolean... updateStrategy) throws RepositoryException {
		log.info("Removing endpoint " + e.getId() + " from federation ...");

		/* check if e is a federation member */
		if (!federation.getMembers().contains(e))
			throw new FedXRuntimeException("Endpoint " + e.getId() + " is not a member of the current federation.");

		federation.removeMember(e);
		EndpointManager.getEndpointManager().removeEndpoint(e);
		e.shutDown();

		if (updateStrategy == null || updateStrategy.length == 0
				|| (updateStrategy.length == 1 && updateStrategy[0] == true))
			updateStrategy();
	}

	/**
	 * Remove all endpoints from the federation, e.g. to load a new preset. Repositories of the endpoints are shutDown,
	 * and the EndpointManager is added accordingly.
	 * 
	 * @throws RepositoryException
	 */
	public void removeAll() throws RepositoryException {
		log.info("Removing all endpoints from federation.");

		for (Endpoint e : new ArrayList<Endpoint>(federation.getMembers())) {
			removeEndpoint(e, false);
		}

		updateStrategy();
	}

	/**
	 * return the number of triples in the federation as string. Retrieving the size is only supported
	 * {@link EndpointType#NativeStore} and {@link EndpointType#RemoteRepository}.
	 * 
	 * If the federation contains other types of endpoints, the size is indicated as a lower bound, i.e. the string
	 * starts with a larger sign.
	 * 
	 * @return the number of triples in the federation
	 */
	public String getFederationSize() {
		long size = 0;
		boolean isLowerBound = false;
		for (Endpoint e : getFederation().getMembers())
			try {
				size += e.size();
			} catch (RepositoryException e1) {
				isLowerBound = true;
			}
		return isLowerBound ? ">" + size : Long.toString(size);
	}

	/**
	 * Shutdown the federation including the following operations:
	 * <p>
	 * 
	 * <ul>
	 * <li>shut down repositories of all federation members</li>
	 * <li>persist the cached information</li>
	 * <li>clear the endpoint manager</li>
	 * </ul>
	 * 
	 * @throws FedXException if an error occurs while shutting down the federation
	 */
	public synchronized void shutDown() throws FedXException {
		if (instance == null) {
			log.warn("Federation is already shut down. Ignoring.");
			log.debug("Details:", new Exception("Trace"));
			return;
		}
		log.info("Shutting down federation and all underlying repositories ...");
		// Abort all running queries
		QueryManager.instance.shutdown();
		executor.shutdown();
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.warn("Failed to shutdown executor:" + e.getMessage());
			log.debug("Details:", e);
		}
		try {
			joinScheduler.shutdown();
		} catch (Exception e) {
			log.warn("Failed to shutdown join scheduler: " + e.getMessage());
			log.debug("Details: ", e);
		}
		try {
			unionScheduler.shutdown();
		} catch (Exception e) {
			log.warn("Failed to shutdown union scheduler: " + e.getMessage());
			log.debug("Details: ", e);
		}
		try {
			leftJoinScheduler.shutdown();
		} catch (Exception e) {
			log.warn("Failed to shutdown left join scheduler: " + e.getMessage());
			log.debug("Details: ", e);
		}
		DelegateFederatedServiceResolver.shutdown(); // shutdown any federated service resolver
		federation.shutDownInternal();
		cache.persist();
		Config.reset();
		EndpointManager.getEndpointManager().shutDown();
		instance = null;
		monitoring = null;
	}

	/**
	 * Create an appropriate worker union for this federation, i.e. a synchronous worker union for local federations and
	 * a multithreaded worker union for remote & hybrid federations.
	 * 
	 * @return the {@link WorkerUnionBase}
	 * 
	 * @see ControlledWorkerUnion
	 * @see SynchronousWorkerUnion
	 */
	public WorkerUnionBase<BindingSet> createWorkerUnion(QueryInfo queryInfo) {
		FederationEvalStrategy strategy = FederationManager.getInstance().getStrategy();
		if (type == FederationType.LOCAL)
			return new SynchronousWorkerUnion<BindingSet>(strategy, queryInfo);
		return new ControlledWorkerUnion<BindingSet>(strategy, unionScheduler, queryInfo);

	}

	/**
	 * Update the federation evaluation strategy using the classification of endpoints as provided by
	 * {@link Endpoint#getEndpointClassification()}:
	 * <p>
	 * 
	 * Which strategy is applied depends on {@link FederationEvaluationStrategyFactory}.
	 * 
	 * Default strategies:
	 * <ul>
	 * <li>local federation: {@link SailFederationEvalStrategy}</li>
	 * <li>endpoint federation: {@link SparqlFederationEvalStrategy}</li>
	 * <li>hybrid federation: {@link SparqlFederationEvalStrategy}</li>
	 * </ul>
	 * 
	 */
	public void updateStrategy() {

		int localCount = 0, remoteCount = 0;
		for (Endpoint e : federation.getMembers()) {
			if (e.getEndpointClassification() == EndpointClassification.Remote)
				remoteCount++;
			else
				localCount++;
		}

		boolean updated = false;
		if (remoteCount == 0) {
			if (type != FederationType.LOCAL) {
				type = FederationType.LOCAL;
				updated = true;
			}
		} else if (localCount == 0) {
			if (type != FederationType.REMOTE) {
				type = FederationType.REMOTE;
				updated = true;
			}
		} else {
			if (type != FederationType.HYBRID) {
				type = FederationType.HYBRID;
				updated = true;
			}
		}

		if (updated) {
			strategy = FederationEvaluationStrategyFactory.getEvaluationStrategy(type);
			log.info("Federation updated. Type: " + type + ", evaluation strategy is "
					+ instance.strategy.getClass().getSimpleName());
		}

	}

}
