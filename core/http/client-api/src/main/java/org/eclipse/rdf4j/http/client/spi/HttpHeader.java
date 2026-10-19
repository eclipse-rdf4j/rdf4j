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

/**
 * An immutable HTTP header name/value pair.
 */
public final class HttpHeader {

	private final String name;
	private final String value;

	private HttpHeader(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Creates a new {@link HttpHeader} with the given name and value.
	 *
	 * @param name  the header name; must not be {@code null}
	 * @param value the header value; must not be {@code null}
	 * @return a new {@link HttpHeader}
	 */
	public static HttpHeader of(String name, String value) {
		return new HttpHeader(name, value);
	}

	/**
	 * Returns the header name.
	 *
	 * @return the header name; never {@code null}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the header value.
	 *
	 * @return the header value; never {@code null}
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return name + ": " + value;
	}
}
