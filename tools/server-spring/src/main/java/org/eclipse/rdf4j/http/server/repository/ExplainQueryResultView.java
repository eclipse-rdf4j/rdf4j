/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.explanation.Explanation;

public class ExplainQueryResultView extends QueryResultView {

	private static final String MIME_PLAIN = "text/plain";
	private static final String MIME_JSON = "application/json";

	@Override
	protected void renderInternal(
			final Map model, final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		String mimeType = getRequestedMimeType(request);
		Explanation explanation = (Explanation) model.get(QUERY_EXPLAIN_RESULT_KEY);

		if (explanation == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No explanation result found.");
			return;
		}

		response.setCharacterEncoding("UTF-8");
		response.setStatus(HttpStatus.SC_OK);

		try (PrintWriter writer = response.getWriter()) {
			if (MIME_JSON.equals(mimeType)) {
				response.setContentType(MIME_JSON);
				writer.write(explanation.toJson());
			} else if (MIME_PLAIN.equals(mimeType) || mimeType == null || mimeType.isEmpty()) {
				response.setContentType(MIME_PLAIN);
				writer.write(explanation.toString());
			} else {
				response.sendError(
						HttpServletResponse.SC_BAD_REQUEST,
						"Unsupported MIME type: " + mimeType + ". Must be either text/plain or application/json."
				);
			}
		}
	}

	private String getRequestedMimeType(HttpServletRequest request) {
		String mimeType = request.getParameter(Protocol.ACCEPT_PARAM_NAME);
		return (mimeType != null) ? mimeType : request.getHeader("Accept");
	}
}
