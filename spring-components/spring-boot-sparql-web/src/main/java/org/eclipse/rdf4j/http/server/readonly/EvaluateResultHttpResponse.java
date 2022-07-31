/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.readonly.sparql.EvaluateResult;

/**
 * Encapsulated the {@link HttpServletResponse}.
 */
class EvaluateResultHttpResponse implements EvaluateResult {

	private HttpServletResponse response;

	public EvaluateResultHttpResponse(HttpServletResponse response) {
		this.response = response;
	}

	@Override
	public void setContentType(String contentType) {
		response.setContentType(contentType);
	}

	@Override
	public String getContentType() {
		return response.getContentType();
	}

	@Override
	public OutputStream getOutputstream() throws IOException {
		return response.getOutputStream();
	}
}
