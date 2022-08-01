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
package org.eclipse.rdf4j.workbench.exceptions;

import javax.servlet.ServletException;

public class BadRequestException extends ServletException {

	private static final long serialVersionUID = -6227037493079059474L;

	public BadRequestException(String message) {
		super(message);
	}

	public BadRequestException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
