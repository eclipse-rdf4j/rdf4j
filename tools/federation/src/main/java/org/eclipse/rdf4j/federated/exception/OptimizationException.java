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

public class OptimizationException extends FedXRuntimeException {

	private static final long serialVersionUID = 1L;

	public OptimizationException() {
		super();
	}

	public OptimizationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public OptimizationException(String arg0) {
		super(arg0);
	}

	public OptimizationException(Throwable arg0) {
		super(arg0);
	}
}
