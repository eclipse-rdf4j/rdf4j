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
package org.eclipse.rdf4j.workbench.base;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServlet implements Servlet {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	@Deprecated
	protected static final String SERVER_USER = "server-user";

	@Deprecated
	protected static final String SERVER_PASSWORD = "server-password";

	protected static final String SERVER_USER_PASSWORD = "server-user-password";

	protected static final String ACCEPT = "Accept";

	/**
	 * This response content type is always used for JSONP results.
	 */
	protected static final String APPLICATION_JAVASCRIPT = "application/javascript";

	/**
	 * This response content type is used in cases where application/xml is explicitly requested, or in cases where the
	 * user agent is known to be a commonly available browser.
	 */
	protected static final String APPLICATION_XML = "application/xml";

	/**
	 * This response content type is used for SPARQL Results XML results in non-browser user agents or other cases where
	 * application/xml is not specifically requested.
	 */
	protected static final String APPLICATION_SPARQL_RESULTS_XML = "application/sparql-results+xml";

	protected static final String TEXT_HTML = "text/html";
	protected static final String TEXT_PLAIN = "text/plain";

	protected static final String USER_AGENT = "User-Agent";

	protected static final String MSIE = "MSIE";

	protected static final String MOZILLA = "Mozilla";

	/**
	 * JSONP property for enabling/disabling jsonp functionality.
	 */
	protected static final String JSONP_ENABLED = "org.eclipse.rdf4j.workbench.jsonp.enabled";

	/**
	 * This query parameter is only used in cases where the configuration property is not setup explicitly.
	 */
	protected static final String DEFAULT_JSONP_CALLBACK_PARAMETER = "callback";

	protected static final Pattern JSONP_VALIDATOR = Pattern.compile("^[A-Za-z]\\w+$");

	protected static final String JSONP_CALLBACK_PARAMETER = "org.eclipse.rdf4j.workbench.jsonp.callbackparameter";

	protected ServletConfig config;

	protected AppConfiguration appConfig;

	@Override
	public ServletConfig getServletConfig() {
		return config;
	}

	@Override
	public String getServletInfo() {
		return getClass().getSimpleName();
	}

	@Override
	public void init(final ServletConfig config) throws ServletException {
		this.config = config;
		this.appConfig = new AppConfiguration("Workbench", "RDF4J Workbench");
		try {
			// Suppress loading of log configuration.
			this.appConfig.init(false);
		} catch (IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	public final void service(final ServletRequest req, final ServletResponse resp)
			throws ServletException, IOException {
		final HttpServletRequest hreq = (HttpServletRequest) req;
		final HttpServletResponse hresp = (HttpServletResponse) resp;
		service(hreq, hresp);
	}

	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// default empty implementation
	}

	protected QueryResultFormat getTupleResultFormat(final HttpServletRequest req, final ServletResponse resp) {
		String header = null;
		if (req instanceof WorkbenchRequest) {
			header = ((WorkbenchRequest) req).getParameter(ACCEPT);
		}
		if (null == header) {
			header = req.getHeader(ACCEPT);
		}
		if (header != null) {
			Optional<QueryResultFormat> tupleFormat = QueryResultIO.getParserFormatForMIMEType(header);
			if (tupleFormat.isPresent()) {
				return tupleFormat.get();
			}
		}

		return null;
	}

	protected QueryResultFormat getBooleanResultFormat(final HttpServletRequest req, final ServletResponse resp) {
		String header = req.getHeader(ACCEPT);
		if (header != null) {
			// Then try boolean format
			Optional<QueryResultFormat> booleanFormat = QueryResultIO.getBooleanParserFormatForMIMEType(header);
			if (booleanFormat.isPresent()) {
				return booleanFormat.get();
			}
		}

		return null;
	}

	protected QueryResultFormat getJSONPResultFormat(final HttpServletRequest req, final ServletResponse resp) {
		String header = req.getHeader(ACCEPT);
		if (header != null) {
			if (header.equals(APPLICATION_JAVASCRIPT)) {
				return TupleQueryResultFormat.JSON;
			}
		}

		return null;
	}

	protected QueryResultWriter getResultWriter(final HttpServletRequest req, final ServletResponse resp,
			final OutputStream outputStream) throws UnsupportedQueryResultFormatException, IOException {
		QueryResultFormat resultFormat = getTupleResultFormat(req, resp);

		if (resultFormat == null) {
			resultFormat = getBooleanResultFormat(req, resp);
		}

		if (resultFormat == null) {
			resultFormat = getJSONPResultFormat(req, resp);
		}

		if (resultFormat == null) {
			// This is safe with the current SPARQL Results XML implementation that
			// is able to write out boolean results from the "Tuple" writer.
			resultFormat = TupleQueryResultFormat.SPARQL;
		}

		return QueryResultIO.createWriter(resultFormat, outputStream);
	}

	/**
	 * Gets a {@link TupleResultBuilder} based on the Accept header, and sets the result content type to the best
	 * available match for that, returning a builder that can be used to write out the results.
	 *
	 * @param req          the current HTTP request
	 * @param resp         the current HTTP response
	 * @param outputStream TODO
	 * @return a builder that can be used to write out the results
	 * @throws IOException
	 * @throws UnsupportedQueryResultFormatException
	 */
	protected TupleResultBuilder getTupleResultBuilder(HttpServletRequest req, HttpServletResponse resp,
			OutputStream outputStream) throws UnsupportedQueryResultFormatException, IOException {
		String contentType;
		QueryResultWriter resultWriter = checkJSONP(req, outputStream);

		if (resultWriter != null) {
			// explicitly set the content type to "application/javascript"
			// to fit JSONP best practices
			contentType = APPLICATION_JAVASCRIPT;
		} else {
			// If the JSON-P check above failed, use the normal methods to
			// determine output format
			resultWriter = getResultWriter(req, resp, resp.getOutputStream());
			contentType = resultWriter.getQueryResultFormat().getDefaultMIMEType();
		}

		// HACK: In order to make XSLT stylesheet driven user interface work,
		// browser user agents must receive application/xml if they are going to
		// actually get application/sparql-results+xml
		// NOTE: This will test against both BooleanQueryResultsFormat and
		// TupleQueryResultsFormat
		if (contentType.equals(APPLICATION_SPARQL_RESULTS_XML)) {
			String uaHeader = req.getHeader(USER_AGENT);
			String acceptHeader = req.getHeader(ACCEPT);

			if (acceptHeader != null && acceptHeader.contains(APPLICATION_SPARQL_RESULTS_XML)) {
				// Do nothing, leave the contentType as
				// application/sparql-results+xml
			}
			// Switch back to application/xml for user agents who claim to be
			// Mozilla compatible
			else if (uaHeader != null && uaHeader.contains(MOZILLA)) {
				contentType = APPLICATION_XML;
			}
			// Switch back to application/xml for user agents who accept either
			// application/xml or text/html
			else if (acceptHeader != null
					&& (acceptHeader.contains(APPLICATION_XML) || acceptHeader.contains(TEXT_HTML))) {
				contentType = APPLICATION_XML;
			}
		}

		// Setup qname support for result writers who declare that they support it
		if (resultWriter.getSupportedSettings().contains(BasicQueryWriterSettings.ADD_SESAME_QNAME)) {
			resultWriter.getWriterConfig().set(BasicQueryWriterSettings.ADD_SESAME_QNAME, true);
		}

		resp.setContentType(contentType);

		// TODO: Make the following two settings configurable

		// Convert xsd:string back to plain literals where this behaviour is
		// supported
		if (resultWriter.getSupportedSettings().contains(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL)) {
			resultWriter.getWriterConfig().set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true);
		}

		// Convert rdf:langString back to language literals where this behaviour
		// is supported
		if (resultWriter.getSupportedSettings().contains(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL)) {
			resultWriter.getWriterConfig().set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true);
		}

		return new TupleResultBuilder(resultWriter, SimpleValueFactory.getInstance());
	}

	protected QueryResultWriter checkJSONP(HttpServletRequest req, OutputStream outputStream) throws IOException {
		QueryResultWriter resultWriter = null;
		// HACK : SES-2043 : Need to support Chrome who decide to send Accept: */*
		// instead of application/javascript for JSONP queries, so need to check
		// it first as other algorithm fails in this case.
		// JSONP is enabled in the default properties, but if users setup their
		// own application.properties file then it must be inserted explicitly
		// to be enabled
		if (appConfig != null && appConfig.getProperties().containsKey(JSONP_ENABLED)) {

			String jsonpEnabledProperty = appConfig.getProperties().getProperty(JSONP_ENABLED);

			// check if jsonp is a property and it is set to true
			if (jsonpEnabledProperty != null && Boolean.parseBoolean(jsonpEnabledProperty)) {
				String parameterName = null;

				// check whether they customised the parameter to use to identify
				// the jsonp callback
				if (appConfig.getProperties().containsKey(JSONP_CALLBACK_PARAMETER)) {
					parameterName = appConfig.getProperties().getProperty(JSONP_CALLBACK_PARAMETER);
				}

				// Use default parameter name if it was missing in the
				// configuration after jsonp was enabled
				if (parameterName == null || parameterName.trim().isEmpty()) {
					parameterName = DEFAULT_JSONP_CALLBACK_PARAMETER;
				}

				String parameter = req.getParameter(parameterName);

				if (parameter != null) {
					parameter = parameter.trim();

					if (parameter.isEmpty()) {
						parameter = BasicQueryWriterSettings.JSONP_CALLBACK.getDefaultValue();
					}

					// check callback function name is a valid javascript function
					// name
					if (!JSONP_VALIDATOR.matcher(parameter).matches()) {
						throw new IOException("Callback function name was invalid");
					}

					resultWriter = QueryResultIO.createWriter(TupleQueryResultFormat.JSON, outputStream);

					resultWriter.getWriterConfig().set(BasicQueryWriterSettings.JSONP_CALLBACK, parameter);
				}
			}
		}
		return resultWriter;
	}
}
