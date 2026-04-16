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
package org.eclipse.rdf4j.http.client.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.http.client.spi.HttpHeader;

/**
 * {@link HttpResponse} backed by a JDK {@link HttpResponse}.
 */
public class JdkHttpClientResponse implements org.eclipse.rdf4j.http.client.spi.HttpResponse {

	// The JDK HttpClient does not expose reason phrases (HTTP/2 removed them).
	// This table covers the standard codes most relevant for error reporting.
	private static final Map<Integer, String> REASON_PHRASES = Map.ofEntries(
			Map.entry(100, "Continue"),
			Map.entry(101, "Switching Protocols"),
			Map.entry(200, "OK"),
			Map.entry(201, "Created"),
			Map.entry(204, "No Content"),
			Map.entry(206, "Partial Content"),
			Map.entry(301, "Moved Permanently"),
			Map.entry(302, "Found"),
			Map.entry(303, "See Other"),
			Map.entry(304, "Not Modified"),
			Map.entry(307, "Temporary Redirect"),
			Map.entry(308, "Permanent Redirect"),
			Map.entry(400, "Bad Request"),
			Map.entry(401, "Unauthorized"),
			Map.entry(403, "Forbidden"),
			Map.entry(404, "Not Found"),
			Map.entry(405, "Method Not Allowed"),
			Map.entry(406, "Not Acceptable"),
			Map.entry(408, "Request Timeout"),
			Map.entry(409, "Conflict"),
			Map.entry(410, "Gone"),
			Map.entry(413, "Content Too Large"),
			Map.entry(414, "URI Too Long"),
			Map.entry(415, "Unsupported Media Type"),
			Map.entry(429, "Too Many Requests"),
			Map.entry(500, "Internal Server Error"),
			Map.entry(501, "Not Implemented"),
			Map.entry(502, "Bad Gateway"),
			Map.entry(503, "Service Unavailable"),
			Map.entry(504, "Gateway Timeout")
	);

	private final HttpResponse<InputStream> response;
	private final List<HttpHeader> headers;

	public JdkHttpClientResponse(HttpResponse<InputStream> response) {
		this.response = response;
		this.headers = response.headers()
				.map()
				.entrySet()
				.stream()
				.flatMap(e -> e.getValue().stream().map(v -> HttpHeader.of(e.getKey(), v)))
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public int getStatusCode() {
		return response.statusCode();
	}

	@Override
	public String getReasonPhrase() {
		return REASON_PHRASES.getOrDefault(response.statusCode(), "HTTP " + response.statusCode());
	}

	@Override
	public List<HttpHeader> getHeaders() {
		return headers;
	}

	@Override
	public InputStream getBodyAsStream() throws IOException {
		return response.body();
	}

	@Override
	public void discard() throws IOException {
		InputStream body = response.body();
		if (body != null) {
			body.transferTo(OutputStream.nullOutputStream());
			body.close();
		}
	}

	@Override
	public void close() {
		InputStream body = response.body();
		if (body != null) {
			try {
				body.close();
			} catch (IOException e) {
				// ignore on close
			}
		}
	}
}
