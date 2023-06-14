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
package org.eclipse.rdf4j.http.server;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.http.server.repository.statements.ValidationExceptionView;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Simple resolver for Exceptions: returns the correct response code and message to the client.
 *
 * @author Herko ter Horst
 */
public class ProtocolExceptionResolver implements HandlerExceptionResolver {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception exception) {
		logger.debug("ProtocolExceptionResolver.resolveException() called");

		Map<String, Object> model = new HashMap<>();

		int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		String errMsg = exception.getMessage();

		if (exception instanceof HTTPException) {
			HTTPException httpExc = (HTTPException) exception;
			statusCode = httpExc.getStatusCode();

			if (exception instanceof ClientHTTPException) {
				logger.info("Client sent bad request ( " + statusCode + ")", exception);
			} else {
				logger.error("Error while handling request (" + statusCode + ")", exception);
			}
		} else {
			logger.error("Error while handling request", exception);
		}

		int depth = 10;
		Throwable temp = exception;
		while (!(temp instanceof ValidationException)) {
			if (depth-- == 0) {
				break;
			}
			if (temp == null) {
				break;
			}
			temp = temp.getCause();
		}

		if (temp instanceof ValidationException) {

			model.put(SimpleResponseView.SC_KEY, HttpServletResponse.SC_CONFLICT);

			ProtocolUtil.logRequestParameters(request);

			RDFWriterFactory rdfWriterFactory = RDFWriterRegistry.getInstance().get(RDFFormat.BINARY).orElseThrow();

			model.put(ValidationExceptionView.FACTORY_KEY, rdfWriterFactory);
			model.put(ValidationExceptionView.VALIDATION_EXCEPTION, temp);
			return new ModelAndView(ValidationExceptionView.getInstance(), model);

		} else {
			model.put(SimpleResponseView.SC_KEY, statusCode);
			model.put(SimpleResponseView.CONTENT_KEY, errMsg);
		}

		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}
}
