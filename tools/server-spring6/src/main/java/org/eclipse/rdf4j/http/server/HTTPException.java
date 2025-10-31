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

/**
 * HTTP-related exception that includes the relevant HTTP status code.
 *
 * @author Arjohn Kampman
 */
public class HTTPException extends Exception {

	private static final long serialVersionUID = 1356463348553827230L;

	private int statusCode;

	public HTTPException(int statusCode) {
		super();
		setStatusCode(statusCode);
	}

	public HTTPException(int statusCode, String message) {
		super(message);
		setStatusCode(statusCode);
	}

	public HTTPException(int statusCode, String message, Throwable t) {
		super(message, t);
		setStatusCode(statusCode);
	}

	public HTTPException(int statusCode, Throwable t) {
		super(t);
		setStatusCode(statusCode);
	}

	public final int getStatusCode() {
		return statusCode;
	}

	protected void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
}
