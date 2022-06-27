/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import java.io.File;

import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.RepositoryResolverClient;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;

/**
 * Wrapper for the {@link FedXRepository} in order to allow for lazy initialization.
 *
 * <p>
 * The wrapper is used from {@link FedXRepositoryFactory} in environments with a {@link RepositoryManager}, e.g. in the
 * RDF4J workbench. The background is that the RDF4J repository manager requires control over the repository instance.
 * </p>
 *
 * <p>
 * The data directory and the {@link RepositoryResolver} are handled by RDF4J {@link RepositoryManager}.
 * </p>
 *
 * @author Andreas Schwarte
 * @see FedXFactory
 *
 */
public class FedXRepositoryWrapper extends RepositoryWrapper
		implements RepositoryResolverClient, FederatedServiceResolverClient {

	private final FedXRepositoryConfig fedXConfig;

	private File dataDir;

	private RepositoryResolver repositoryResolver;

	private FederatedServiceResolver serviceResolver;

	public FedXRepositoryWrapper(FedXRepositoryConfig fedXConfig) {
		super();
		this.fedXConfig = fedXConfig;
	}

	@Override
	public void setDataDir(File dataDir) {
		this.dataDir = dataDir;
	}

	@Override
	public File getDataDir() {
		return dataDir;
	}

	@Override
	public boolean isInitialized() {
		if (getDelegate() == null) {
			return false;
		}
		return super.isInitialized();
	}

	@Override
	public void init() throws RepositoryException {

		if (getDelegate() != null) {
			return;
		}

		FedXRepository fedxRepo;
		try {
			FedXFactory factory = createFactory();

			fedxRepo = factory.create();

			fedxRepo.init();
		} catch (Exception e) {
			throw new RepositoryException(e);
		}
		setDelegate(fedxRepo);
	}

	/**
	 * Create the initialized {@link FedXFactory}
	 *
	 * @return
	 */
	protected FedXFactory createFactory() {

		File baseDir = getDataDir();
		if (baseDir == null) {
			baseDir = new File(".");
		}

		// explicit federation members model
		Model members = fedXConfig.getMembers();

		// optional data config
		File dataConfigFile = null;
		if (fedXConfig.getDataConfig() != null) {
			dataConfigFile = new File(baseDir, fedXConfig.getDataConfig());
		}

		if (members == null && dataConfigFile == null) {
			throw new RepositoryException(
					"No federation members defined: neither explicitly nor via data config.");
		}

		// apply a repository resolver (if any) set from RepositoryManager
		FedXFactory factory = FedXFactory.newFederation()
				.withRepositoryResolver(repositoryResolver)
				.withFederatedServiceResolver(serviceResolver)
				.withFedXBaseDir(baseDir);

		if (dataConfigFile != null) {
			factory.withMembers(dataConfigFile);
		}

		if (members != null) {
			factory.withMembers(members);
		}

		if (fedXConfig.getConfig() != null) {
			factory.withConfig(fedXConfig.getConfig());
		}

		return factory;
	}

	@Override
	public void shutDown() throws RepositoryException {
		if (!isInitialized()) {
			return;
		}
		super.shutDown();
		setDelegate(null);
	}

	@Override
	public void setRepositoryResolver(RepositoryResolver resolver) {
		this.repositoryResolver = resolver;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}
}
