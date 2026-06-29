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
package org.eclipse.rdf4j.common.webapp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

/**
 * @author jeen
 */
public class HttpServerUtilTest {

	private ArrayList<String> tupleQueryMimeTypes;

	/**
	 */
	@BeforeEach
	public void setUp() {
		FileFormatServiceRegistry<? extends FileFormat, ?> registry = TupleQueryResultWriterRegistry.getInstance();

		tupleQueryMimeTypes = new ArrayList<>(16);
		for (FileFormat format : registry.getKeys()) {
			tupleQueryMimeTypes.addAll(format.getMIMETypes());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, jakarta.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType1() {

		ServletRequestStub testRequest = new ServletRequestStub("application/sparql-results+json, */*");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/sparql-results+json", preferredType);

	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, jakarta.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType2() {

		ServletRequestStub testRequest = new ServletRequestStub("application/sparql-results+json, */*;q=0.9");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/sparql-results+json", preferredType);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, jakarta.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType3() {

		ServletRequestStub testRequest = new ServletRequestStub("application/xml");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/xml", preferredType);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, jakarta.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType4() {

		ServletRequestStub testRequest = new ServletRequestStub("*/*", "application/sparql-result+xml;q=0.9",
				"application/sparql-results+json");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/sparql-results+json", preferredType);
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, jakarta.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType5() {

		ServletRequestStub testRequest = new ServletRequestStub("application/*", "application/sparql-results+json");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/sparql-results+json", preferredType);
	}

	class ServletRequestStub implements jakarta.servlet.http.HttpServletRequest {

		private final Enumeration<String> testHeaders;

		public ServletRequestStub(String... testHeaders) {
			this.testHeaders = Collections.enumeration(Arrays.asList(testHeaders));
		}

		@Override
		public Object getAttribute(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getAttributeNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getCharacterEncoding() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setCharacterEncoding(String env) {
			// TODO Auto-generated method stub

		}

		@Override
		public int getContentLength() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getContentType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ServletInputStream getInputStream() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getParameter(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getParameterNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String[] getParameterValues(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map getParameterMap() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getProtocol() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getScheme() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getServerName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getServerPort() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public BufferedReader getReader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRemoteAddr() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRemoteHost() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setAttribute(String name, Object o) {
			// TODO Auto-generated method stub

		}

		@Override
		public void removeAttribute(String name) {
			// TODO Auto-generated method stub

		}

		@Override
		public Locale getLocale() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getLocales() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isSecure() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String path) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getRemotePort() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getLocalName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getLocalAddr() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getLocalPort() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getAuthType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Cookie[] getCookies() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getDateHeader(String name) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getHeader(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration getHeaders(String name) {
			return testHeaders;
		}

		@Override
		public Enumeration getHeaderNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getIntHeader(String name) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getMethod() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPathInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getPathTranslated() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getContextPath() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getQueryString() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRemoteUser() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isUserInRole(String role) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Principal getUserPrincipal() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRequestedSessionId() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRequestURI() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public StringBuffer getRequestURL() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getServletPath() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HttpSession getSession(boolean create) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public HttpSession getSession() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isRequestedSessionIdValid() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromCookie() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isRequestedSessionIdFromURL() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#getServletContext()
		 */
		@Override
		public ServletContext getServletContext() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#startAsync()
		 */
		@Override
		public AsyncContext startAsync() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#startAsync(jakarta.servlet.ServletRequest,
		 * jakarta.servlet.ServletResponse)
		 */
		@Override
		public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
				throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#isAsyncStarted()
		 */
		@Override
		public boolean isAsyncStarted() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#isAsyncSupported()
		 */
		@Override
		public boolean isAsyncSupported() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#getAsyncContext()
		 */
		@Override
		public AsyncContext getAsyncContext() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.ServletRequest#getDispatcherType()
		 */
		@Override
		public DispatcherType getDispatcherType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRequestId() {
			return null;
		}

		@Override
		public String getProtocolRequestId() {
			return null;
		}

		@Override
		public ServletConnection getServletConnection() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.http.HttpServletRequest#authenticate(jakarta.servlet.http.HttpServletResponse)
		 */
		@Override
		public boolean authenticate(HttpServletResponse response) throws ServletException {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.http.HttpServletRequest#login(java.lang.String, java.lang.String)
		 */
		@Override
		public void login(String username, String password) throws ServletException {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.http.HttpServletRequest#logout()
		 */
		@Override
		public void logout() throws ServletException {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.http.HttpServletRequest#getParts()
		 */
		@Override
		public Collection<Part> getParts() throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see jakarta.servlet.http.HttpServletRequest#getPart(java.lang.String)
		 */
		@Override
		public Part getPart(String name) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
			return null;
		}

		@Override
		public String changeSessionId() {
			return null;
		}

		@Override
		public long getContentLengthLong() {
			return 0;
		}
	}
}
