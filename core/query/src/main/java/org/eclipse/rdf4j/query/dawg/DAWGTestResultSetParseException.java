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
package org.eclipse.rdf4j.query.dawg;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception that is thrown to indicate that the parsing of a DAWG Test Result Set graph failed due to an
 * incompatible or incomplete graph.
 */
public class DAWGTestResultSetParseException extends RDF4JException {

	private static final long serialVersionUID = -8655777672973690037L;

	/**
	 * Creates a new DAWGTestResultSetParseException.
	 *
	 * @param msg An error message.
	 */
	public DAWGTestResultSetParseException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new DAWGTestResultSetParseException wrapping another exception.
	 *
	 * @param cause The cause of the exception.
	 */
	public DAWGTestResultSetParseException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new DAWGTestResultSetParseException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public DAWGTestResultSetParseException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
