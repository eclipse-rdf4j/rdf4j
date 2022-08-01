/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.tx.exception;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class WriteDeniedException extends RDF4JTransactionException {
	public WriteDeniedException() {
	}

	public WriteDeniedException(String message) {
		super(message);
	}

	public WriteDeniedException(String message, Throwable cause) {
		super(message, cause);
	}

	public WriteDeniedException(Throwable cause) {
		super(cause);
	}
}
