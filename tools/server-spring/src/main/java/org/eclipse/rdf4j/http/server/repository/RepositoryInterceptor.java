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

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.ServerInterceptor;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor for repository requests. Should not be a singleton bean! Configure as inner bean in openrdf-servlet.xml
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class RepositoryInterceptor extends ServerInterceptor {

	/*-----------*
	 * Constants *
	 *-----------*/

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final String REPOSITORY_ID_KEY = "repositoryID";

	private static final String REPOSITORY_KEY = "repository";

	/*-----------*
	 * Variables *
	 *-----------*/

	private volatile RepositoryManager repositoryManager;

	private volatile String repositoryID;

	/*---------*
	 * Methods *
	 *---------*/

	public void setRepositoryManager(RepositoryManager repMan) {
		repositoryManager = Objects.requireNonNull(repMan, "Repository manager was null");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse respons, Object handler) throws Exception {
		String pathInfoStr = request.getPathInfo();
		logger.debug("path info: {}", pathInfoStr);

		repositoryID = null;

		if (pathInfoStr != null && !pathInfoStr.equals("/")) {
			String[] pathInfo = pathInfoStr.substring(1).split("/");
			if (pathInfo.length > 0) {
				repositoryID = pathInfo[0];
				logger.debug("repositoryID is '{}'", repositoryID);
			}
		}

		ProtocolUtil.logRequestParameters(request);

		return super.preHandle(request, respons, handler);
	}

	@Override
	protected String getThreadName() {
		String threadName = Protocol.REPOSITORIES;

		String nextRepositoryID = repositoryID;
		if (nextRepositoryID != null) {
			threadName += "/" + nextRepositoryID;
		}

		return threadName;
	}

	@Override
	protected void setRequestAttributes(HttpServletRequest request) throws ClientHTTPException, ServerHTTPException {
		String nextRepositoryID = repositoryID;
		if (RepositoryConfigRepository.ID.equals(nextRepositoryID)) {
			request.setAttribute(REPOSITORY_ID_KEY, nextRepositoryID);
			request.setAttribute(REPOSITORY_KEY, new RepositoryConfigRepository(repositoryManager));
		} else if (nextRepositoryID != null) {
			try {
				Repository repository = repositoryManager.getRepository(nextRepositoryID);
				if (repository == null && !"PUT".equals(request.getMethod())) {
					throw new ClientHTTPException(SC_NOT_FOUND, "Unknown repository: " + nextRepositoryID);
				}

				request.setAttribute(REPOSITORY_ID_KEY, nextRepositoryID);
				request.setAttribute(REPOSITORY_KEY, repository);
			} catch (RepositoryConfigException | RepositoryException e) {
				throw new ServerHTTPException(e.getMessage(), e);
			}
		}
	}

	public static String getRepositoryID(HttpServletRequest request) {
		return (String) request.getAttribute(REPOSITORY_ID_KEY);
	}

	public static Repository getRepository(HttpServletRequest request) {
		return (Repository) request.getAttribute(REPOSITORY_KEY);
	}

	/**
	 * Obtain a new {@link RepositoryConnection} with suitable parser/writer configuration for handling the incoming
	 * HTTP request. The caller of this method is responsible for closing the connection.
	 *
	 * @param request the {@link HttpServletRequest} for which a {@link RepositoryConnection} is to be returned
	 * @return a configured {@link RepositoryConnection}
	 */
	public static RepositoryConnection getRepositoryConnection(HttpServletRequest request) {
		Repository repo = getRepository(request);
		RepositoryConnection conn = repo.getConnection();
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
		conn.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
		return conn;
	}
}
