/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.dataset.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.dataset.DatasetRepository;

/**
 * A {@link RepositoryFactory} that creates {@link DatasetRepository}s based on RDF configuration data.
 *
 * @author Arjohn Kampman
 */
public class DatasetRepositoryFactory implements RepositoryFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:DatasetRepository";

	/**
	 * Returns the repository's type: <var>openrdf:DatasetRepository</var>.
	 */
	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new DatasetRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		if (config instanceof DatasetRepositoryConfig) {
			return new DatasetRepository();
		}

		throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
	}
}
