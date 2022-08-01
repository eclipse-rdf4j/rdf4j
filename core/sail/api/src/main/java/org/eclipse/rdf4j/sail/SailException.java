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
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception thrown by some methods in Sail to indicate that a requested operation could not be executed.
 */
public class SailException extends RDF4JException {

	private static final long serialVersionUID = 2432600780159917763L;

	public SailException() {
		super();
	}

	public SailException(String msg) {
		super(msg);
	}

	public SailException(Throwable t) {
		super(t);
	}

	public SailException(String msg, Throwable t) {
		super(msg, t);
	}
}
