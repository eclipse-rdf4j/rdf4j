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
package org.eclipse.rdf4j.repository.sparql.config;

import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

/**
 * Creates {@link SPARQLRepository} from a configuration.
 *
 * @author James Leigh
 */
public class SPARQLRepositoryFactory implements RepositoryFactory {

	public static final String REPOSITORY_TYPE = "openrdf:SPARQLRepository";

	@Override
	public String getRepositoryType() {
		return REPOSITORY_TYPE;
	}

	@Override
	public RepositoryImplConfig getConfig() {
		return new SPARQLRepositoryConfig();
	}

	@Override
	public SPARQLRepository getRepository(RepositoryImplConfig config) throws RepositoryConfigException {
		SPARQLRepository result;

		if (config instanceof SPARQLRepositoryConfig) {
			SPARQLRepositoryConfig httpConfig = (SPARQLRepositoryConfig) config;
			if (httpConfig.getUpdateEndpointUrl() != null) {
				result = new SPARQLRepository(httpConfig.getQueryEndpointUrl(), httpConfig.getUpdateEndpointUrl());
			} else {
				result = new SPARQLRepository(httpConfig.getQueryEndpointUrl());
			}
			if (httpConfig.getPassThroughEnabled() != null) {
				result.setPassThroughEnabled(httpConfig.getPassThroughEnabled());
			}
		} else {
			throw new RepositoryConfigException("Invalid configuration class: " + config.getClass());
		}
		return result;
	}
}
