/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.webapp.filters;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author Herko ter Horst
 */
public class PathFilter implements Filter {

	@Override
	public void init(FilterConfig filterConf) throws ServletException {
		// do nothing
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
			throws IOException, ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpServletResponse response = (HttpServletResponse) res;
			String path = request.getContextPath();

			PrintWriter out = response.getWriter();
			CharResponseWrapper wrapper = new CharResponseWrapper((HttpServletResponse) response);
			filterChain.doFilter(request, wrapper);
			CharArrayWriter caw = new CharArrayWriter();
			caw.write(wrapper.toString().replace("${path}", path));
			String result = caw.toString();
			response.setContentLength(result.length());
			out.write(result);
		}
	}

	private static class CharResponseWrapper extends HttpServletResponseWrapper {

		private final CharArrayWriter output;

		@Override
		public String toString() {
			return output.toString();
		}

		public CharResponseWrapper(HttpServletResponse response) {
			super(response);
			output = new CharArrayWriter();
		}

		@Override
		public PrintWriter getWriter() {
			return new PrintWriter(output);
		}
	}
}
