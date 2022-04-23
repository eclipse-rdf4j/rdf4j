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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointClassification;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategyFactory;
import org.eclipse.rdf4j.federated.evaluation.SailFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.SparqlFederationEvalStrategy;
import org.eclipse.rdf4j.federated.evaluation.concurrent.ControlledWorkerScheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.NamingThreadFactory;
import org.eclipse.rdf4j.federated.evaluation.concurrent.Scheduler;
import org.eclipse.rdf4j.federated.evaluation.concurrent.TaskWrapper;
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
	public enum FederationType {
		LOCAL,
		REMOTE,
		HYBRID
	}

	/* Instance variables */
	private FederationContext federationContext;
	private FedX federation;
	private ExecutorService executor;
	private FederationType federationType;
	private ControlledWorkerScheduler<BindingSet> joinScheduler;
	private ControlledWorkerScheduler<BindingSet> leftJoinScheduler;
	private ControlledWorkerScheduler<BindingSet> unionScheduler;

	public FederationManager() {

	}

	public void init(FedX federation, FederationContext federationContext) {
		this.federation = federation;
		this.federationContext = federationContext;
		this.executor = Executors.newCachedThreadPool(new NamingThreadFactory("FedX Executor"));

		updateFederationType();
		reset();
	}

	/**
	 *
	 * @return the initialized and configured {@link FederationEvaluationStrategyFactory}
	 */
	/* package */ FederationEvaluationStrategyFactory getFederationEvaluationStrategyFactory() {
		FederationEvaluationStrategyFactory strategyFactory = federation.getFederationEvaluationStrategyFactory();
		strategyFactory.setFederationType(federationType);
		strategyFactory.setFederationContext(federationContext);
		return strategyFactory;
	}

	/**
	 * Reset the {@link Scheduler} instances, i.e. abort all running threads and create a new scheduler instance.
	 */
	public void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Scheduler for join and union are reset.");
		}

		Optional<TaskWrapper> taskWrapper = federationContext.getConfig().getTaskWrapper();
		if (joinScheduler != null) {
			joinScheduler.abort();
		}
		joinScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getJoinWorkerThreads(),
				"Join Scheduler");
		taskWrapper.ifPresent(joinScheduler::setTaskWrapper);

		if (unionScheduler != null) {
			unionScheduler.abort();
		}
		unionScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getUnionWorkerThreads(),
				"Union Scheduler");
		taskWrapper.ifPresent(unionScheduler::setTaskWrapper);

		if (leftJoinScheduler != null) {
			leftJoinScheduler.abort();
		}
		leftJoinScheduler = new ControlledWorkerScheduler<>(federationContext.getConfig().getLeftJoinWorkerThreads(),
				"Left Join Scheduler");
		taskWrapper.ifPresent(leftJoinScheduler::setTaskWrapper);

	}

	/**
	 * Returns the managed {@link Executor} which takes for properly handling any configured
	 * {@link FedXConfig#getTaskWrapper()}
	 *
	 */
	public Executor getExecutor() {
		final Optional<TaskWrapper> taskWrapper = federationContext.getConfig().getTaskWrapper();
		return (runnable) -> {

			// Note: for specific use-cases the runnable may be wrapped (e.g. to allow injection of thread-contexts). By
			// default the unmodified runnable is returned from the task wrapper
			Runnable wrappedRunnable = taskWrapper.map(tw -> tw.wrap(runnable)).orElse(runnable);

			executor.execute(wrappedRunnable);
		};
	}

	public FedX getFederation() {
		return this.federation;
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
		return federationType;
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
		for (Endpoint member : federation.getMembers()) {
			if (member.getEndpoint().equals(e.getEndpoint())) {
				throw new FedXRuntimeException("Adding failed: there exists already an endpoint with location "
						+ e.getEndpoint() + " (eid=" + member.getId() + ")");
			}
		}

		federation.addMember(e);
		federationContext.getEndpointManager().addEndpoint(e);

		if (updateStrategy == null || updateStrategy.length == 0
				|| (updateStrategy.length == 1 && updateStrategy[0] == true)) {
			updateFederationType();
		}
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

		updateFederationType();
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
		if (!federation.getMembers().contains(e)) {
			throw new FedXRuntimeException("Endpoint " + e.getId() + " is not a member of the current federation.");
		}

		federation.removeMember(e);
		federationContext.getEndpointManager().removeEndpoint(e);

		if (updateStrategy == null || updateStrategy.length == 0
				|| (updateStrategy.length == 1 && updateStrategy[0] == true)) {
			updateFederationType();
		}
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

		updateFederationType();
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
		if (federationType == FederationType.LOCAL) {
			return new SynchronousWorkerUnion<>(queryInfo);
		}
		return new ControlledWorkerUnion<>(unionScheduler, queryInfo);

	}

	/**
	 * Update the federation evaluation strategy using the classification of endpoints as provided by
	 * {@link Endpoint#getEndpointClassification()}:
	 * <p>
	 * Which strategy is applied depends on the {@link FederationEvaluationStrategyFactory}, see
	 * {@link #getFederationEvaluationStrategyFactory()}.
	 * </p>
	 *
	 * Default strategies:
	 * <ul>
	 * <li>local federation: {@link SailFederationEvalStrategy}</li>
	 * <li>endpoint federation: {@link SparqlFederationEvalStrategy}</li>
	 * <li>hybrid federation: {@link SparqlFederationEvalStrategy}</li>
	 * </ul>
	 *
	 */
	private void updateFederationType() {

		int localCount = 0, remoteCount = 0;
		for (Endpoint e : federation.getMembers()) {
			if (e.getEndpointClassification() == EndpointClassification.Remote) {
				remoteCount++;
			} else {
				localCount++;
			}
		}

		boolean updated = false;
		if (remoteCount == 0) {
			if (federationType != FederationType.LOCAL) {
				federationType = FederationType.LOCAL;
				updated = true;
			}
		} else if (localCount == 0) {
			if (federationType != FederationType.REMOTE) {
				federationType = FederationType.REMOTE;
				updated = true;
			}
		} else {
			if (federationType != FederationType.HYBRID) {
				federationType = FederationType.HYBRID;
				updated = true;
			}
		}

		if (updated) {
			log.info("Federation updated. Type: " + federationType);
		}

	}

}
