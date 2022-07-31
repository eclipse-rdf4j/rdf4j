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
package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.http.server.repository.handler.DefaultQueryRequestHandler;
import org.eclipse.rdf4j.http.server.repository.handler.DefaultRepositoryRequestHandler;
import org.eclipse.rdf4j.http.server.repository.handler.QueryRequestHandler;
import org.eclipse.rdf4j.http.server.repository.handler.RepositoryRequestHandler;
import org.eclipse.rdf4j.http.server.repository.resolver.DefaultRepositoryResolver;
import org.eclipse.rdf4j.http.server.repository.resolver.RepositoryResolver;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;

/**
 * Handles queries and admin (delete) operations on a repository and renders the results in a format suitable to the
 * type of operation.
 *
 * @author Herko ter Horst
 */
public class RepositoryController extends AbstractRepositoryController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private QueryRequestHandler queryRequestHandler;
	private RepositoryRequestHandler repositoryRequestHandler;

	public RepositoryController() throws ApplicationContextException {
	}

	public void setRepositoryManager(RepositoryManager repMan) {
		if (logger.isDebugEnabled()) {
			logger.debug("setRepositoryManager {}", repMan);
		}

		RepositoryResolver repositoryResolver = new DefaultRepositoryResolver(repMan);
		queryRequestHandler = new DefaultQueryRequestHandler(repositoryResolver);
		repositoryRequestHandler = new DefaultRepositoryRequestHandler(repositoryResolver);
	}

	@Override
	protected QueryRequestHandler getQueryRequestHandler() {
		return queryRequestHandler;
	}

	@Override
	protected RepositoryRequestHandler getRepositoryRequestHandler() {
		return repositoryRequestHandler;
	}

}
