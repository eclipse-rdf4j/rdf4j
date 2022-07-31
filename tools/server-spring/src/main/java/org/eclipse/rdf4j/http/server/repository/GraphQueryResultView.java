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
package org.eclipse.rdf4j.http.server.repository;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View used to render graph query results. Renders the graph as RDF using a serialization specified using a parameter
 * or Accept header.
 *
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class GraphQueryResultView extends QueryResultView {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private static final GraphQueryResultView INSTANCE = new GraphQueryResultView();

	public static GraphQueryResultView getInstance() {
		return INSTANCE;
	}

	private GraphQueryResultView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void renderInternal(Map model, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		RDFWriterFactory rdfWriterFactory = (RDFWriterFactory) model.get(FACTORY_KEY);
		RDFFormat rdfFormat = rdfWriterFactory.getRDFFormat();

		response.setStatus(SC_OK);
		setContentType(response, rdfFormat);
		setContentDisposition(model, response, rdfFormat);

		boolean headersOnly = (Boolean) model.get(HEADERS_ONLY);

		if (!headersOnly) {
			try (OutputStream out = response.getOutputStream()) {
				RDFWriter rdfWriter = rdfWriterFactory.getWriter(out);
				GraphQueryResult graphQueryResult = (GraphQueryResult) model.get(QUERY_RESULT_KEY);
				QueryResults.report(graphQueryResult, rdfWriter);
			} catch (QueryInterruptedException e) {
				logger.error("Query interrupted", e);
				response.sendError(SC_SERVICE_UNAVAILABLE, "Query evaluation took too long");
			} catch (QueryEvaluationException e) {
				logger.error("Query evaluation error", e);
				response.sendError(SC_INTERNAL_SERVER_ERROR, "Query evaluation error: " + e.getMessage());
			} catch (RDFHandlerException e) {
				logger.error("Serialization error", e);
				response.sendError(SC_INTERNAL_SERVER_ERROR, "Serialization error: " + e.getMessage());
			}
		}
		logEndOfRequest(request);
	}
}
