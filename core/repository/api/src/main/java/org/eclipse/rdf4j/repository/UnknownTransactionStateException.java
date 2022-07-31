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

/**
 * A specific subtype of {@link RepositoryException} that indicates the connection's transaction state can not be
 * determined.
 *
 * @author Jeen Broekstra
 */
public class UnknownTransactionStateException extends RepositoryException {

	private static final long serialVersionUID = -5938676154783704438L;

	public UnknownTransactionStateException() {
		super();
	}

	public UnknownTransactionStateException(String msg) {
		super(msg);
	}

	public UnknownTransactionStateException(Throwable t) {
		super(t);
	}

	public UnknownTransactionStateException(String msg, Throwable t) {
		super(msg, t);
	}
}
