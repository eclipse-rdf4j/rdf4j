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
package org.eclipse.rdf4j.common.webapp.views;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * @author Herko ter Horst
 */
public class SimpleResponseView implements View {

	public static final String SC_KEY = "sc";

	public static final String CONTENT_KEY = "content";

	private static final int DEFAULT_SC = HttpServletResponse.SC_OK;

	private static final String CONTENT_TYPE = "text/plain; charset=UTF-8";

	public static final String CUSTOM_HEADERS_KEY = "headers";

	private static final SimpleResponseView INSTANCE = new SimpleResponseView();

	public static SimpleResponseView getInstance() {
		return INSTANCE;
	}

	private SimpleResponseView() {
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Integer sc = (Integer) model.get(SC_KEY);
		if (sc == null) {
			sc = DEFAULT_SC;
		}
		response.setStatus(sc.intValue());

		response.setContentType(getContentType());

		if (model.containsKey(CUSTOM_HEADERS_KEY)) {
			Map<String, String> customHeaders = (Map<String, String>) model.get(CUSTOM_HEADERS_KEY);
			if (customHeaders != null) {
				for (String headerName : customHeaders.keySet()) {
					response.setHeader(headerName, customHeaders.get(headerName));
				}
			}
		}

		try (OutputStream out = response.getOutputStream()) {
			String content = (String) model.get(CONTENT_KEY);
			if (content != null) {
				byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
				response.setContentLength(contentBytes.length);
				out.write(contentBytes);
			} else {
				response.setContentLength(0);
			}
		}
	}
}
