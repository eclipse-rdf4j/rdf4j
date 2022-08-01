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
package org.eclipse.rdf4j.query;

/**
 * An exception that can be thrown by an TupleQueryResultHandler when it encounters an unrecoverable error. If an
 * exception is associated with the error then this exception can be wrapped in a TupleHandlerException and can later be
 * retrieved from it when the TupleHandlerException is caught using the <var>getCause()</var>.
 */
public class TupleQueryResultHandlerException extends QueryResultHandlerException {

	private static final long serialVersionUID = 8530574857852836665L;

	/**
	 * Creates a new TupleQueryResultHandlerException.
	 *
	 * @param msg An error message.
	 */
	public TupleQueryResultHandlerException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new TupleQueryResultHandlerException wrapping another exception.
	 *
	 * @param cause The cause of the exception.
	 */
	public TupleQueryResultHandlerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new TupleQueryResultHandlerException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public TupleQueryResultHandlerException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
