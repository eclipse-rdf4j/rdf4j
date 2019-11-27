/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
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
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
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
 * FedXRepository repo = FedXFactory.initializeFederation(endpoints);
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

	/* Instance variables */
	private FederationContext federationContext;
	private FedX federation;
	private ExecutorService executor;
	private FederationEvalStrategy strategy;
	private FederationType type;
	private ControlledWorkerScheduler<BindingSet> joinScheduler;
	private ControlledWorkerScheduler<BindingSet> leftJoinScheduler;
	private ControlledWorkerScheduler<BindingSet> unionScheduler;

	public FederationManager() {

	}

	public void init(FedX federation, FederationContext federationContext) {
		this.federation = federation;
		this.federationContext = federationContext;
		this.executor = Executors.newCachedThreadPool(new NamingThreadFactory("FedX Executor"));

		updateStrategy();
		reset();
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
		joinScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getJoinWorkerThreads(),
				"Join Scheduler");

		if (unionScheduler != null)
			unionScheduler.abort();
		unionScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getUnionWorkerThreads(),
				"Union Scheduler");

		if (leftJoinScheduler != null)
			leftJoinScheduler.abort();
		leftJoinScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getLeftJoinWorkerThreads(),
				"Left Join Scheduler");

	}

	public Executor getExecutor() {
		return executor;
	}

	public FedX getFederation() {
		return this.federation;
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
	 * Add the specified endpoint to the federation. The federation must not contain a member with the same endpoint
	 * location.
	 * 
	 * @param e              the endpoint
	 * @param updateStrategy optional parameter, to determine if strategy is to be updated, default=true
	 * 
	 * @throws FedXRuntimeException if the federation has already a member with the same location
	 */
	public void addEndpoint(Endpoint e, boolean... updateStrategy) throws FedXRuntimeException {
		log.info("Adding endpoint " + e.getId() + " to federation ...");

		/* check for duplicate before adding: heuristic => same location */
		for (Endpoint member : federation.getMembers())
			if (member.getEndpoint().equals(e.getEndpoint()))
				throw new FedXRuntimeException("Adding failed: there exists already an endpoint with location "
						+ e.getEndpoint() + " (eid=" + member.getId() + ")");

		federation.addMember(e);
		federationContext.getEndpointManager().addEndpoint(e);

		if (updateStrategy == null || updateStrategy.length == 0
				|| (updateStrategy.length == 1 && updateStrategy[0] == true))
			updateStrategy();
	}

	/**
	 * Add the specified endpoints to the federation and take care for updating all structures.
	 * 
	 * @param endpoints a list of endpoints to add
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
		federationContext.getEndpointManager().removeEndpoint(e);

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

		for (Endpoint e : new ArrayList<>(federation.getMembers())) {
			removeEndpoint(e, false);
		}

		updateStrategy();
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

		log.info("Shutting down federation and all underlying repositories ...");
		// Abort all running queries
		federationContext.getQueryManager().shutdown();
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
		federationContext.getFederatedServiceResolver().shutDown();
		federationContext.getCache().persist();
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
		FederationEvalStrategy strategy = getStrategy();
		if (type == FederationType.LOCAL)
			return new SynchronousWorkerUnion<>(strategy, queryInfo);
		return new ControlledWorkerUnion<>(strategy, unionScheduler, queryInfo);

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
			strategy = FederationEvaluationStrategyFactory.getEvaluationStrategy(type, federationContext);
			log.info("Federation updated. Type: " + type + ", evaluation strategy is "
					+ strategy.getClass().getSimpleName());
		}

	}

}
