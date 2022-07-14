/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.webapp.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jeen
 */
public class HttpServerUtilTest {

	private ArrayList<String> tupleQueryMimeTypes;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		FileFormatServiceRegistry<? extends FileFormat, ?> registry = TupleQueryResultWriterRegistry.getInstance();

		tupleQueryMimeTypes = new ArrayList<>(16);
		for (FileFormat format : registry.getKeys()) {
			tupleQueryMimeTypes.addAll(format.getMIMETypes());
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, javax.servlet.http.HttpServletRequest)}
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
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, javax.servlet.http.HttpServletRequest)}
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
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, javax.servlet.http.HttpServletRequest)}
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
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, javax.servlet.http.HttpServletRequest)}
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
	 * {@link org.eclipse.rdf4j.common.webapp.util.HttpServerUtil#selectPreferredMIMEType(java.util.Iterator, javax.servlet.http.HttpServletRequest)}
	 * .
	 */
	@Test
	public void testSelectPreferredMIMEType5() {

		ServletRequestStub testRequest = new ServletRequestStub("application/*", "application/sparql-results+json");

		String preferredType = HttpServerUtil.selectPreferredMIMEType(tupleQueryMimeTypes.iterator(), testRequest);

		assertEquals("application/sparql-results+json", preferredType);
	}

	class ServletRequestStub implements javax.servlet.http.HttpServletRequest {

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
		public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
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
		public ServletInputStream getInputStream() throws IOException {
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
		public BufferedReader getReader() throws IOException {
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
		public String getRealPath(String path) {
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

		@Override
		public boolean isRequestedSessionIdFromUrl() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#getServletContext()
		 */
		@Override
		public ServletContext getServletContext() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#startAsync()
		 */
		@Override
		public AsyncContext startAsync() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#startAsync(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
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
		 * @see javax.servlet.ServletRequest#isAsyncStarted()
		 */
		@Override
		public boolean isAsyncStarted() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#isAsyncSupported()
		 */
		@Override
		public boolean isAsyncSupported() {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#getAsyncContext()
		 */
		@Override
		public AsyncContext getAsyncContext() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.ServletRequest#getDispatcherType()
		 */
		@Override
		public DispatcherType getDispatcherType() {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.http.HttpServletRequest#authenticate(javax.servlet.http.HttpServletResponse)
		 */
		@Override
		public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
			// TODO Auto-generated method stub
			return false;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.http.HttpServletRequest#login(java.lang.String, java.lang.String)
		 */
		@Override
		public void login(String username, String password) throws ServletException {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.http.HttpServletRequest#logout()
		 */
		@Override
		public void logout() throws ServletException {
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.http.HttpServletRequest#getParts()
		 */
		@Override
		public Collection<Part> getParts() throws IOException, ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.servlet.http.HttpServletRequest#getPart(java.lang.String)
		 */
		@Override
		public Part getPart(String name) throws IOException, ServletException {
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
