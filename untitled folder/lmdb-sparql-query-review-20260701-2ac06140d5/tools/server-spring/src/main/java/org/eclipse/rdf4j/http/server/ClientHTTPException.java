/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server;

import java.net.HttpURLConnection;

/**
 * HTTP-related exception indicating that an HTTP client has erred. Status codes for these types of errors are in the
 * 4xx range. The default status code for constructors without a <var>statusCode</var> parameter is <var>400 Bad
 * Request</var>.
 *
 * @author Arjohn Kampman
 */
public class ClientHTTPException extends HTTPException {

	private static final long serialVersionUID = 7722604284325312749L;

	private static final int DEFAULT_STATUS_CODE = HttpURLConnection.HTTP_BAD_REQUEST;

	/**
	 * Creates a {@link ClientHTTPException} with status code 400 "Bad Request".
	 */
	public ClientHTTPException() {
		this(DEFAULT_STATUS_CODE);
	}

	/**
	 * Creates a {@link ClientHTTPException} with status code 400 "Bad Request".
	 */
	public ClientHTTPException(String msg) {
		this(DEFAULT_STATUS_CODE, msg);
	}

	/**
	 * Creates a {@link ClientHTTPException} with status code 400 "Bad Request".
	 */
	public ClientHTTPException(String msg, Throwable t) {
		this(DEFAULT_STATUS_CODE, t);
	}

	/**
	 * Creates a {@link ClientHTTPException} with the specified status code.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 4xx range.
	 */
	public ClientHTTPException(int statusCode) {
		super(statusCode);
	}

	/**
	 * Creates a {@link ClientHTTPException} with the specified status code.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 4xx range.
	 */
	public ClientHTTPException(int statusCode, String message) {
		super(statusCode, message);
	}

	/**
	 * Creates a {@link ClientHTTPException} with the specified status code.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 4xx range.
	 */
	public ClientHTTPException(int statusCode, String message, Throwable t) {
		super(statusCode, message, t);
	}

	/**
	 * Creates a {@link ClientHTTPException} with the specified status code.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 4xx range.
	 */
	public ClientHTTPException(int statusCode, Throwable t) {
		super(statusCode, t);
	}

	@Override
	protected void setStatusCode(int statusCode) {
		if (statusCode < 400 || statusCode > 499) {
			throw new IllegalArgumentException("Status code must be in the 4xx range, is: " + statusCode);
		}

		super.setStatusCode(statusCode);
	}
}
