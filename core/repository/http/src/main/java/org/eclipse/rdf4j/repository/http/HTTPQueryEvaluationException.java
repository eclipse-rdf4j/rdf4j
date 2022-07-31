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
package org.eclipse.rdf4j.repository.http;

import java.io.IOException;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;

/**
 * @author Herko ter Horst
 */
public class HTTPQueryEvaluationException extends QueryEvaluationException {

	private static final long serialVersionUID = -8315025167877093272L;

	/**
	 * @param msg
	 */
	public HTTPQueryEvaluationException(String msg) {
		super(msg);
	}

	/**
	 * @param msg
	 * @param cause
	 */
	public HTTPQueryEvaluationException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * @param cause
	 */
	public HTTPQueryEvaluationException(Throwable cause) {
		super(cause);
	}

	public boolean isCausedByIOException() {
		return getCause() instanceof IOException;
	}

	public boolean isCausedByRepositoryException() {
		return getCause() instanceof RepositoryException;
	}

	public boolean isCausedByMalformedQueryException() {
		return getCause() instanceof MalformedQueryException;
	}

	public IOException getCauseAsIOException() {
		return (IOException) getCause();
	}

	public RepositoryException getCauseAsRepositoryException() {
		return (RepositoryException) getCause();
	}

	public MalformedQueryException getCauseAsMalformedQueryException() {
		return (MalformedQueryException) getCause();
	}
}
