/*
 * ******************************************************************************
 *  * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the Eclipse Distribution License v1.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.eclipse.org/org/documents/edl-v10.php.
 *  ******************************************************************************
 */

package org.eclipse.rdf4j.spring.dao.exception;

public class UnsupportedDataTypeException extends UnexpectedResultException {
	public UnsupportedDataTypeException() {
	}

	public UnsupportedDataTypeException(String message) {
		super(message);
	}

	public UnsupportedDataTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedDataTypeException(Throwable cause) {
		super(cause);
	}
}