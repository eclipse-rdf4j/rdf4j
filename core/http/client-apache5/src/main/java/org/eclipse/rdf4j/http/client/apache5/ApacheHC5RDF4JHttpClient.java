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
import java.net.HttpURLConnection;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.eclipse.rdf4j.http.client.spi.HttpClientHeader;
import org.eclipse.rdf4j.http.client.spi.HttpClientRequest;
import org.eclipse.rdf4j.http.client.spi.HttpClientRequestBody;
import org.eclipse.rdf4j.http.client.spi.HttpClientResponse;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RDF4JHttpClient} implementation backed by Apache HttpComponents 5.
 */
public class ApacheHC5RDF4JHttpClient implements RDF4JHttpClient {

	private static final Logger logger = LoggerFactory.getLogger(ApacheHC5RDF4JHttpClient.class);

	private final CloseableHttpClient httpClient;

	/**
	 * Maximum number of 408 retries (= maxConnectionsPerRoute + 1, to drain all stale pooled connections).
	 */
	private final int maxRetries408;

	ApacheHC5RDF4JHttpClient(CloseableHttpClient httpClient, int maxConnectionsPerRoute) {
		this.httpClient = httpClient;
		this.maxRetries408 = maxConnectionsPerRoute + 1;
	}

	@Override
	public HttpClientResponse execute(HttpClientRequest request) throws IOException {
		boolean repeatable = request.getBody().map(HttpClientRequestBody::isRepeatable).orElse(true);

		// Use executeOpen to get a streaming response (caller closes to return connection to pool).
		// Response-based retry (HTTP 408) is not applied automatically by executeOpen, so we implement
		// the retry loop here — but only for repeatable (byte-backed) bodies. Single-use stream bodies
		// cannot be re-sent, so retry is skipped and the 408 propagates to the caller.
		ClassicHttpResponse hcResponse = httpClient.executeOpen(null, buildRequest(request), null);
		if (!repeatable) {
			return new ApacheHC5HttpClientResponse(hcResponse);
		}
		for (int attempt = 1; hcResponse.getCode() == HttpURLConnection.HTTP_CLIENT_TIMEOUT
				&& attempt <= maxRetries408; attempt++) {
			try {
				hcResponse.close();
			} catch (Exception ignore) {
			}
			logger.info("Retrying request after HTTP 408 (attempt {})", attempt);
			hcResponse = httpClient.executeOpen(null, buildRequest(request), null);
		}
		return new ApacheHC5HttpClientResponse(hcResponse);
	}

	private HttpUriRequestBase buildRequest(HttpClientRequest request) throws IOException {
		String method = request.getMethod();
		String uri = request.getUri().toASCIIString();

		HttpUriRequestBase hcRequest = switch (method) {
		case "GET" -> new HttpGet(uri);
		case "POST" -> new HttpPost(uri);
		case "PUT" -> new HttpPut(uri);
		case "DELETE" -> new HttpDelete(uri);
		default -> new HttpUriRequestBase(method, request.getUri());
		};

		// Set headers
		for (HttpClientHeader header : request.getHeaders()) {
			hcRequest.addHeader(header.getName(), header.getValue());
		}

		// Set body. Repeatable (byte-backed) bodies use ByteArrayEntity so that Apache HC5's
		// RedirectExec can follow redirects and the 408 retry loop can re-send the request.
		// Single-use stream bodies use InputStreamEntity (non-repeatable); RedirectExec will
		// skip redirect following for those automatically.
		if (request.getBody().isPresent()) {
			HttpClientRequestBody body = request.getBody().get();
			ContentType contentType = ContentType.parse(body.getContentType());
			if (body.isRepeatable()) {
				hcRequest.setEntity(new ByteArrayEntity(body.getContent().readAllBytes(), contentType));
			} else {
				hcRequest.setEntity(new InputStreamEntity(body.getContent(), body.getContentLength(), contentType));
			}
		}

		return hcRequest;
	}

	@Override
	public void close() {
		try {
			httpClient.close();
		} catch (IOException e) {
			// ignore on close
		}
	}
}
