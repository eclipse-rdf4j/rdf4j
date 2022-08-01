/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.dao.exception;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class IncorrectResultSetSizeException extends RDF4JDaoException {
	int expectedSize;
	int actualSize;

	public IncorrectResultSetSizeException(int expectedSize, int actualSize) {
		super(makeMessage(expectedSize, actualSize));
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	private static String makeMessage(int expectedSize, int actualSize) {
		return String.format("Expected %d results but got %d", expectedSize, actualSize);
	}

	public IncorrectResultSetSizeException(String message, int expectedSize, int actualSize) {
		super(message);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public IncorrectResultSetSizeException(
			String message, Throwable cause, int expectedSize, int actualSize) {
		super(message, cause);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public IncorrectResultSetSizeException(Throwable cause, int expectedSize, int actualSize) {
		super(makeMessage(expectedSize, actualSize), cause);
		this.expectedSize = expectedSize;
		this.actualSize = actualSize;
	}

	public int getExpectedSize() {
		return expectedSize;
	}

	public int getActualSize() {
		return actualSize;
	}
}
