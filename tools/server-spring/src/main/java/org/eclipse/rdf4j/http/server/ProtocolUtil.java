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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.common.webapp.util.HttpServerUtil;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to help with the transition between HTTP requests/responses and values expected by the protocol.
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class ProtocolUtil {

	public static Value parseValueParam(HttpServletRequest request, String paramName, ValueFactory vf)
			throws ClientHTTPException {
		String paramValue = request.getParameter(paramName);
		try {
			return Protocol.decodeValue(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Invalid value for parameter '" + paramName + "': " + paramValue);
		}
	}

	public static Resource parseResourceParam(HttpServletRequest request, String paramName, ValueFactory vf)
			throws ClientHTTPException {
		String paramValue = request.getParameter(paramName);
		try {
			return Protocol.decodeResource(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Invalid value for parameter '" + paramName + "': " + paramValue);
		}
	}

	public static IRI parseURIParam(HttpServletRequest request, String paramName, ValueFactory vf)
			throws ClientHTTPException {
		String paramValue = request.getParameter(paramName);
		try {
			return Protocol.decodeURI(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Invalid value for parameter '" + paramName + "': " + paramValue);
		}
	}

	public static IRI parseGraphParam(HttpServletRequest request, ValueFactory vf) throws ClientHTTPException {
		String paramValue = request.getParameter(Protocol.GRAPH_PARAM_NAME);
		if (paramValue == null) {
			return null;
		}

		try {
			return Protocol.decodeURI("<" + paramValue + ">", vf);
		} catch (IllegalArgumentException e) {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Invalid value for parameter '" + Protocol.GRAPH_PARAM_NAME + "': " + paramValue);
		}
	}

	public static Resource[] parseContextParam(HttpServletRequest request, String paramName, ValueFactory vf)
			throws ClientHTTPException {
		String[] paramValues = request.getParameterValues(paramName);
		try {
			return Protocol.decodeContexts(paramValues, vf);
		} catch (IllegalArgumentException e) {
			throw new ClientHTTPException(SC_BAD_REQUEST,
					"Invalid value for parameter '" + paramName + "': " + e.getMessage());
		}
	}

	public static boolean parseBooleanParam(HttpServletRequest request, String paramName, boolean defaultValue) {
		String paramValue = request.getParameter(paramName);
		if (paramValue == null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(paramValue);
		}
	}

	public static long parseLongParam(HttpServletRequest request, String paramName, long defaultValue)
			throws ClientHTTPException {
		String paramValue = request.getParameter(paramName);
		if (paramValue == null) {
			return defaultValue;
		} else {
			try {
				return Long.parseLong(paramValue);
			} catch (IllegalArgumentException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST,
						"Invalid value for parameter '" + paramName + "': " + e.getMessage());
			}
		}
	}

	/**
	 * Logs all request parameters of the supplied request.
	 */
	public static void logRequestParameters(HttpServletRequest request) {
		Logger logger = LoggerFactory.getLogger(ProtocolUtil.class);
		if (logger.isDebugEnabled()) {
			@SuppressWarnings("unchecked")
			Enumeration<String> paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String name = paramNames.nextElement();
				for (String value : request.getParameterValues(name)) {
					logger.debug("{}=\"{}\"", name, value);
				}
			}
		}
	}

	public static <FF extends FileFormat, S> S getAcceptableService(HttpServletRequest request,
			HttpServletResponse response, FileFormatServiceRegistry<FF, S> serviceRegistry) throws ClientHTTPException {
		// Accept-parameter takes precedence over request headers
		String mimeType = request.getParameter(Protocol.ACCEPT_PARAM_NAME);
		boolean hasAcceptParam = mimeType != null;

		if (mimeType == null) {
			// Find an acceptable MIME type based on the request headers
			logAcceptableFormats(request);

			Collection<String> mimeTypes = new LinkedHashSet<>(16);
			// Prefer the default mime types, explicitly before non-default
			for (FileFormat format : serviceRegistry.getKeys()) {
				mimeTypes.add(format.getDefaultMIMEType());
			}
			for (FileFormat format : serviceRegistry.getKeys()) {
				mimeTypes.addAll(format.getMIMETypes());
			}

			mimeType = HttpServerUtil.selectPreferredMIMEType(mimeTypes.iterator(), request);

			response.setHeader("Vary", "Accept");
		}

		if (mimeType != null) {
			Optional<FF> format = serviceRegistry.getFileFormatForMIMEType(mimeType);

			if (format.isPresent()) {
				return serviceRegistry.get(format.get()).get();
			}
		}

		if (hasAcceptParam) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_FILE_FORMAT, mimeType);
			throw new ClientHTTPException(SC_BAD_REQUEST, errInfo.toString());
		} else {
			// No acceptable format was found, send 406 as required by RFC 2616
			throw new ClientHTTPException(SC_NOT_ACCEPTABLE, "No acceptable file format found.");
		}
	}

	/**
	 * Reads the {@link Protocol#TIMEOUT_PARAM_NAME} parameter from the request and (if present) parses it into an
	 * integer value.
	 *
	 * @param request the {@link HttpServletRequest} to read the parameter from
	 * @return the value of the timeout parameter as an integer (representing the timeout time in seconds), or 0 if no
	 *         timeout parameter is specified in the request.
	 * @throws ClientHTTPException if the value of the timeout parameter is not a valid integer.
	 */
	public static int parseTimeoutParam(HttpServletRequest request) throws ClientHTTPException {
		final String timeoutParam = request.getParameter(Protocol.TIMEOUT_PARAM_NAME);
		int maxExecutionTime = 0;
		if (timeoutParam != null) {
			try {
				maxExecutionTime = Integer.parseInt(timeoutParam);
			} catch (NumberFormatException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Invalid timeout value: " + timeoutParam);
			}
		}
		return maxExecutionTime;
	}

	public static void logAcceptableFormats(HttpServletRequest request) {
		Logger logger = LoggerFactory.getLogger(ProtocolUtil.class);
		if (logger.isDebugEnabled()) {
			StringBuilder acceptable = new StringBuilder(64);

			@SuppressWarnings("unchecked")
			Enumeration<String> acceptHeaders = request.getHeaders("Accept");

			while (acceptHeaders.hasMoreElements()) {
				acceptable.append(acceptHeaders.nextElement());

				if (acceptHeaders.hasMoreElements()) {
					acceptable.append(',');
				}
			}

			logger.debug("Acceptable formats: " + acceptable);
		}
	}
}
