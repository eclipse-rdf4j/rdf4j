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
 * The super class of exceptions originating from {@link QueryResultHandler} implementations.
 *
 * @author Peter Ansell
 */
public class QueryResultHandlerException extends RDF4JException {

	private static final long serialVersionUID = 5096811224670124398L;

	/**
	 * Creates a new QueryResultHandlerException.
	 *
	 * @param msg An error message.
	 */
	public QueryResultHandlerException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new QueryResultHandlerException wrapping another exception.
	 *
	 * @param t The cause of the exception.
	 */
	public QueryResultHandlerException(Throwable t) {
		super(t);
	}

	/**
	 * Creates a new QueryResultHandlerException wrapping another exception.
	 *
	 * @param msg An error message.
	 * @param t   The cause of the exception.
	 */
	public QueryResultHandlerException(String msg, Throwable t) {
		super(msg, t);
	}

}
