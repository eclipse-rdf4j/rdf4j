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

package org.eclipse.rdf4j.spring.dao.exception;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class RDF4JSpringException extends RDF4JException {
	public RDF4JSpringException() {
	}

	public RDF4JSpringException(String message) {
		super(message);
	}

	public RDF4JSpringException(String message, Throwable cause) {
		super(message, cause);
	}

	public RDF4JSpringException(Throwable cause) {
		super(cause);
	}

}
