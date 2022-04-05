/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.handler;

import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.resolver.RepositoryResolver;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

public class DefaultRepositoryRequestHandler implements RepositoryRequestHandler {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final RepositoryResolver repositoryResolver;

	public DefaultRepositoryRequestHandler(RepositoryResolver repositoryResolver) {
		this.repositoryResolver = repositoryResolver;
	}

	public ModelAndView handleDeleteRepositoryRequest(HttpServletRequest request) throws HTTPException {
		String repId = repositoryResolver.getRepositoryID(request);
		logger.info("DELETE request invoked for repository '" + repId + "'");

		if (request.getParameter(QUERY_PARAM_NAME) != null) {
			logger.warn("query supplied on repository delete request, aborting delete");
			throw new HTTPException(HttpStatus.SC_BAD_REQUEST,
					"Repository delete error: query supplied with request");
		}

		try {
			boolean success = repositoryResolver.getRepositoryManager().removeRepository(repId);
			if (success) {
				logger.info("DELETE request successfully completed");
				return new ModelAndView(EmptySuccessView.getInstance());
			} else {
				logger.error("error while attempting to delete repository '" + repId + "'");
				throw new HTTPException(HttpStatus.SC_BAD_REQUEST,
						"could not locate repository configuration for repository '" + repId + "'.");
			}
		} catch (RDF4JException e) {
			logger.error("error while attempting to delete repository '" + repId + "'", e);
			throw new ServerHTTPException("Repository delete error: " + e.getMessage(), e);
		}
	}

	public ModelAndView handleCreateOrUpdateRepositoryRequest(HttpServletRequest request)
			throws IOException, HTTPException {
		// create new repo
		String repId = repositoryResolver.getRepositoryID(request);
		logger.info("PUT request invoked for repository '" + repId + "'");
		try {
			if (repositoryResolver.getRepositoryManager().hasRepositoryConfig(repId)) {
				ErrorInfo errorInfo = new ErrorInfo(ErrorType.REPOSITORY_EXISTS,
						"repository already exists: " + repId);
				throw new ClientHTTPException(HttpStatus.SC_CONFLICT, errorInfo.toString());
			}
			Model model = Rio.parse(request.getInputStream(), "",
					Rio.getParserFormatForMIMEType(request.getContentType())
							.orElseThrow(() -> new HTTPException(HttpStatus.SC_BAD_REQUEST,
									"unrecognized content type " + request.getContentType())));

			RepositoryConfig config = repositoryResolver.getRepositoryConfig(repId, model);

			if (config == null) {
				throw new RepositoryConfigException("could not read repository config from supplied data");
			}
			repositoryResolver.getRepositoryManager().addRepositoryConfig(config);

			return new ModelAndView(EmptySuccessView.getInstance());
		} catch (RepositoryConfigException e) {
			ErrorInfo errorInfo = new ErrorInfo(ErrorType.MALFORMED_DATA,
					"Supplied repository configuration is invalid: " + e.getMessage());
			throw new ClientHTTPException(HttpStatus.SC_BAD_REQUEST, errorInfo.toString());
		} catch (RDF4JException e) {
			logger.error("error while attempting to create/configure repository '" + repId + "'", e);
			throw new ServerHTTPException("Repository create error: " + e.getMessage(), e);
		}
	}

}
