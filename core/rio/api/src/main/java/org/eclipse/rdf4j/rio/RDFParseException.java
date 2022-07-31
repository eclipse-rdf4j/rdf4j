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
 * A parse exception that can be thrown by a parser when it encounters an error from which it cannot or doesn't want to
 * recover.
 */
public class RDFParseException extends RDF4JException {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final long serialVersionUID = -4686126837948873012L;

	private final long lineNo;

	private final long columnNo;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new ParseException.
	 *
	 * @param msg An error message.
	 */
	public RDFParseException(String msg) {
		this(msg, -1, -1);
	}

	/**
	 * Creates a new ParseException.
	 *
	 * @param msg      An error message.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public RDFParseException(String msg, long lineNo, long columnNo) {
		super(msg + getLocationString(lineNo, columnNo));
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/**
	 * Creates a new ParseException wrapping another exception. The ParseException will inherit its message from the
	 * supplied source exception.
	 *
	 * @param t The source exception.
	 */
	public RDFParseException(Throwable t) {
		this(t, -1, -1);
	}

	/**
	 * Creates a new ParseException wrapping another exception. The ParseException will inherit its message from the
	 * supplied source exception.
	 *
	 * @param msg An error message.
	 * @param t   The source exception.
	 */
	public RDFParseException(String msg, Throwable t) {
		this(msg, t, -1, -1);
	}

	/**
	 * Creates a new ParseException wrapping another exception. The ParseException will inherit its message from the
	 * supplied source exception.
	 *
	 * @param t        The source exception.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public RDFParseException(Throwable t, long lineNo, long columnNo) {
		super(t.getMessage() + getLocationString(lineNo, columnNo), t);
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/**
	 * Creates a new ParseException wrapping another exception. The ParseException will inherit its message from the
	 * supplied source exception.
	 *
	 * @param t        The source exception.
	 * @param lineNo   A line number associated with the message.
	 * @param columnNo A column number associated with the message.
	 */
	public RDFParseException(String msg, Throwable t, long lineNo, long columnNo) {
		super(msg + getLocationString(lineNo, columnNo), t);
		this.lineNo = lineNo;
		this.columnNo = columnNo;
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the line number associated with this parse exception.
	 *
	 * @return A line number, or -1 if no line number is available or applicable.
	 */
	public long getLineNumber() {
		return lineNo;
	}

	/**
	 * Gets the column number associated with this parse exception.
	 *
	 * @return A column number, or -1 if no column number is available or applicable.
	 */
	public long getColumnNumber() {
		return columnNo;
	}

	/**
	 * Creates a string to that shows the specified line and column number. Negative line numbers are interpreted as
	 * unknowns. Example output: "[line 12, column 34]". If the specified line number is negative, this method returns
	 * an empty string.
	 */
	public static String getLocationString(long lineNo, long columnNo) {
		if (lineNo < 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder(16);
		sb.append(" [line ");
		sb.append(lineNo);

		if (columnNo >= 0) {
			sb.append(", column ");
			sb.append(columnNo);
		}

		sb.append("]");
		return sb.toString();
	}
}
