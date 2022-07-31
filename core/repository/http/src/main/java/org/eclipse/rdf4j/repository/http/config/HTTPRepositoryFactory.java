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
package org.eclipse.rdf4j.repository.http.config;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

/**
 * A {@link RepositoryFactory} that creates {@link HTTPRepository}s based on RDF configuration data.
 *
 * @author Arjohn Kampman
 */
public class HTTPRepositoryFactory implements RepositoryFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see RepositoryFactory#getRepositoryType()
	 */
	public static final String REPOSITORY_TYPE = "openrdf:HTTPRepository";

	/**
	 * Returns the repository's type: <var>openrdf:HTTPRepository</var>.
	 */
	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new HTTPRepositoryConfig();
	}

	@Override
	public Repository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		HTTPRepository result;

		if (config instanceof HTTPRepositoryConfig) {
			HTTPRepositoryConfig httpConfig = (HTTPRepositoryConfig) config;
			result = new HTTPRepository(httpConfig.getURL());
			// result.setUsernameAndPassword(httpConfig.getUsername(), httpConfig.getPassword());
		} else {
			throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
		}
		return result;
	}
}
