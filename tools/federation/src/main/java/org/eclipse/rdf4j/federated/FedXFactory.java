/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.EndpointFactory;
import org.eclipse.rdf4j.federated.endpoint.provider.NativeRepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.ResolvableRepositoryInformation;
import org.eclipse.rdf4j.federated.endpoint.provider.SPARQLRepositoryInformation;
import org.eclipse.rdf4j.federated.evaluation.FederationEvaluationStrategyFactory;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.repository.FedXRepository;
import org.eclipse.rdf4j.federated.write.DefaultWriteStrategyFactory;
import org.eclipse.rdf4j.federated.write.WriteStrategyFactory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.sail.Sail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FedX initialization factory methods for convenience: methods initialize the {@link FederationManager} and all
 * required FedX structures. See {@link FederationManager} for some a code snippet.
 *
 * <p>
 * Use the {@link FedXFactory#newFederation()} builder to create an advanced and customized federation
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class FedXFactory {

	protected static final Logger log = LoggerFactory.getLogger(FedXFactory.class);

	/**
	 * Create a federation with the provided sparql endpoints
	 *
	 * @param sparqlEndpoints the list of SPARQL endpoints
	 *
	 * @return the configured FedX federation {@link Sail} wrapped in a {@link FedXRepository}
	 *
	 * @throws Exception
	 */
	public static FedXRepository createSparqlFederation(
			List<String> sparqlEndpoints) throws Exception {
		return newFederation().withSparqlEndpoints(sparqlEndpoints).create();
	}

	/**
	 * Create the federation with a specified data source configuration file (*.ttl). Federation members are constructed
	 * from the data source configuration. Sample data source configuration files can be found in the documentation.
	 *
	 * @param dataConfig the location of the data source configuration
	 *
	 * @return the configured FedX federation {@link Sail} wrapped in a {@link FedXRepository}
	 *
	 * @throws Exception
	 */
	public static FedXRepository createFederation(File dataConfig)
			throws Exception {
		return newFederation().withMembers(dataConfig).create();
	}

	/**
	 * Create the federation by providing the endpoints to add. The fedx configuration can provide information about the
	 * dataConfig to be used which may contain the default federation members.
	 * <p>
	 *
	 * @param endpoints additional endpoints to be added, may be null or empty
	 *
	 * @return the configured FedX federation {@link Sail} wrapped in a {@link FedXRepository}
	 *
	 * @throws FedXException
	 */
	public static FedXRepository createFederation(
			List<Endpoint> endpoints) throws FedXException {

		return newFederation().withMembers(endpoints).create();
	}

	/**
	 * Create a new customizable FedX federation. Once all configuration is supplied, the Federation can be created
	 * using {@link #create()}
	 *
	 * @return the {@link FedXFactory} builder
	 */
	public static FedXFactory newFederation() {
		return new FedXFactory();
	}

	protected RepositoryResolver repositoryResolver;
	protected FederatedServiceResolver federatedServiceResolver;
	protected FederationEvaluationStrategyFactory strategyFactory;
	protected WriteStrategyFactory writeStrategyFactory;
	protected List<Endpoint> members = new ArrayList<>();
	protected FedXConfig config = FedXConfig.DEFAULT_CONFIG;
	protected File fedxBaseDir;

	private FedXFactory() {

	}

	public FedXFactory withRepositoryResolver(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
		return this;
	}

	public FedXFactory withFederatedServiceResolver(FederatedServiceResolver federatedServiceResolver) {
		this.federatedServiceResolver = federatedServiceResolver;
		return this;
	}

	public FedXFactory withFederationEvaluationStrategyFactory(FederationEvaluationStrategyFactory strategyFactory) {
		this.strategyFactory = strategyFactory;
		return this;
	}

	/**
	 * Specify the {@link WriteStrategyFactory} to be used. If not explicitly set, {@link DefaultWriteStrategyFactory}
	 * is used.
	 *
	 * @param writeStrategyFactory the {@link WriteStrategyFactory} to be used.
	 * @return this factory
	 */
	public FedXFactory withWriteStrategyFactory(WriteStrategyFactory writeStrategyFactory) {
		this.writeStrategyFactory = writeStrategyFactory;
		return this;
	}

	public FedXFactory withMembers(List<Endpoint> endpoints) {
		members.addAll(endpoints);
		return this;
	}

	public FedXFactory withMembers(File dataConfig) {
		log.info("Loading federation members from dataConfig " + dataConfig + ".");
		members.addAll(EndpointFactory.loadFederationMembers(dataConfig, fedxBaseDir));
		return this;
	}

	/**
	 * Initialize the federation with members from the model.
	 * <p>
	 * Currently the types NativeStore, ResolvableEndpoint and SPARQLEndpoint are supported. For details please refer to
	 * the documentation in {@link NativeRepositoryInformation}, {@link ResolvableRepositoryInformation} and
	 * {@link SPARQLRepositoryInformation}.
	 * </p>
	 *
	 * @param model the model defining the federation members
	 * @return the factory
	 */
	public FedXFactory withMembers(Model model) {
		log.debug("Loading federation members from model.");
		members.addAll(EndpointFactory.loadFederationMembers(model, fedxBaseDir));
		return this;
	}

	public FedXFactory withSparqlEndpoint(String sparqlEndpoint) {
		members.add(EndpointFactory.loadSPARQLEndpoint(sparqlEndpoint));
		return this;
	}

	public FedXFactory withSparqlEndpoints(List<String> sparqlEndpoints) {
		for (String sparqlEndpoint : sparqlEndpoints) {
			withSparqlEndpoint(sparqlEndpoint);
		}
		return this;
	}

	public FedXFactory withResolvableEndpoint(String repositoryId) {
		return withResolvableEndpoint(repositoryId, false);
	}

	public FedXFactory withResolvableEndpoint(String repositoryId, boolean writable) {
		members.add(EndpointFactory.loadResolvableRepository(repositoryId, writable));
		return this;
	}

	public FedXFactory withConfig(FedXConfig config) {
		this.config = config;
		return this;
	}

	/**
	 * Configure the FedX base directory at federation construction time.
	 *
	 * @param fedxBaseDir the fedx base directory
	 * @return the {@link FedXFactory} instance
	 */
	public FedXFactory withFedXBaseDir(File fedxBaseDir) {
		this.fedxBaseDir = fedxBaseDir;
		return this;
	}

	/**
	 * Create the federation using the provided configuration
	 *
	 * @return the configured {@link FedXRepository}
	 */
	public FedXRepository create() {
		if (members.isEmpty()) {
			log.info("Initializing federation without any pre-configured members");
		}

		FedX federation = new FedX(members);
		if (this.strategyFactory != null) {
			federation.setFederationEvaluationStrategy(strategyFactory);
		}
		if (this.writeStrategyFactory != null) {
			federation.setWriteStrategyFactory(writeStrategyFactory);
		}

		FedXRepository repo = new FedXRepository(federation, this.config);
		if (this.repositoryResolver != null) {
			repo.setRepositoryResolver(repositoryResolver);
		}
		if (this.federatedServiceResolver != null) {
			repo.setFederatedServiceResolver(federatedServiceResolver);
		}
		if (this.fedxBaseDir != null) {
			repo.setDataDir(fedxBaseDir);
		}

		return repo;
	}
}
