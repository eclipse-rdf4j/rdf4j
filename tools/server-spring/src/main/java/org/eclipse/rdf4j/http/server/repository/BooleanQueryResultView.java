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

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterFactory;

/**
 * View used to render boolean query results. Renders results in a format specified using a parameter or Accept header.
 *
 * @author Arjohn Kampman
 */
public class BooleanQueryResultView extends QueryResultView {

	private static final BooleanQueryResultView INSTANCE = new BooleanQueryResultView();

	public static BooleanQueryResultView getInstance() {
		return INSTANCE;
	}

	private BooleanQueryResultView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void renderInternal(Map model, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		BooleanQueryResultWriterFactory brWriterFactory = (BooleanQueryResultWriterFactory) model.get(FACTORY_KEY);
		BooleanQueryResultFormat brFormat = brWriterFactory.getBooleanQueryResultFormat();

		response.setStatus(SC_OK);
		setContentType(response, brFormat);
		setContentDisposition(model, response, brFormat);

		boolean headersOnly = (Boolean) model.get(HEADERS_ONLY);

		if (!headersOnly) {
			try (OutputStream out = response.getOutputStream()) {
				BooleanQueryResultWriter qrWriter = brWriterFactory.getWriter(out);
				boolean value = (Boolean) model.get(QUERY_RESULT_KEY);
				qrWriter.handleBoolean(value);
			} catch (QueryResultHandlerException e) {
				if (e.getCause() != null && e.getCause() instanceof IOException) {
					throw (IOException) e.getCause();
				} else {
					throw new IOException(e);
				}
			}
		}
		logEndOfRequest(request);
	}
}
