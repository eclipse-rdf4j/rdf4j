/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

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
 * Interceptor for repository requests. Handles the opening and closing of
 * connections to the repository specified in the request. Should not be a
 * singleton bean! Configure as inner bean in openrdf-servlet.xml
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

	private static final String REPOSITORY_CONNECTION_KEY = "repositoryConnection";

	/*-----------*
	 * Variables *
	 *-----------*/

	private RepositoryManager repositoryManager;

	private String repositoryID;

	private RepositoryConnection repositoryCon;

	/*---------*
	 * Methods *
	 *---------*/

	public void setRepositoryManager(RepositoryManager repMan) {
		repositoryManager = repMan;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse respons, Object handler)
		throws Exception
	{
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

		if (repositoryID != null) {
			threadName += "/" + repositoryID;
		}

		return threadName;
	}

	@Override
	protected void setRequestAttributes(HttpServletRequest request)
		throws ClientHTTPException, ServerHTTPException
	{
		if (repositoryID != null) {
			try {
				Repository repository = repositoryManager.getRepository(repositoryID);

				if (repository == null) {
					throw new ClientHTTPException(SC_NOT_FOUND, "Unknown repository: " + repositoryID);
				}

				repositoryCon = repository.getConnection();

				// SES-1834 by default, the Sesame server should not treat datatype or language value verification errors
				// as fatal. This is to be graceful, by default, about accepting "dirty" data.
				// FIXME SES-1833 this should be configurable by the user.
				repositoryCon.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
				repositoryCon.getParserConfig().addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
				
				// FIXME: hack for repositories that return connections that are not
				// in auto-commit mode by default
				if (!repositoryCon.isAutoCommit()) {
					repositoryCon.setAutoCommit(true);
				}

				request.setAttribute(REPOSITORY_ID_KEY, repositoryID);
				request.setAttribute(REPOSITORY_KEY, repository);
				request.setAttribute(REPOSITORY_CONNECTION_KEY, repositoryCon);
			}
			catch (RepositoryConfigException e) {
				throw new ServerHTTPException(e.getMessage(), e);
			}
			catch (RepositoryException e) {
				throw new ServerHTTPException(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void cleanUpResources()
		throws ServerHTTPException
	{
		if (repositoryCon != null) {
			try {
				repositoryCon.close();
			}
			catch (RepositoryException e) {
				throw new ServerHTTPException(e.getMessage(), e);
			}
		}
	}

	public static String getRepositoryID(HttpServletRequest request) {
		return (String)request.getAttribute(REPOSITORY_ID_KEY);
	}

	public static Repository getRepository(HttpServletRequest request) {
		return (Repository)request.getAttribute(REPOSITORY_KEY);
	}

	public static RepositoryConnection getRepositoryConnection(HttpServletRequest request) {
		return (RepositoryConnection)request.getAttribute(REPOSITORY_CONNECTION_KEY);
	}
}
