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
package org.eclipse.rdf4j.workbench.proxy;

import javax.servlet.ServletConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles cookies for proxy servlets.
 */
public class CookieHandler {

	protected static final String COOKIE_AGE_PARAM = "cookie-max-age";

	private final String maxAge;

	protected CookieHandler(final String maxAge) {
		this.maxAge = maxAge;
	}

	protected CookieHandler(final ServletConfig config) {
		this(config.getInitParameter(COOKIE_AGE_PARAM));
	}

	protected String getCookieNullIfEmpty(final HttpServletRequest req, final HttpServletResponse resp,
			final String name) {
		String value = this.getCookie(req, resp, name);
		if (null != value && value.isEmpty()) {
			value = null;
		}
		return value;
	}

	protected String getCookie(final HttpServletRequest req, final HttpServletResponse resp, final String name) {
		String value = null;
		final Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					resp.addHeader("Vary", "Cookie");
					initCookie(cookie, req);
					resp.addCookie(cookie);
					value = cookie.getValue();
					break;
				}
			}
		}
		return value;
	}

	private void initCookie(final Cookie cookie, final HttpServletRequest req) {
		final String context = req.getContextPath();
		cookie.setPath(context.isEmpty() ? "/" : context);
		if (maxAge != null) {
			cookie.setMaxAge(Integer.parseInt(maxAge));
		}
	}

	/**
	 * @param req   servlet request
	 * @param resp  servlet response
	 * @param name  cookie name
	 * @param value cookie value
	 */
	protected void addNewCookie(final HttpServletRequest req, final HttpServletResponse resp, final String name,
			final String value) {
		final Cookie cookie = new Cookie(name, value);
		initCookie(cookie, req);
		resp.addCookie(cookie);
	}

	/**
	 * @return the maximum age allowed for cookies
	 */
	public String getMaxAge() {
		return maxAge;
	}
}
