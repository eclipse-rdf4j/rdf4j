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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

/**
 * Utility methods for working with {@link HttpClientResponse} instances.
 */
public final class HttpClientResponses {

	private HttpClientResponses() {
	}

	/**
	 * Reads the response body into a {@link String}, using the charset declared in the {@code Content-Type} header. If
	 * no {@code charset} parameter is present, {@code ISO-8859-1} is used as the default, consistent with the HTTP/1.1
	 * specification and the behaviour of Apache HttpClient's {@code EntityUtils.toString()}.
	 *
	 * @param response the response whose body is to be read; the caller is responsible for closing the response
	 * @return the response body as a string
	 * @throws IOException if reading the body fails
	 */
	public static String toString(HttpClientResponse response) throws IOException {
		return toString(response, StandardCharsets.ISO_8859_1);
	}

	/**
	 * Reads the response body into a {@link String}, using the charset declared in the {@code Content-Type} header. If
	 * no {@code charset} parameter is present, {@code defaultCharset} is used instead.
	 *
	 * @param response       the response whose body is to be read; the caller is responsible for closing the response
	 * @param defaultCharset the charset to use when none is declared in the {@code Content-Type} header
	 * @return the response body as a string
	 * @throws IOException if reading the body fails
	 */
	public static String toString(HttpClientResponse response, Charset defaultCharset) throws IOException {
		Charset charset = extractCharset(response, defaultCharset);
		byte[] bytes = response.getBodyAsStream().readAllBytes();
		return new String(bytes, charset);
	}

	/**
	 * Extracts the {@code charset} parameter from the response's {@code Content-Type} header.
	 *
	 * @param response       the response to inspect
	 * @param defaultCharset fallback charset if none is declared
	 * @return the resolved charset
	 */
	private static Charset extractCharset(HttpClientResponse response, Charset defaultCharset) {
		return response.getHeader("Content-Type")
				.map(ct -> parseCharset(ct, defaultCharset))
				.orElse(defaultCharset);
	}

	/**
	 * Parses a {@code charset=} parameter from a {@code Content-Type} header value such as
	 * {@code text/html; charset=utf-8} or {@code application/json; charset="UTF-8"}.
	 *
	 * @param contentType    the raw {@code Content-Type} header value
	 * @param defaultCharset fallback charset if no {@code charset=} parameter is found or the name is unrecognised
	 * @return the resolved charset
	 */
	private static Charset parseCharset(String contentType, Charset defaultCharset) {
		for (String part : contentType.split(";")) {
			String trimmed = part.trim();
			if (trimmed.toLowerCase(Locale.ROOT).startsWith("charset=")) {
				String name = trimmed.substring("charset=".length()).trim();
				// Strip optional surrounding quotes: charset="utf-8"
				if (name.startsWith("\"") && name.endsWith("\"")) {
					name = name.substring(1, name.length() - 1);
				}
				try {
					return Charset.forName(name);
				} catch (UnsupportedCharsetException e) {
					return defaultCharset;
				}
			}
		}
		return defaultCharset;
	}
}
