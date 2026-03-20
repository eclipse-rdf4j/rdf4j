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
import java.util.stream.Collectors;

import org.eclipse.rdf4j.http.client.spi.HttpClientHeader;
import org.eclipse.rdf4j.http.client.spi.HttpClientResponse;

/**
 * {@link HttpClientResponse} backed by a JDK {@link HttpResponse}.
 */
class JdkHttpClientResponse implements HttpClientResponse {

	private final HttpResponse<InputStream> response;
	private final List<HttpClientHeader> headers;

	JdkHttpClientResponse(HttpResponse<InputStream> response) {
		this.response = response;
		this.headers = response.headers()
				.map()
				.entrySet()
				.stream()
				.flatMap(e -> e.getValue().stream().map(v -> HttpClientHeader.of(e.getKey(), v)))
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public int getStatusCode() {
		return response.statusCode();
	}

	@Override
	public String getReasonPhrase() {
		return "";
	}

	@Override
	public List<HttpClientHeader> getHeaders() {
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
