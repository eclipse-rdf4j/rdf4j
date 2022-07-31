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

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception indicating that a query could not be processed by the query parser, typically due to syntax errors.
 *
 * @author jeen
 * @author Herko ter Horst
 */
public class MalformedQueryException extends RDF4JException {

	private static final long serialVersionUID = 1210214405486786142L;

	public MalformedQueryException() {
		super();
	}

	public MalformedQueryException(String message) {
		super(message);
	}

	public MalformedQueryException(Throwable t) {
		super(t);
	}

	public MalformedQueryException(String message, Throwable t) {
		super(message, t);
	}
}
