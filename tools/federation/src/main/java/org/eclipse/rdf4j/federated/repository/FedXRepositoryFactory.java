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
package org.eclipse.rdf4j.federated.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RepositoryFactory} to use FedX in settings with a repository manager, e.g. in the RDF4J workbench.
 * </p>
 *
 * <p>
 * See {@link FedXRepositoryConfig} for the configuration.
 * </p>
 *
 * @author Andreas Schwarte
 * @see FedXRepositoryConfig
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
		return new FedXRepositoryWrapper(fedXConfig);
	}

}
