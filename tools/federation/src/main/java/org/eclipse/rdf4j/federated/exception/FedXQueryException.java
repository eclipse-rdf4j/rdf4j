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
 * Exception to be thrown if during query evaluation a data source is not reachable, i.e. SocketException. All endpoints
 * are repaired and should work for the next query.
 *
 * @author Andreas Schwarte
 *
 */
public class FedXQueryException extends RuntimeException {

	public FedXQueryException() {
		super();
	}

	public FedXQueryException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public FedXQueryException(String arg0) {
		super(arg0);
	}

	public FedXQueryException(Throwable arg0) {
		super(arg0);
	}

	private static final long serialVersionUID = 1L;

}
