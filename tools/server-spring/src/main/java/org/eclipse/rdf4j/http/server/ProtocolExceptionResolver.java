/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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
			// This is currently just a simple fix that causes the validation report to be printed.
			// This should not be the final solution.
			Model validationReportModel = ((ValidationException) temp).validationReportAsModel();

			StringWriter stringWriter = new StringWriter();

			// We choose NQUADS because we want to support streaming in the future, and because there could be a use for
			// different graphs in the future
			Rio.write(validationReportModel, stringWriter, RDFFormat.NQUADS);

			statusCode = HttpServletResponse.SC_CONFLICT;
			errMsg = stringWriter.toString();

			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/shacl-validation-report+n-quads");
			model.put(SimpleResponseView.CUSTOM_HEADERS_KEY, headers);
		}

		model.put(SimpleResponseView.SC_KEY, statusCode);
		model.put(SimpleResponseView.CONTENT_KEY, errMsg);

		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}
}
