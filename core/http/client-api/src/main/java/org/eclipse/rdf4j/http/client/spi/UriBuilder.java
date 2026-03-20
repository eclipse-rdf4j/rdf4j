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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for building URIs with query parameters.
 */
public final class UriBuilder {

	private final String baseUrl;
	private final List<NameValuePair> params = new ArrayList<>();

	private UriBuilder(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Creates a new {@link UriBuilder} starting from the given base URL. The base URL may already contain a query
	 * string.
	 *
	 * @param baseUrl the base URL; must not be {@code null}
	 * @return a new {@link UriBuilder}
	 */
	public static UriBuilder from(String baseUrl) {
		return new UriBuilder(baseUrl);
	}

	/**
	 * Appends a query parameter. Multiple calls are additive.
	 *
	 * @param name  the parameter name; must not be {@code null}
	 * @param value the parameter value; must not be {@code null}
	 * @return this builder
	 */
	public UriBuilder addParameter(String name, String value) {
		params.add(NameValuePair.of(name, value));
		return this;
	}

	/**
	 * Appends a query parameter. Multiple calls are additive.
	 *
	 * @param param the parameter; must not be {@code null}
	 * @return this builder
	 */
	public UriBuilder addParameter(NameValuePair param) {
		params.add(param);
		return this;
	}

	/**
	 * Builds and returns the URI with all appended query parameters URL-encoded.
	 *
	 * @return the resulting {@link URI}
	 */
	public URI build() {
		if (params.isEmpty()) {
			return URI.create(baseUrl);
		}
		StringBuilder sb = new StringBuilder(baseUrl);
		boolean first = !baseUrl.contains("?");
		for (NameValuePair nvp : params) {
			if (nvp.getValue() == null) {
				continue;
			}
			sb.append(first ? '?' : '&');
			first = false;
			sb.append(URLEncoder.encode(nvp.getName(), StandardCharsets.UTF_8));
			sb.append('=');
			sb.append(URLEncoder.encode(nvp.getValue(), StandardCharsets.UTF_8));
		}
		return URI.create(sb.toString());
	}

	@Override
	public String toString() {
		return build().toASCIIString();
	}
}
