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
 * An exception indicating that the evaluation of a query failed.
 *
 * @author Arjohn Kampman
 */
public class QueryEvaluationException extends RDF4JException {

	private static final long serialVersionUID = 602749602257031631L;

	public QueryEvaluationException() {
		super();
	}

	/**
	 * Creates a new TupleQueryResultHandlerException.
	 *
	 * @param msg An error message.
	 */
	public QueryEvaluationException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new TupleQueryResultHandlerException wrapping another exception.
	 *
	 * @param cause The cause of the exception.
	 */
	public QueryEvaluationException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new TupleQueryResultHandlerException wrapping another exception.
	 *
	 * @param msg   An error message.
	 * @param cause The cause of the exception.
	 */
	public QueryEvaluationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
