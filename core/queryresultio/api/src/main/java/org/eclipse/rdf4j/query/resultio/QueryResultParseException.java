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
package org.eclipse.rdf4j.query.resultio;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * A parse exception that can be thrown by a query result parser when it encounters an error from which it cannot or
 * doesn't want to recover.
 *
 * @author Arjohn Kampman
 */
public class QueryResultParseException extends RDF4JException {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -6212290295459157916L;

	/*-----------*
	 * Variables *
	 *-----------*/

	private long lineNo = -1L;

	private long columnNo = -1L;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new QueryResultParseException.
	 *
	 * @param msg An error message.
	 */
	public QueryResultParseException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new QueryResultParseException wrapping another exception.
	 *
	 * @param msg An error message.
	 * @param t   The source exception.
	 */
	public QueryResultParseException(String msg, Throwable t) {
		super(msg, t);
	}

	/**
	 * Creates a new QueryResultParseException.
	 *
	 * @param msg      An error message.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public QueryResultParseException(String msg, long lineNo, long columnNo) {
		super(msg);
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/**
	 * Creates a new QueryResultParseException wrapping another exception. The QueryResultParseException will inherit
	 * its message from the supplied source exception.
	 *
	 * @param t The source exception.
	 */
	public QueryResultParseException(Throwable t) {
		super(t);
	}

	/**
	 * Creates a new QueryResultParseException wrapping another exception. The QueryResultParseException will inherit
	 * its message from the supplied source exception.
	 *
	 * @param t        The source exception.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public QueryResultParseException(Throwable t, long lineNo, long columnNo) {
		super(t);
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/**
	 * Creates a new QueryResultParseException wrapping another exception.
	 *
	 * @param msg      An error message.
	 * @param t        The source exception.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public QueryResultParseException(String msg, Throwable t, long lineNo, long columnNo) {
		super(msg, t);
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * Gets the line number associated with this parse exception.
	 *
	 * @return A line number, or <var>-1</var> if no line number is available or applicable.
	 */
	public long getLineNumber() {
		return lineNo;
	}

	/**
	 * Gets the column number associated with this parse exception.
	 *
	 * @return A column number, or <var>-1</var> if no column number is available or applicable.
	 */
	public long getColumnNumber() {
		return columnNo;
	}
}
