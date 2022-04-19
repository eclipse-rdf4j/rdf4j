/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.proxy;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.workbench.RepositoryServlet;
import org.eclipse.rdf4j.workbench.base.AbstractRepositoryServlet;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.exceptions.MissingInitParameterException;
import org.eclipse.rdf4j.workbench.util.BasicServletConfig;
import org.eclipse.rdf4j.workbench.util.DynamicHttpRequest;

public class ProxyRepositoryServlet extends AbstractRepositoryServlet {

	private static final String HEADER_IFMODSINCE = "If-Modified-Since";

	private static final String HEADER_LASTMOD = "Last-Modified";

	private static final String DEFAULT_PATH_PARAM = "default-command";

	private final Map<String, RepositoryServlet> servlets = new HashMap<>();

	private long lastModified;

	@Override
	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		lastModified = System.currentTimeMillis();
		if (config.getInitParameter(DEFAULT_PATH_PARAM) == null) {
			throw new MissingInitParameterException(DEFAULT_PATH_PARAM);
		}
		Enumeration<String> names = config.getInitParameterNames();
		while (names.hasMoreElements()) {
			String path = names.nextElement();
			if (path.startsWith("/")) {
				try {
					servlets.put(path, createServlet(path));
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					throw new ServletException(e);
				}
			}
		}
	}

	@Override
	public void destroy() {
		for (RepositoryServlet servlet : servlets.values()) {
			servlet.destroy();
		}
	}

	public void resetCache() {
		lastModified = System.currentTimeMillis();
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (isCachable(req)) {
			long ifModifiedSince = req.getDateHeader(HEADER_IFMODSINCE);
			if (ifModifiedSince < lastModified) {
				resp.setDateHeader(HEADER_LASTMOD, lastModified);
			} else {
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
		}
		String pathInfo = req.getPathInfo();
		if (pathInfo == null) {
			String defaultPath = config.getInitParameter(DEFAULT_PATH_PARAM);
			resp.sendRedirect(req.getRequestURI() + defaultPath);
		} else if ("/".equals(pathInfo)) {
			String defaultPath = config.getInitParameter(DEFAULT_PATH_PARAM);
			resp.sendRedirect(req.getRequestURI() + defaultPath.substring(1));
		} else {
			RepositoryServlet servlet = servlets.get(pathInfo);
			if (servlet == null) {
				throw new BadRequestException("Unconfigured path: " + pathInfo);
			}
			DynamicHttpRequest hreq = new DynamicHttpRequest(req);
			hreq.setServletPath(hreq.getServletPath() + hreq.getPathInfo());
			hreq.setPathInfo(null);
			servlet.service(hreq, resp);
		}
		if ("POST".equals(req.getMethod())) {
			lastModified = System.currentTimeMillis();
		} else if (lastModified % 1000 != 0) {
			long modified = System.currentTimeMillis() / 1000 * 1000;
			if (lastModified < modified) {
				lastModified = modified;
			}
		}
	}

	private boolean isCachable(HttpServletRequest req) {
		if (!"GET".equals(req.getMethod())) {
			return false;
		}
		// MSIE does not cache different url parameters separately
		return req.getRequestURL().toString().indexOf(';') < 0;
	}

	private RepositoryServlet createServlet(String path)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, ServletException {
		Class<?> klass = Class.forName(config.getInitParameter(path));
		RepositoryServlet servlet = (RepositoryServlet) klass.newInstance();
		servlet.setRepositoryManager(manager);
		servlet.setRepositoryInfo(info);
		servlet.setRepository(repository);
		servlet.init(new BasicServletConfig(path, config));
		return servlet;
	}

}
