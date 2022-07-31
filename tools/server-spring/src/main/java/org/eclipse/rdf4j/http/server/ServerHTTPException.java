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
 * HTTP-related exception indicating that an error occurred in a server. Status codes for these types of errors are in
 * the 5xx range. The default status code for constructors without a <var>statusCode</var> parameter is <var>500
 * Internal Server Error</var>.
 *
 * @author Arjohn Kampman
 */
public class ServerHTTPException extends HTTPException {

	private static final long serialVersionUID = -3949837199542648966L;

	private static final int DEFAULT_STATUS_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

	/**
	 * Creates a {@link ServerHTTPException} with status code 500 "Internal Server Error".
	 */
	public ServerHTTPException() {
		this(DEFAULT_STATUS_CODE);
	}

	/**
	 * Creates a {@link ServerHTTPException} with status code 500 "Internal Server Error".
	 */
	public ServerHTTPException(String msg) {
		this(DEFAULT_STATUS_CODE, msg);
	}

	/**
	 * Creates a {@link ServerHTTPException} with status code 500 "Internal Server Error".
	 */
	public ServerHTTPException(String msg, Throwable t) {
		this(DEFAULT_STATUS_CODE, t);
	}

	/**
	 * Creates a {@link ServerHTTPException} with the specified status code. The supplied status code must be in the 5xx
	 * range.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 5xx range.
	 */
	public ServerHTTPException(int statusCode) {
		super(statusCode);
	}

	/**
	 * Creates a {@link ServerHTTPException} with the specified status code. The supplied status code must be in the 5xx
	 * range.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 5xx range.
	 */
	public ServerHTTPException(int statusCode, String message) {
		super(statusCode, message);
	}

	/**
	 * Creates a {@link ServerHTTPException} with the specified status code. The supplied status code must be in the 5xx
	 * range.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 5xx range.
	 */
	public ServerHTTPException(int statusCode, String message, Throwable t) {
		super(statusCode, message, t);
	}

	/**
	 * Creates a {@link ServerHTTPException} with the specified status code. The supplied status code must be in the 5xx
	 * range.
	 *
	 * @throws IllegalArgumentException If <var>statusCode</var> is not in the 5xx range.
	 */
	public ServerHTTPException(int statusCode, Throwable t) {
		super(statusCode, t);
	}

	@Override
	protected void setStatusCode(int statusCode) {
		if (statusCode < 500 || statusCode > 599) {
			throw new IllegalArgumentException("Status code must be in the 5xx range, is: " + statusCode);
		}

		super.setStatusCode(statusCode);
	}
}
