/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.repository.handler.QueryRequestHandler;
import org.eclipse.rdf4j.http.server.repository.handler.RepositoryRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public abstract class AbstractRepositoryController extends AbstractController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public AbstractRepositoryController() throws ApplicationContextException {
		setSupportedMethods(METHOD_GET, METHOD_POST, "PUT", "DELETE", METHOD_HEAD);
	}

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		logger.debug("Request method: {}", request.getMethod());

		RequestMethod requestMethod = RequestMethod.valueOf(request.getMethod());

		switch (requestMethod) {
		case DELETE: {
			logger.debug("handleDeleteRepositoryRequest");
			return getRepositoryRequestHandler().handleDeleteRepositoryRequest(request);
		}
		case PUT: {
			logger.debug("handleCreateOrUpdateRepositoryRequest");
			return getRepositoryRequestHandler().handleCreateOrUpdateRepositoryRequest(request);
		}
		}

		logger.debug("handleQueryRequest");

		return getQueryRequestHandler().handleQueryRequest(request, requestMethod, response);
	}

	protected abstract QueryRequestHandler getQueryRequestHandler();

	protected abstract RepositoryRequestHandler getRepositoryRequestHandler();

}
