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
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import org.eclipse.rdf4j.http.client.spi.HttpHeader;
import org.eclipse.rdf4j.http.client.spi.HttpRequestBody;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientConfig;

/**
 * {@link RDF4JHttpClient} implementation backed by {@code java.net.http.HttpClient}.
 */
public class JdkRDF4JHttpClient implements RDF4JHttpClient {

	private final HttpClient httpClient;
	private final RDF4JHttpClientConfig config;

	public JdkRDF4JHttpClient(HttpClient httpClient, RDF4JHttpClientConfig config) {
		this.httpClient = httpClient;
		this.config = config;
	}

	@Override
	public org.eclipse.rdf4j.http.client.spi.HttpResponse execute(org.eclipse.rdf4j.http.client.spi.HttpRequest request)
			throws IOException {
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(request.getUri());

			// Set default headers from config, then per-request headers
			for (HttpHeader header : config.getDefaultHeaders()) {
				builder.header(header.getName(), header.getValue());
			}
			for (HttpHeader header : request.getHeaders()) {
				builder.header(header.getName(), header.getValue());
			}

			// Set socket timeout if configured
			if (config.getSocketTimeoutMs() > 0) {
				builder.timeout(Duration.ofMillis(config.getSocketTimeoutMs()));
			}

			// Build body publisher
			BodyPublisher bodyPublisher;
			if (request.getBody().isPresent()) {
				HttpRequestBody body = request.getBody().get();
				builder.header("Content-Type", body.getContentType());
				if (body.isRepeatable()) {
					// Buffered body: read into byte[] (safe for redirect retries) and close the stream
					try (InputStream in = body.getContent()) {
						bodyPublisher = BodyPublishers.ofByteArray(in.readAllBytes());
					}
				} else {
					// Streaming body: pass directly to avoid buffering large payloads into memory;
					// the JDK HttpClient closes the stream after the request is sent
					bodyPublisher = BodyPublishers.ofInputStream(() -> {
						try {
							return body.getContent();
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					});
				}
			} else {
				bodyPublisher = BodyPublishers.noBody();
			}

			builder.method(request.getMethod(), bodyPublisher);

			HttpRequest jdkRequest = builder.build();
			HttpResponse<java.io.InputStream> response = httpClient.send(jdkRequest, BodyHandlers.ofInputStream());
			return new JdkHttpClientResponse(response);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("HTTP request interrupted", e);
		}
	}

	@Override
	public void close() {
		// JDK HttpClient does not require explicit closing (resource-managed in JDK 21+, no-op here).
	}
}
