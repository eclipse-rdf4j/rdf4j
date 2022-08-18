/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

public class ShaclUnsupportedException extends UnsupportedOperationException {
	public ShaclUnsupportedException() {
	}

	public ShaclUnsupportedException(String message) {
		super(message);
	}

	public ShaclUnsupportedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ShaclUnsupportedException(Throwable cause) {
		super(cause);
	}

//	@Override
//	public synchronized Throwable fillInStackTrace() {
//		return this;
//	}
}
