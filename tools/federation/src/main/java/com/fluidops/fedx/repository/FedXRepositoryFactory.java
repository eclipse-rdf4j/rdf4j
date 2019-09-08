/*
 * Copyright (C) 2019 Veritas Technologies LLC.
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
package com.fluidops.fedx.repository;

import java.io.File;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.endpoint.ResolvableEndpoint;
import com.fluidops.fedx.exception.FedXException;

/**
 * A {@link RepositoryFactory} to use FedX in the RDF4J workbench. See
 * {@link FedXRepositoryConfig} for the configuration.
 * 
 * <p>
 * Note that this initialization obtains a {@link RepositoryResolver} from
 * {@link FedXRepositoryResolverBean} (if any). This is used for the
 * initialization of all {@link ResolvableEndpoint}s via
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

				File dataConfigFile = null;
				if (fedXConfig.getDataConfig() != null) {
					dataConfigFile = new File(baseDir, fedXConfig.getDataConfig());
				} else if (Config.getConfig().getDataConfig() != null) {
					dataConfigFile = new File(baseDir, Config.getConfig().getDataConfig());
				}


				if (dataConfigFile == null) {
					throw new RepositoryException(
							"No data config provided, neither explicitly nor via fedx configuration");
				}

				FedXRepository fedxRepo;
				try {
					// apply a repository resolver (if any) from FedXRepositoryResolverBean
					fedxRepo = FedXFactory.newFederation()
							.withFedXBaseDir(baseDir)
							.withRepositoryResolver(FedXRepositoryResolverBean.getRepositoryResolver())
							.withMembers(dataConfigFile).create();
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
