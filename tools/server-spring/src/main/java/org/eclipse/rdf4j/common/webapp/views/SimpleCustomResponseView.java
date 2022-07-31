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

import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.springframework.web.servlet.View;

/**
 * @author Herko ter Horst
 */
public class SimpleCustomResponseView implements View {

	public static final String SC_KEY = "sc";

	public static final String CONTENT_KEY = "content";

	public static final String CONTENT_LENGTH_KEY = "contentLength";

	public static final String CONTENT_TYPE_KEY = "contentType";

	private static final int DEFAULT_SC = HttpServletResponse.SC_OK;

	private static final SimpleCustomResponseView INSTANCE = new SimpleCustomResponseView();

	public static SimpleCustomResponseView getInstance() {
		return INSTANCE;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		int sc = DEFAULT_SC;
		if (model.containsKey(SC_KEY)) {
			sc = (Integer) model.get(SC_KEY);
		}
		String contentType = (String) model.get(CONTENT_TYPE_KEY);
		Integer contentLength = (Integer) model.get(CONTENT_LENGTH_KEY);

		try (InputStream content = (InputStream) model.get(CONTENT_KEY)) {
			response.setStatus(sc);

			try (ServletOutputStream out = response.getOutputStream()) {
				if (content != null) {
					if (contentType != null) {
						response.setContentType(contentType);
					}
					if (contentLength != null) {
						response.setContentLength(contentLength);
					}
					IOUtil.transfer(content, out);
				} else {
					response.setContentLength(0);
				}
			}
		}
	}
}
