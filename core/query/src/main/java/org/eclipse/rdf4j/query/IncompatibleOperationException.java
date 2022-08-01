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
 * An exception indicating that a string could not be parsed into an operation of the expected type by the parser.
 *
 * @author jeen
 */
public class IncompatibleOperationException extends MalformedQueryException {

	/**
	 *
	 */
	private static final long serialVersionUID = -4926665776729656410L;

	public IncompatibleOperationException() {
		super();
	}

	public IncompatibleOperationException(String message) {
		super(message);
	}

	public IncompatibleOperationException(Throwable t) {
		super(t);
	}

	public IncompatibleOperationException(String message, Throwable t) {
		super(message, t);
	}
}
