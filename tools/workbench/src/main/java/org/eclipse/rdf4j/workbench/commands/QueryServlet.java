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
package org.eclipse.rdf4j.workbench.commands;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.UnsupportedQueryResultFormatException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.http.HTTPQueryEvaluationException;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.QueryEvaluator;
import org.eclipse.rdf4j.workbench.util.QueryStorage;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QueryServlet extends TransformationServlet {

	protected static final String REF = "ref";

	protected static final String LIMIT = "limit_query";

	private static final String QUERY_LN = "queryLn";

	private static final String INFER = "infer";

	private static final String ACCEPT = "Accept";

	protected static final String QUERY = "query";

	private static final String[] EDIT_PARAMS = new String[] { QUERY_LN, QUERY, INFER, LIMIT };

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryServlet.class);

	private static final QueryEvaluator EVAL = QueryEvaluator.INSTANCE;

	private static final ObjectMapper mapper = new ObjectMapper();

	private QueryStorage storage;

	protected boolean writeQueryCookie;

	// Poor Man's Cache: At the very least, garbage collection can clean up keys
	// followed by values whenever the JVM faces memory pressure.
	private static Map<String, String> queryCache = Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * For testing purposes only.
	 *
	 * @param testQueryCache cache to use instead of the production cache instance
	 */
	protected static void substituteQueryCache(Map<String, String> testQueryCache) {
		queryCache = testQueryCache;
	}

	protected void substituteQueryStorage(QueryStorage storage) {
		this.storage = storage;
	}

	/**
	 * @return the names of the cookies that will be retrieved from the request, and returned in the response
	 */
	@Override
	public String[] getCookieNames() {
		String[] result;
		if (writeQueryCookie) {
			result = new String[] { QUERY, REF, LIMIT, QUERY_LN, INFER, "total_result_count", "show-datatypes" };
		} else {
			result = new String[] { REF, LIMIT, QUERY_LN, INFER, "total_result_count", "show-datatypes" };
		}
		return result;
	}

	/**
	 * Initialize this instance of the servlet.
	 *
	 * @param config configuration passed in by the application container
	 */
	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		try {
			this.storage = QueryStorage.getSingletonInstance(this.appConfig);
		} catch (RepositoryException | IOException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public void destroy() {
		this.storage.shutdown();
		super.destroy();
	}

	/**
	 * Long query strings could blow past the Tomcat default 8k HTTP header limit if stuffed into a cookie. In this
	 * case, we need to set a flag to avoid this happening before
	 * {@link TransformationServlet#service(HttpServletRequest, HttpServletResponse)} is called. A much lower limit on
	 * the size of the query text is used to stay well below the Tomcat limitation.
	 */
	@Override
	public final void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		this.writeQueryCookie = shouldWriteQueryCookie(req.getParameter(QUERY));
		super.service(req, resp);
	}

	/**
	 * <p>
	 * Determines if the servlet should write out the query text into a cookie as received, or write it's hash instead.
	 * </p>
	 * <p>
	 * Note: This is a separate method for testing purposes.
	 * </p>
	 *
	 * @param queryText the text received as the value for the parameter 'query'
	 */
	protected boolean shouldWriteQueryCookie(String queryText) {
		return (null == queryText || queryText.length() <= 2048);
	}

	@Override
	protected void service(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws IOException, RDF4JException, BadRequestException {
		if (!writeQueryCookie) {
			// If we suppressed putting the query text into the cookies before.
			cookies.addCookie(req, resp, REF, "hash");
			String queryValue = req.getParameter(QUERY);
			String hash = String.valueOf(queryValue.hashCode());
			queryCache.put(hash, queryValue);
			cookies.addCookie(req, resp, QUERY, hash);
		}
		if ("get".equals(req.getParameter("action"))) {
			ObjectNode jsonObject = mapper.createObjectNode();
			jsonObject.put("queryText", getQueryText(req));
			PrintWriter writer = new PrintWriter(new BufferedWriter(resp.getWriter()));
			try {
				writer.write(mapper.writeValueAsString(jsonObject));
			} finally {
				writer.flush();
			}
		} else {
			handleStandardBrowserRequest(req, resp, xslPath);
		}
	}

	private void handleStandardBrowserRequest(WorkbenchRequest req, HttpServletResponse resp, String xslPath)
			throws IOException, RDF4JException, QueryResultHandlerException {
		setContentType(req, resp);
		OutputStream out = resp.getOutputStream();
		try {
			service(req, resp, out, xslPath);
		} catch (BadRequestException | HTTPQueryEvaluationException exc) {
			LOGGER.warn(exc.toString(), exc);
			TupleResultBuilder builder = getTupleResultBuilder(req, resp, out);
			builder.transform(xslPath, "query.xsl");
			builder.start("error-message");
			builder.link(Arrays.asList(INFO, "namespaces"));
			builder.result(exc.getMessage());
			builder.end();
		} finally {
			out.flush();
		}
	}

	@Override
	protected void doPost(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws IOException, BadRequestException, RDF4JException {
		final String action = req.getParameter("action");
		if ("save".equals(action)) {
			saveQuery(req, resp);
		} else if ("edit".equals(action)) {
			if (canReadSavedQuery(req)) {
				/*
				 * only need read access for edit action, since we are only reading the saved query text to present it
				 * in the editor
				 */
				final TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
				builder.transform(xslPath, "query.xsl");
				builder.start(EDIT_PARAMS);
				builder.link(Arrays.asList(INFO, "namespaces"));
				final String queryLn = req.getParameter(EDIT_PARAMS[0]);
				final String query = getQueryText(req);
				final Boolean infer = Boolean.valueOf(req.getParameter(EDIT_PARAMS[2]));
				final Literal limit = SimpleValueFactory.getInstance()
						.createLiteral(req.getParameter(EDIT_PARAMS[3]), XSD.INTEGER);
				builder.result(queryLn, query, infer, limit);
				builder.end();
			} else {
				throw new BadRequestException("Current user may not read the given query.");
			}
		} else if ("exec".equals(action)) {
			if (canReadSavedQuery(req)) {
				service(req, resp, xslPath);
			} else {
				throw new BadRequestException("Current user may not read the given query.");
			}
		} else {
			throw new BadRequestException("POST with unexpected action parameter value: " + action);
		}
	}

	private void saveQuery(final WorkbenchRequest req, final HttpServletResponse resp)
			throws IOException, BadRequestException, RDF4JException {
		resp.setContentType("application/json");
		ObjectNode jsonObject = mapper.createObjectNode();
		jsonObject.put("queryText", getQueryText(req));

		final HTTPRepository http = (HTTPRepository) repository;
		final boolean accessible = storage.checkAccess(http);
		jsonObject.put("accessible", accessible);
		if (accessible) {
			final String queryName = req.getParameter("query-name");
			String userName = getUserNameFromParameter(req, SERVER_USER);
			final boolean existed = storage.askExists(http, queryName, userName);
			jsonObject.put("existed", existed);
			final boolean written = Boolean.valueOf(req.getParameter("overwrite")) || !existed;
			if (written) {
				final boolean shared = !Boolean.valueOf(req.getParameter("save-private"));
				final QueryLanguage queryLanguage = QueryLanguage.valueOf(req.getParameter(QUERY_LN));
				final String queryText = req.getParameter(QUERY);
				final boolean infer = req.isParameterPresent(INFER) ? Boolean.valueOf(req.getParameter(INFER)) : false;
				final int rowsPerPage = Integer.valueOf(req.getParameter(LIMIT));
				if (existed) {
					final IRI query = storage.selectSavedQuery(http, userName, queryName);
					storage.updateQuery(query, userName, shared, queryLanguage, queryText, infer, rowsPerPage);
				} else {
					storage.saveQuery(http, queryName, userName, shared, queryLanguage, queryText, infer, rowsPerPage);
				}
			}
			jsonObject.put("written", written);
		}
		final PrintWriter writer = new PrintWriter(new BufferedWriter(resp.getWriter()));
		writer.write(mapper.writeValueAsString(jsonObject));
		writer.flush();
	}

	private String getUserNameFromParameter(WorkbenchRequest req, String parameter) {
		String userName = req.getParameter(parameter);
		if (null == userName) {
			userName = "";
		}
		return userName;
	}

	private void setContentType(final WorkbenchRequest req, final HttpServletResponse resp) {
		String result = "application/xml";
		String ext = "xml";
		if (req.isParameterPresent(ACCEPT)) {
			final String accept = req.getParameter(ACCEPT);
			final Optional<RDFFormat> format = Rio.getWriterFormatForMIMEType(accept);
			if (format.isPresent()) {
				result = format.get().getDefaultMIMEType();
				ext = format.get().getDefaultFileExtension();
			} else {
				final Optional<QueryResultFormat> tupleFormat = QueryResultIO.getWriterFormatForMIMEType(accept);

				if (tupleFormat.isPresent()) {
					result = tupleFormat.get().getDefaultMIMEType();
					ext = tupleFormat.get().getDefaultFileExtension();
				} else {
					final Optional<QueryResultFormat> booleanFormat = QueryResultIO
							.getBooleanWriterFormatForMIMEType(accept);

					if (booleanFormat.isPresent()) {
						result = booleanFormat.get().getDefaultMIMEType();
						ext = booleanFormat.get().getDefaultFileExtension();
					}
				}
			}
		}

		resp.setContentType(result);
		if (!result.equals("application/xml")) {
			final String attachment = "attachment; filename=query." + ext;
			resp.setHeader("Content-disposition", attachment);
		}
	}

	private void service(final WorkbenchRequest req, final HttpServletResponse resp, final OutputStream out,
			final String xslPath)
			throws BadRequestException, RDF4JException, UnsupportedQueryResultFormatException, IOException {
		try (RepositoryConnection con = repository.getConnection()) {
			con.setParserConfig(NON_VERIFYING_PARSER_CONFIG);
			final TupleResultBuilder builder = getTupleResultBuilder(req, resp, resp.getOutputStream());
			for (Namespace ns : Iterations.asList(con.getNamespaces())) {
				builder.prefix(ns.getPrefix(), ns.getName());
			}
			String query = getQueryText(req);
			if (query.isEmpty()) {
				builder.transform(xslPath, "query.xsl");
				builder.start();
				builder.link(Arrays.asList(INFO, "namespaces"));
				builder.end();
			} else {
				try {
					EVAL.extractQueryAndEvaluate(builder, resp, out, xslPath, con, query, req, this.cookies);
				} catch (MalformedQueryException exc) {
					throw new BadRequestException(exc.getMessage(), exc);
				} catch (HTTPQueryEvaluationException exc) {
					if (exc.getCause() instanceof MalformedQueryException) {
						throw new BadRequestException(exc.getCause().getMessage(), exc);
					}
					throw exc;
				}
			}
		}
	}

	/**
	 * @param req for looking at the request parameters
	 * @return the query text, if it can somehow be retrieved from request parameters, otherwise an empty string
	 * @throws BadRequestException if a problem occurs grabbing the request from storage
	 * @throws RDF4JException      if a problem occurs grabbing the request from storage
	 */
	protected String getQueryText(WorkbenchRequest req) throws BadRequestException, RDF4JException {
		String result;
		if (req.isParameterPresent(QUERY)) {
			String query = req.getParameter(QUERY);
			if (req.isParameterPresent(REF)) {
				String ref = req.getParameter(REF);
				if ("text".equals(ref)) {
					result = query;
				} else if ("hash".equals(ref)) {
					result = queryCache.get(query);
					if (null == result) {
						result = "";
					}
				} else if ("id".equals(ref)) {
					result = storage.getQueryText((HTTPRepository) repository, getUserNameFromParameter(req, "owner"),
							query);
				} else {
					// if ref not recognized assume request meant "text"
					result = query;
				}
			} else {
				result = query;
			}
		} else {
			result = "";
		}
		return result;
	}

	private boolean canReadSavedQuery(WorkbenchRequest req) throws BadRequestException, RDF4JException {
		if (req.isParameterPresent(REF)) {
			return "id".equals(req.getParameter(REF))
					? storage.canRead(
							storage.selectSavedQuery((HTTPRepository) repository,
									getUserNameFromParameter(req, "owner"), req.getParameter(QUERY)),
							getUserNameFromParameter(req, SERVER_USER))
					: true;
		} else {
			throw new BadRequestException("Expected 'ref' parameter in request.");
		}
	}

}
