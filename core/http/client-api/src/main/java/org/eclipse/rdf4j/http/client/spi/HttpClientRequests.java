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

import java.net.URI;

/**
 * Convenience factory methods for common HTTP request types.
 */
public final class HttpClientRequests {

	private HttpClientRequests() {
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a GET request targeting the given URI.
	 *
	 * @param uri the target URI; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for GET
	 */
	public static HttpClientRequest.Builder get(URI uri) {
		return HttpClientRequest.newBuilder("GET", uri);
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a GET request targeting the given String URI.
	 *
	 * @param uriString the target URI as a string; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for GET
	 */
	public static HttpClientRequest.Builder get(String uriString) {
		return get(URI.create(uriString));
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a POST request targeting the given URI.
	 *
	 * @param uri the target URI; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for POST
	 */
	public static HttpClientRequest.Builder post(URI uri) {
		return HttpClientRequest.newBuilder("POST", uri);
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a POST request targeting the given String URI.
	 *
	 * @param uriString the target URI as a string; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for POST
	 */
	public static HttpClientRequest.Builder post(String uriString) {
		return post(URI.create(uriString));
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a PUT request targeting the given URI.
	 *
	 * @param uri the target URI; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for PUT
	 */
	public static HttpClientRequest.Builder put(URI uri) {
		return HttpClientRequest.newBuilder("PUT", uri);
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a PUT request targeting the given String URI.
	 *
	 * @param uriString the target URI as a string; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for PUT
	 */
	public static HttpClientRequest.Builder put(String uriString) {
		return put(URI.create(uriString));
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a DELETE request targeting the given URI.
	 *
	 * @param uri the target URI; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for DELETE
	 */
	public static HttpClientRequest.Builder delete(URI uri) {
		return HttpClientRequest.newBuilder("DELETE", uri);
	}

	/**
	 * Returns a {@link HttpClientRequest.Builder} for a DELETE request targeting the given String URI.
	 *
	 * @param uriString the target URI as a string; must not be {@code null}
	 * @return a new {@link HttpClientRequest.Builder} configured for DELETE
	 */
	public static HttpClientRequest.Builder delete(String uriString) {
		return delete(URI.create(uriString));
	}
}
