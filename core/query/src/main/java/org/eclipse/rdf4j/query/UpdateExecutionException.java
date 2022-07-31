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
 * An exception indicating that the execution of an update failed.
 *
 * @author Jeen
 */
public class UpdateExecutionException extends RDF4JException {

	/**
	 *
	 */
	private static final long serialVersionUID = 7969399526232927434L;

	public UpdateExecutionException() {
		super();
	}

	/**
	 * Creates a new UpdateExecutionException.
	 *
	 * @param msg An error message.
	 */
	public UpdateExecutionException(String msg) {
		super(msg);
	}

	/**
	 * Creates a new {@link UpdateExecutionException} wrapping another exception.
	 *
	 * @param cause the cause of the exception
	 */
	public UpdateExecutionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new {@link UpdateExecutionException} wrapping another exception.
	 *
	 * @param msg   and error message.
	 * @param cause the cause of the exception
	 */
	public UpdateExecutionException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
