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
package org.eclipse.rdf4j.repository;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception thrown by classes from the Repository API to indicate an error. Most of the time, this exception will
 * wrap another exception that indicates the actual source of the error.
 */
public class RepositoryException extends RDF4JException {

	private static final long serialVersionUID = -5345676977796873420L;

	public RepositoryException() {
		super();
	}

	public RepositoryException(String msg) {
		super(msg);
	}

	public RepositoryException(Throwable t) {
		super(t);
	}

	public RepositoryException(String msg, Throwable t) {
		super(msg, t);
	}
}
