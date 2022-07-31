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
package org.eclipse.rdf4j.spin;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception thrown by the SPIN parser when it encounters malformed SPIN rules, constraints or constructors.
 */
public class MalformedSpinException extends RDF4JException {

	private static final long serialVersionUID = -5345676977796873420L;

	public MalformedSpinException() {
		super();
	}

	public MalformedSpinException(String msg) {
		super(msg);
	}

	public MalformedSpinException(Throwable t) {
		super(t);
	}

	public MalformedSpinException(String msg, Throwable t) {
		super(msg, t);
	}
}
