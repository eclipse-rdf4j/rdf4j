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
 * A RuntimeException indicating that a specific query language is not supported. A typical cause of this exception is
 * that the class library for the specified query language is not present in the classpath.
 */
public class UnsupportedQueryLanguageException extends RuntimeException {

	private static final long serialVersionUID = -2709196386078518696L;

	/**
	 * Creates a new UnsupportedRDFormatException.
	 *
	 * @param msg An error message.
	 */
	public UnsupportedQueryLanguageException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new UnsupportedRDFormatException.
	 *
	 * @param cause The cause of the exception.
	 */
	public UnsupportedQueryLanguageException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new UnsupportedRDFormatException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public UnsupportedQueryLanguageException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
