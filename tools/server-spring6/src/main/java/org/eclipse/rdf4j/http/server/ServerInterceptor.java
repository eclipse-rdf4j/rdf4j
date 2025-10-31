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
package org.eclipse.rdf4j.http.server;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Base class for single-use request interceptors. This implementation sets the thread name to something sensible at the
 * start of the request handling and resets the name at the end. This is useful for logging frameworks that make use of
 * thread names, such as Log4J. Should not be a singleton bean! Configure as inner bean in openrdf-servlet.xml
 *
 * @author Herko ter Horst
 */
public abstract class ServerInterceptor implements HandlerInterceptor {

	private static final String REQUEST_ID_KEY = "org.eclipse.rdf4j.requestId";
	private static final String PROCESS_ID = "process:" + UUID.randomUUID();

	private static final AtomicLong requestNumber = new AtomicLong(0L);

	private volatile String origThreadName;

	private static String createRequestId() {
		return PROCESS_ID + ":request:" + requestNumber.getAndIncrement();
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		origThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(getThreadName());
		MDC.put(REQUEST_ID_KEY, createRequestId());

		setRequestAttributes(request);

		return HandlerInterceptor.super.preHandle(request, response, handler);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception exception) throws Exception {
		try {
			cleanUpResources();
		} finally {
			MDC.remove(REQUEST_ID_KEY);
			Thread.currentThread().setName(origThreadName);
		}
	}

	/**
	 * Determine the thread name to use. Called before the request is forwarded to a handler.
	 *
	 * @return a name that makes sense based on the request
	 * @throws ServerHTTPException if it was impossible to determine a name due to an internal error
	 */
	protected abstract String getThreadName() throws ServerHTTPException;

	/**
	 * Set attributes for this request. Called before the request is forwarded to a handler. By default, this method
	 * does nothing.
	 *
	 * @param request the request
	 * @throws ClientHTTPException if it was impossible to set one or more attributes due to a bad request on the part
	 *                             of the client
	 * @throws ServerHTTPException if it was impossible to set one or more attributes due to an internal error
	 */
	protected void setRequestAttributes(HttpServletRequest request) throws ClientHTTPException, ServerHTTPException {
	}

	/**
	 * Clean up resources used in handling this request. Called after the request is handled and a the view is rendered
	 * (or an exception has occurred). By default, this method does nothing.
	 *
	 * @throws ServerHTTPException if some resources could not be cleaned up because of an internal error
	 */
	protected void cleanUpResources() throws ServerHTTPException {
	}
}
