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
package org.eclipse.rdf4j.repository.contextaware.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.contextaware.ContextAwareRepository;

/**
 * A {@link RepositoryFactory} that creates {@link ContextAwareRepository}s based on RDF configuration data.
 *
 * @author James Leigh
 */
public class ContextAwareFactory implements RepositoryFactory {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:ContextAwareRepository";

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Returns the repository's type: <var>openrdf:ContextAwareRepository</var>.
	 */
	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new ContextAwareConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig configuration) throws RepositoryConfigException {
		if (configuration instanceof ContextAwareConfig) {
			ContextAwareConfig config = (ContextAwareConfig) configuration;

			ContextAwareRepository repo = new ContextAwareRepository();

			repo.setIncludeInferred(config.isIncludeInferred());
			repo.setMaxQueryTime(config.getMaxQueryTime());
			repo.setQueryLanguage(config.getQueryLanguage());
			repo.setBaseURI(config.getBaseURI());
			repo.setReadContexts(config.getReadContexts());
			repo.setAddContexts(config.getAddContexts());
			repo.setRemoveContexts(config.getRemoveContexts());
			repo.setArchiveContexts(config.getArchiveContexts());
			repo.setInsertContext(config.getInsertContext());

			return repo;
		}

		throw new RepositoryConfigException("Invalid configuration class: " + configuration.getClass());
	}
}
