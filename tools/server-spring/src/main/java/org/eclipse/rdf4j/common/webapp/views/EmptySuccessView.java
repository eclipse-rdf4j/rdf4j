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

import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.View;

/**
 * @author Herko ter Horst
 */
public class EmptySuccessView implements View {

	private static final EmptySuccessView INSTANCE = new EmptySuccessView();

	public static EmptySuccessView getInstance() {
		return INSTANCE;
	}

	private EmptySuccessView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			// Indicate success with a 204 NO CONTENT response
			response.setStatus(SC_NO_CONTENT);
		} finally {
			response.getOutputStream().close();
		}
	}

}
