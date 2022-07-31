/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository.config;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.web.servlet.View;

/**
 * View used to export a repository config. Renders the statements as RDF using a serialization specified using a
 * parameter or Accept header.
 *
 * @author Jeen Broekstra
 */
public class ConfigView implements View {

	public static final String CONFIG_DATA_KEY = "configData";

	public static final String FORMAT_KEY = "format";

	public static final String HEADERS_ONLY = "headersOnly";

	private static final ConfigView INSTANCE = new ConfigView();

	public static ConfigView getInstance() {
		return INSTANCE;
	}

	private ConfigView() {
	}

	@Override
	public String getContentType() {
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		boolean headersOnly = (Boolean) model.get(HEADERS_ONLY);

		RDFFormat rdfFormat = (RDFFormat) model.get(FORMAT_KEY);

		try {
			try (OutputStream out = response.getOutputStream()) {

				response.setStatus(SC_OK);

				String mimeType = rdfFormat.getDefaultMIMEType();
				if (rdfFormat.hasCharset()) {
					Charset charset = rdfFormat.getCharset();
					mimeType += "; charset=" + charset.name();
				}
				response.setContentType(mimeType);

				String filename = "config";
				if (rdfFormat.getDefaultFileExtension() != null) {
					filename += "." + rdfFormat.getDefaultFileExtension();
				}
				response.setHeader("Content-Disposition", "attachment; filename=" + filename);

				if (!headersOnly) {
					Model configuration = (Model) model.get(CONFIG_DATA_KEY);
					Rio.write(configuration, out, rdfFormat);
				}
			}
		} catch (RDFHandlerException e) {
			throw new ServerHTTPException("Serialization error: " + e.getMessage(), e);
		} catch (RepositoryException e) {
			throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
		}
	}

}
