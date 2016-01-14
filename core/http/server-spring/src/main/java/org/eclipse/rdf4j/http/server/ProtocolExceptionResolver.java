/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.webapp.views.SimpleResponseView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Simple resolver for Exceptions: returns the correct response code and message
 * to the client.
 * 
 * @author Herko ter Horst
 */
public class ProtocolExceptionResolver implements HandlerExceptionResolver {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception exception)
	{
		logger.debug("ProtocolExceptionResolver.resolveException() called");

		int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		String errMsg = exception.getMessage();

		if (exception instanceof HTTPException) {
			HTTPException httpExc = (HTTPException)exception;
			statusCode = httpExc.getStatusCode();

			if (exception instanceof ClientHTTPException) {
				logger.info("Client sent bad request ( " + statusCode + ")", exception);
			}
			else {
				logger.error("Error while handling request (" + statusCode + ")", exception);
			}
		}
		else {
			logger.error("Error while handling request", exception);
		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put(SimpleResponseView.SC_KEY, Integer.valueOf(statusCode));
		model.put(SimpleResponseView.CONTENT_KEY, errMsg);

		return new ModelAndView(SimpleResponseView.getInstance(), model);
	}
}
