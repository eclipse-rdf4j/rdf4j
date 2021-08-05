/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.tx.exception;

public class TransactionInactiveException extends RDF4JTransactionException {
	public TransactionInactiveException() {
	}

	public TransactionInactiveException(String message) {
		super(message);
	}

	public TransactionInactiveException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransactionInactiveException(Throwable cause) {
		super(cause);
	}
}