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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An HTTP request with method, URI, headers, and optional body.
 *
 * <p>
 * Instances are created via the {@link Builder}, which is obtained from {@link #newBuilder(String, URI)}. For common
 * request patterns, consider using the convenience factory methods in {@link HttpRequests}.
 *
 * <p>
 * Headers can be added after construction via {@link #addHeader(String, String)}, which allows
 * {@link AuthenticationHandler} implementations to modify requests in-place.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * HttpRequest request = HttpRequest.newBuilder("POST", URI.create("https://example.com/sparql"))
 * 		.header("Accept", "application/sparql-results+json")
 * 		.body(HttpRequestBody.ofFormData(params))
 * 		.build();
 * </pre>
 *
 * @see HttpRequests
 * @see RDF4JHttpClient
 * @see HttpRequestBody
 */
public class HttpRequest {

	private final String method;
	private final URI uri;
	private final List<HttpHeader> headers;
	private final HttpRequestBody body;

	private HttpRequest(Builder builder) {
		this.method = builder.method;
		this.uri = builder.uri;
		this.headers = new ArrayList<>(builder.headers);
		this.body = builder.body;
	}

	/**
	 * Returns the HTTP method of this request (e.g. {@code GET}, {@code POST}).
	 *
	 * @return the HTTP method; never {@code null}
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the target URI of this request.
	 *
	 * @return the request URI; never {@code null}
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * Returns the list of headers associated with this request. The list may be empty if no headers were set.
	 *
	 * @return the list of {@link HttpHeader}s; never {@code null}
	 */
	public List<HttpHeader> getHeaders() {
		return headers;
	}

	/**
	 * Appends a header to this request. Multiple calls with the same name are additive; no deduplication is performed.
	 *
	 * @param name  the header name; must not be {@code null}
	 * @param value the header value; must not be {@code null}
	 */
	public void addHeader(String name, String value) {
		headers.add(HttpHeader.of(name, value));
	}

	/**
	 * Returns the optional request body. For methods that do not carry a body (e.g. {@code GET}, {@code DELETE}) this
	 * will be empty.
	 *
	 * @return an {@link Optional} containing the {@link HttpRequestBody}, or empty if no body was set
	 */
	public Optional<HttpRequestBody> getBody() {
		return Optional.ofNullable(body);
	}

	/**
	 * Creates a new {@link Builder} for the given HTTP method and target URI.
	 *
	 * @param method the HTTP method (e.g. {@code "GET"}, {@code "POST"}); must not be {@code null}
	 * @param uri    the target URI; must not be {@code null}
	 * @return a new {@link Builder}
	 */
	public static Builder newBuilder(String method, URI uri) {
		return new Builder(method, uri);
	}

	/**
	 * Creates a new {@link Builder} pre-populated from an existing {@link HttpRequest}, optionally targeting a
	 * different URI. The method, all headers, and the body are copied from {@code original}; the URI is replaced by
	 * {@code uri}.
	 *
	 * @param original the request to copy from; must not be {@code null}
	 * @param uri      the target URI for the new request; must not be {@code null}
	 * @return a new {@link Builder} seeded with the original's method, headers, and body
	 */
	public static Builder copyOf(HttpRequest original, URI uri) {
		return new Builder(original.method, uri)
				.headers(original.headers)
				.body(original.body);
	}

	/**
	 * Builder for {@link HttpRequest}.
	 *
	 * <p>
	 * The HTTP method and target URI are required and must be supplied at construction time via
	 * {@link HttpRequest#newBuilder(String, URI)}. Headers and body are optional and may be added incrementally before
	 * calling {@link #build()}.
	 */
	public static final class Builder {
		private final String method;
		private final URI uri;
		private final List<HttpHeader> headers = new ArrayList<>();
		private HttpRequestBody body;

		private Builder(String method, URI uri) {
			this.method = method;
			this.uri = uri;
		}

		/**
		 * Appends a single header to the request. Multiple calls with the same name are additive; no deduplication is
		 * performed.
		 *
		 * @param name  the header name; must not be {@code null}
		 * @param value the header value; must not be {@code null}
		 * @return this builder
		 */
		public Builder header(String name, String value) {
			headers.add(HttpHeader.of(name, value));
			return this;
		}

		/**
		 * Appends all headers from the given list to the request. Multiple calls are additive.
		 *
		 * @param hdrs the list of {@link HttpHeader}s to add; must not be {@code null}
		 * @return this builder
		 */
		public Builder headers(List<HttpHeader> hdrs) {
			headers.addAll(hdrs);
			return this;
		}

		/**
		 * Sets the request body. Pass {@code null} or omit this call for bodyless requests (e.g. {@code GET},
		 * {@code DELETE}).
		 *
		 * @param body the {@link HttpRequestBody} to set, or {@code null} for no body
		 * @return this builder
		 */
		public Builder body(HttpRequestBody body) {
			this.body = body;
			return this;
		}

		/**
		 * Builds and returns the immutable {@link HttpRequest}.
		 *
		 * @return a new {@link HttpRequest}
		 */
		public HttpRequest build() {
			return new HttpRequest(this);
		}
	}
}
