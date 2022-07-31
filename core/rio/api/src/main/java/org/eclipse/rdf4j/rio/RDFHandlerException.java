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
package org.eclipse.rdf4j.rio;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception that can be thrown by an RDFHandler when it encounters an unrecoverable error. If an exception is
 * associated with the error then this exception can be wrapped in an RDFHandlerException and can later be retrieved
 * from it when the RDFHandlerException is catched using the <var>getCause()</var>.
 */
public class RDFHandlerException extends RDF4JException {

	private static final long serialVersionUID = -1931215293637533642L;

	/**
	 * Creates a new RDFHandlerException.
	 *
	 * @param msg An error message.
	 */
	public RDFHandlerException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new RDFHandlerException.
	 *
	 * @param cause The cause of the exception.
	 */
	public RDFHandlerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new RDFHandlerException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public RDFHandlerException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
