/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class SizeController extends AbstractActionController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {
		logger.info("{} txn size request", request.getMethod());
		var result = getSize(transaction, request, response);
		logger.info("{} txn size request finished", request.getMethod());

		return result;
	}

	private ModelAndView getSize(Transaction transaction, HttpServletRequest request, HttpServletResponse response)
			throws HTTPException {
		ProtocolUtil.logRequestParameters(request);

		Map<String, Object> model = new HashMap<>();
		final boolean headersOnly = METHOD_HEAD.equals(request.getMethod());

		if (!headersOnly) {
			Repository repository = RepositoryInterceptor.getRepository(request);

			ValueFactory vf = repository.getValueFactory();
			Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME, vf);

			long size;

			try {
				size = transaction.getSize(contexts);
			} catch (RepositoryException | InterruptedException | ExecutionException e) {
				throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
			}
			model.put(SimpleResponseView.CONTENT_KEY, String.valueOf(size));
		}

		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}

}
