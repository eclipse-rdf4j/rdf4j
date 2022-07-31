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

/**
 * A RuntimeException indicating that a specific RDF format is not supported. A typical cause of this exception is that
 * the class library for the specified RDF format is not present in the classpath.
 *
 * @author jeen
 */
public class UnsupportedRDFormatException extends RuntimeException {

	private static final long serialVersionUID = -2709196386078518696L;

	/**
	 * Creates a new UnsupportedRDFormatException.
	 *
	 * @param msg An error message.
	 */
	public UnsupportedRDFormatException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new UnsupportedRDFormatException.
	 *
	 * @param cause The cause of the exception.
	 */
	public UnsupportedRDFormatException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new UnsupportedRDFormatException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public UnsupportedRDFormatException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
