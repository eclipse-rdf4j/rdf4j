/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx.exception;

import org.eclipse.rdf4j.spring.dao.exception.RDF4JSpringException;

/**
 * @since 4.0.0
 * @author ameingast@gmail.com
 * @author Florian Kleedorfer
 */
public class RDF4JTransactionException extends RDF4JSpringException {
	public RDF4JTransactionException() {
	}

	public RDF4JTransactionException(String message) {
		super(message);
	}

	public RDF4JTransactionException(String message, Throwable cause) {
		super(message, cause);
	}

	public RDF4JTransactionException(Throwable cause) {
		super(cause);
	}

}
