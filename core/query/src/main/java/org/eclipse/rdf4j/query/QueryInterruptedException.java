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
 * An exception indicating that the evaluation of a query has been interrupted, for example because it took too long to
 * complete.
 *
 * @author Arjohn Kampman
 */
public class QueryInterruptedException extends QueryEvaluationException {

	private static final long serialVersionUID = -1261311645990563247L;

	public QueryInterruptedException() {
		super();
	}

	public QueryInterruptedException(String message) {
		super(message);
	}

	public QueryInterruptedException(String message, Throwable t) {
		super(message, t);
	}

	public QueryInterruptedException(Throwable t) {
		super(t);
	}
}
