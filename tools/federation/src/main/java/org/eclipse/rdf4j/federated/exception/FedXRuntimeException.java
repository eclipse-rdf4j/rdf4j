/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.exception;

/**
 * Base class for any FedX Exception.
 *
 * @author Andreas Schwarte
 *
 */
public class FedXRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -3973697449786957158L;

	public FedXRuntimeException() {
		super();
	}

	public FedXRuntimeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public FedXRuntimeException(String arg0) {
		super(arg0);
	}

	public FedXRuntimeException(Throwable arg0) {
		super(arg0);
	}
}
