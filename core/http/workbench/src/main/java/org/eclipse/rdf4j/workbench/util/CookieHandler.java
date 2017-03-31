/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.util;

import static java.lang.Integer.parseInt;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles cookies for TransformationServlet.
 * 
 * @author Dale Visser
 */
public class CookieHandler {

	private static final String COOKIE_AGE_PARAM = "cookie-max-age";

	private static final Logger LOGGER = LoggerFactory.getLogger(CookieHandler.class);

	private final ServletConfig config;

	private final TransformationServlet servlet;

	public CookieHandler(final ServletConfig config, final TransformationServlet servlet) {
		this.config = config;
		this.servlet = servlet;
	}

	public void updateCookies(final WorkbenchRequest req, final HttpServletResponse resp)
		throws UnsupportedEncodingException
	{
		for (String name : this.servlet.getCookieNames()) {
			if (req.isParameterPresent(name)) {
				addCookie(req, resp, name);
			}
		}
	}

	private void addCookie(final WorkbenchRequest req, final HttpServletResponse resp, final String name)
		throws UnsupportedEncodingException
	{
		final String raw = req.getParameter(name);
		final String value = URLEncoder.encode(raw, "UTF-8");
		LOGGER.info("name: {}\nvalue: {}", name, value);
		LOGGER.info("un-encoded value: {}\n--", raw);
		final Cookie cookie = new Cookie(name, value);
		if (req.getContextPath().isEmpty()) {
			cookie.setPath("/");
		}
		else {
			cookie.setPath(req.getContextPath());
		}
		cookie.setMaxAge(parseInt(config.getInitParameter(COOKIE_AGE_PARAM)));
		addCookie(req, resp, cookie);
	}

	private void addCookie(final WorkbenchRequest req, final HttpServletResponse resp, final Cookie cookie) {
		final Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie c : cookies) {
				if (cookie.getName().equals(c.getName()) && cookie.getValue().equals(c.getValue())) {
					// Cookie already exists. Tell the browser we are using it.
					resp.addHeader("Vary", "Cookie");
				}
			}
		}
		resp.addCookie(cookie);
	}

	/**
	 * Add a 'total_result_count' cookie. Used by both QueryServlet and ExploreServlet.
	 * 
	 * @param req
	 *        the request object
	 * @param resp
	 *        the response object
	 * @value the value to give the cookie
	 */
	public void addTotalResultCountCookie(WorkbenchRequest req, HttpServletResponse resp, int value) {
		addCookie(req, resp, "total_result_count", String.valueOf(value));
	}

	public void addCookie(WorkbenchRequest req, HttpServletResponse resp, String name, String value) {
		final Cookie cookie = new Cookie(name, value);
		if (req.getContextPath().isEmpty()) {
			cookie.setPath("/");
		}
		else {
			cookie.setPath(req.getContextPath());
		}
		cookie.setMaxAge(Integer.parseInt(config.getInitParameter(COOKIE_AGE_PARAM)));
		this.addCookie(req, resp, cookie);
	}
}
