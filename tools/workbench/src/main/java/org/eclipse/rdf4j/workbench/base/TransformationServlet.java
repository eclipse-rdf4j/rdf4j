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
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.workbench.exceptions.MissingInitParameterException;
import org.eclipse.rdf4j.workbench.util.CookieHandler;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransformationServlet extends AbstractRepositoryServlet {

	protected static final ParserConfig NON_VERIFYING_PARSER_CONFIG;

	static {
		NON_VERIFYING_PARSER_CONFIG = new ParserConfig();
		NON_VERIFYING_PARSER_CONFIG.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
		NON_VERIFYING_PARSER_CONFIG.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		NON_VERIFYING_PARSER_CONFIG.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
	}

	public static final String CONTEXT = "context";

	protected static final String INFO = "info";

	private static final String TRANSFORMATIONS = "transformations";

	private static final Logger LOGGER = LoggerFactory.getLogger(TransformationServlet.class);

	private final Map<String, String> defaults = new HashMap<>();

	protected CookieHandler cookies;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		cookies = new CookieHandler(config, this);

		if (config != null) {
			if (config.getInitParameter(TRANSFORMATIONS) == null) {
				throw new MissingInitParameterException(TRANSFORMATIONS);
			}
			final Enumeration<?> names = config.getInitParameterNames();
			while (names.hasMoreElements()) {
				final String name = (String) names.nextElement();
				if (name.startsWith("default-")) {
					defaults.put(name.substring("default-".length()), config.getInitParameter(name));
				}
			}
		}
	}

	public String[] getCookieNames() {
		return new String[0];
	}

	@Override
	public void service(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		if (req.getCharacterEncoding() == null) {
			req.setCharacterEncoding(StandardCharsets.UTF_8.name());
		}
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.setDateHeader("Expires", new Date().getTime() - 10000L);
		resp.setHeader("Cache-Control", "no-cache, no-store");

		final String contextPath = req.getContextPath();
		final String path = config.getInitParameter(TRANSFORMATIONS);
		final String xslPath = contextPath + path;
		try {
			final WorkbenchRequest wreq = new WorkbenchRequest(repository, req, defaults);

			cookies.updateCookies(wreq, resp);
			if ("POST".equals(req.getMethod())) {
				doPost(wreq, resp, xslPath);
			} else {
				service(wreq, resp, xslPath);
			}
		} catch (RuntimeException | ServletException | IOException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	protected void doPost(final WorkbenchRequest wreq, final HttpServletResponse resp, final String xslPath)
			throws Exception {
		service(wreq, resp, xslPath);
	}

	protected void service(final WorkbenchRequest req, final HttpServletResponse resp, final String xslPath)
			throws Exception {
		service(getTupleResultBuilder(req, resp, resp.getOutputStream()), xslPath);
	}

	protected void service(final TupleResultBuilder writer, final String xslPath) throws Exception {
		LOGGER.info("Call made to empty superclass implementation of service(PrintWriter,String) for path: {}",
				xslPath);
	}

}
