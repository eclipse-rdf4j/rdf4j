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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Represents the body of an HTTP request.
 * <p>
 * All built-in factory methods except {@link #ofStream} return re-readable (byte[]-backed) bodies, which makes redirect
 * retry safe. The {@link #ofStream} factory is single-use.
 */
public interface HttpRequestBody {

	/**
	 * @return the MIME content type of this body, e.g. {@code application/x-www-form-urlencoded}.
	 */
	String getContentType();

	/**
	 * @return an {@link InputStream} over the body content.
	 * @throws IOException if reading fails.
	 */
	InputStream getContent() throws IOException;

	/**
	 * @return the content length in bytes, or {@code -1} if unknown.
	 */
	long getContentLength();

	/**
	 * Returns {@code true} if the body can be read multiple times (e.g. for retry or redirect), {@code false} if it is
	 * a single-use stream.
	 * <p>
	 * Implementations created by {@link #ofBytes}, {@link #ofString}, and {@link #ofFormData} return {@code true}.
	 * Implementations created by {@link #ofStream} return {@code false}.
	 *
	 * @return {@code true} if the body is buffered and repeatable
	 */
	default boolean isRepeatable() {
		return false;
	}

	/**
	 * Creates a body from URL-encoded form data.
	 *
	 * @param params the form parameters.
	 * @return a re-readable {@link HttpRequestBody}.
	 */
	static HttpRequestBody ofFormData(List<NameValuePair> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (NameValuePair nvp : params) {
			if (!first) {
				sb.append('&');
			}
			first = false;
			sb.append(URLEncoder.encode(nvp.getName(), StandardCharsets.UTF_8));
			sb.append('=');
			sb.append(URLEncoder.encode(nvp.getValue(), StandardCharsets.UTF_8));
		}
		byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
		return ofBytes(bytes, "application/x-www-form-urlencoded; charset=utf-8");
	}

	/**
	 * Creates a body from a string with the given content type and charset.
	 *
	 * @param content     the string content.
	 * @param contentType the MIME type.
	 * @param charset     the charset used to encode the string.
	 * @return a re-readable {@link HttpRequestBody}.
	 */
	static HttpRequestBody ofString(String content, String contentType, Charset charset) {
		byte[] bytes = content.getBytes(charset);
		return ofBytes(bytes, contentType);
	}

	/**
	 * Creates a body from a byte array.
	 *
	 * @param bytes       the content bytes.
	 * @param contentType the MIME type.
	 * @return a re-readable {@link HttpRequestBody}.
	 */
	static HttpRequestBody ofBytes(byte[] bytes, String contentType) {
		return new HttpRequestBody() {
			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public InputStream getContent() throws IOException {
				return new ByteArrayInputStream(bytes);
			}

			@Override
			public long getContentLength() {
				return bytes.length;
			}

			@Override
			public boolean isRepeatable() {
				return true;
			}
		};
	}

	/**
	 * Creates a body from a streaming {@link InputStream}.
	 * <p>
	 * <b>Note:</b> This body is single-use. If the HTTP implementation needs to retry (e.g., on redirect), it will fail
	 * because the stream cannot be re-read. Prefer the byte[]-backed factories for retry-safe bodies.
	 *
	 * @param stream        the content stream.
	 * @param contentType   the MIME type.
	 * @param contentLength the content length in bytes, or {@code -1} if unknown.
	 * @return a single-use {@link HttpRequestBody}.
	 */
	static HttpRequestBody ofStream(InputStream stream, String contentType, long contentLength) {
		if (stream instanceof ByteArrayInputStream bais) {
			return ofBytes(bais.readAllBytes(), contentType);
		}
		return new HttpRequestBody() {
			@Override
			public String getContentType() {
				return contentType;
			}

			@Override
			public InputStream getContent() throws IOException {
				return stream;
			}

			@Override
			public long getContentLength() {
				return contentLength;
			}
		};
	}
}
