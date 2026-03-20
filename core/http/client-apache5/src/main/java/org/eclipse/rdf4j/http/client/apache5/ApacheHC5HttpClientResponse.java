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
package org.eclipse.rdf4j.http.client.apache5;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.eclipse.rdf4j.http.client.spi.HttpClientHeader;
import org.eclipse.rdf4j.http.client.spi.HttpClientResponse;

/**
 * {@link HttpClientResponse} backed by an Apache HC5 {@link ClassicHttpResponse}.
 * <p>
 * Closing this response returns the connection to the pool.
 */
class ApacheHC5HttpClientResponse implements HttpClientResponse {

	private final ClassicHttpResponse response;
	private final List<HttpClientHeader> headers;

	ApacheHC5HttpClientResponse(ClassicHttpResponse response) {
		this.response = response;
		this.headers = Arrays.stream(response.getHeaders())
				.map(h -> HttpClientHeader.of(h.getName(), h.getValue()))
				.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public int getStatusCode() {
		return response.getCode();
	}

	@Override
	public String getReasonPhrase() {
		String reason = response.getReasonPhrase();
		return reason != null ? reason : "";
	}

	@Override
	public List<HttpClientHeader> getHeaders() {
		return headers;
	}

	@Override
	public InputStream getBodyAsStream() throws IOException {
		var entity = response.getEntity();
		if (entity == null) {
			return InputStream.nullInputStream();
		}
		return entity.getContent();
	}

	@Override
	public void discard() throws IOException {
		var entity = response.getEntity();
		if (entity == null) {
			return;
		}
		if (entity.isStreaming()) {
			final InputStream inStream = entity.getContent();
			if (inStream != null) {
				inStream.close();
			}
		}
	}

	@Override
	public void close() {
		try {
			response.close();
		} catch (IOException e) {
			// ignore on close
		}
	}
}
