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
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Represents an HTTP response. Must be closed (via {@link #close()} or try-with-resources) to release any underlying
 * connection resources.
 */
public interface HttpClientResponse extends AutoCloseable {

	/**
	 * @return the HTTP status code.
	 */
	int getStatusCode();

	/**
	 * @return the HTTP reason phrase, or an empty string if not available.
	 */
	String getReasonPhrase();

	/**
	 * @return all response headers.
	 */
	List<HttpClientHeader> getHeaders();

	/**
	 * Returns the first header value for the given name (case-insensitive).
	 *
	 * @param name the header name.
	 * @return the first matching header value, or {@link Optional#empty()}.
	 */
	default Optional<String> getHeader(String name) {
		return getHeaders(name).stream().map(HttpClientHeader::getValue).findFirst();
	}

	/**
	 * Returns all headers matching the given name (case-insensitive).
	 *
	 * @param name the header name.
	 * @return matching headers, possibly empty.
	 */
	default List<HttpClientHeader> getHeaders(String name) {
		return getHeaders().stream()
				.filter(h -> h.getName().equalsIgnoreCase(name))
				.toList();
	}

	/**
	 * @return an {@link InputStream} over the response body.
	 * @throws IOException if reading fails.
	 */
	InputStream getBodyAsStream() throws IOException;

	/**
	 * Discards the response body, releasing any underlying resources.
	 *
	 * @throws IOException if discarding fails.
	 */
	void discard() throws IOException;

	/**
	 * Discards the response body, silently ignoring any {@link IOException}.
	 */
	default void discardQuietly() {
		try {
			discard();
		} catch (IOException e) {
			// ignore
		}
	}

	/**
	 * Closes this response, releasing any underlying connection or stream resources.
	 */
	@Override
	void close();

	/**
	 * Discards the response body and closes this response, releasing the underlying connection. Any {@link IOException}
	 * thrown by {@link #discard()} is silently ignored; {@link #close()} is always called.
	 */
	default void discardAndClose() {
		try {
			discard();
		} catch (IOException e) {
			// ignore; connection will still be released by close()
		} finally {
			close();
		}
	}
}
