/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client.spi;

import java.io.IOException;

/**
 * HTTP-client-agnostic facade for executing HTTP requests.
 * <p>
 * Implementations wrap a concrete HTTP client library (e.g., JDK {@code java.net.http.HttpClient} or Apache
 * HttpComponents 5). The caller is responsible for closing the returned {@link HttpClientResponse}.
 *
 * <p>
 * Implementations may support transparent retry behavior. For example, a connection-pool-backed implementation may
 * retry a request once when a stale pooled connection is detected (i.e., the server has closed the connection while it
 * was idle), and may additionally retry on HTTP 408 (Request Timeout) responses up to a limit derived from the pool
 * size. Redirect following (including preserving the original HTTP method across 301/302/307/308 responses) is also
 * handled transparently by capable implementations.
 */
public interface RDF4JHttpClient extends AutoCloseable {

	/**
	 * Executes the given HTTP request and returns the response.
	 * <p>
	 * The caller must close the returned {@link HttpClientResponse} after use to release connection resources.
	 *
	 * @param request the request to execute.
	 * @return the HTTP response.
	 * @throws IOException if a network or I/O error occurs.
	 */
	HttpClientResponse execute(HttpClientRequest request) throws IOException;

	/**
	 * Closes this client and releases any resources held (e.g., connection pools, threads).
	 */
	@Override
	void close();
}
