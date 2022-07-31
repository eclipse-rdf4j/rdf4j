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
package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.common.exception.RDF4JException;

/**
 * An exception thrown by {@link Literals} when specific conditions are not met.
 *
 * @author Peter Ansell
 */
public class LiteralUtilException extends RDF4JException {

	private static final long serialVersionUID = 9075761346909805423L;

	public LiteralUtilException() {
		super();
	}

	public LiteralUtilException(String message) {
		super(message);
	}

	public LiteralUtilException(Throwable t) {
		super(t);
	}

	public LiteralUtilException(String message, Throwable t) {
		super(message, t);
	}
}
