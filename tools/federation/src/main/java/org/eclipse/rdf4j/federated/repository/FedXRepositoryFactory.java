/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.repository;

import java.io.File;

import org.eclipse.rdf4j.federated.Config;
import org.eclipse.rdf4j.federated.FedXFactory;
import org.eclipse.rdf4j.federated.endpoint.ResolvableEndpoint;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RepositoryFactory} to use FedX in the RDF4J workbench. See {@link FedXRepositoryConfig} for the
 * configuration.
 * 
 * <p>
 * Note that this initialization obtains a {@link RepositoryResolver} from {@link FedXRepositoryResolverBean} (if any).
 * This is used for the initialization of all {@link ResolvableEndpoint}s via
 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}.
 * </p>
 * 
 * @author Andreas Schwarte
 * @see FedXRepositoryConfig
 * @see FedXRepositoryResolverBean
 *
 */
public class FedXRepositoryFactory implements RepositoryFactory {

	public static final String REPOSITORY_TYPE = "fedx:FedXRepository";

	protected static final Logger log = LoggerFactory.getLogger(FedXRepositoryFactory.class);

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public FedXRepositoryConfig getConfig() {
		return new FedXRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {

		if (!(config instanceof FedXRepositoryConfig)) {
			throw new RepositoryConfigException("Unexpected configuration type: " + config.getClass());
		}

		FedXRepositoryConfig fedXConfig = (FedXRepositoryConfig) config;

		log.info("Configuring FedX for the RDF4J repository manager");

		// wrap the FedX Repository in order to allow lazy initialization
		// => RDF4J repository manager requires control over the repository instance
		// Note: data dir is handled by RDF4J repository manager and used as a
		// base directory.
		return new RepositoryWrapper() {

			File dataDir;

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
			public void initialize() throws RepositoryException {

				if (getDelegate() != null) {
					super.initialize();
					return;
				}

				File baseDir = getDataDir();
				if (baseDir == null) {
					baseDir = new File(".");
				}
				try {
					if (fedXConfig.getFedxConfig() != null) {
						Config.initialize(new File(baseDir, fedXConfig.getFedxConfig()));
					} else {
						Config.initialize();
					}
				} catch (FedXException e) {
					throw new RepositoryException("Failed to initialize config: " + e.getMessage(), e);
				}

				// explicit federation members model
				Model members = fedXConfig.getMembers();

				// optional data config
				File dataConfigFile = null;
				if (fedXConfig.getDataConfig() != null) {
					dataConfigFile = new File(baseDir, fedXConfig.getDataConfig());
				} else if (Config.getConfig().getDataConfig() != null) {
					dataConfigFile = new File(baseDir, Config.getConfig().getDataConfig());
				}

				if (members == null && dataConfigFile == null) {
					throw new RepositoryException(
							"No federation members defined: neither explicitly nor via data config.");
				}

				FedXRepository fedxRepo;
				try {
					// apply a repository resolver (if any) from FedXRepositoryResolverBean
					FedXFactory factory = FedXFactory.newFederation()
							.withFedXBaseDir(baseDir)
							.withRepositoryResolver(FedXRepositoryResolverBean.getRepositoryResolver());

					if (dataConfigFile != null) {
						factory.withMembers(dataConfigFile);
					}

					if (members != null) {
						factory.withMembers(members);
					}

					fedxRepo = factory.create();
				} catch (Exception e) {
					throw new RepositoryException(e);
				}
				setDelegate(fedxRepo);
				super.initialize();
			}

			@Override
			public void shutDown() throws RepositoryException {
				if (!isInitialized()) {
					return;
				}
				super.shutDown();
				setDelegate(null);
			}
		};
	}

}
