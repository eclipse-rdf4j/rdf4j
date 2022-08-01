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
package org.eclipse.rdf4j.http.protocol;

import org.eclipse.rdf4j.repository.RepositoryException;

public class UnauthorizedException extends RepositoryException {

	private static final long serialVersionUID = 4322677542795160482L;

	public UnauthorizedException() {
		super();
	}

	public UnauthorizedException(String msg) {
		super(msg);
	}

	public UnauthorizedException(Throwable t) {
		super(t);
	}

	public UnauthorizedException(String msg, Throwable t) {
		super(msg, t);
	}
}
