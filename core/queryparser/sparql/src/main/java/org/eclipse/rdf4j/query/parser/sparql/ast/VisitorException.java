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
package org.eclipse.rdf4j.query.parser.sparql.ast;

public class VisitorException extends Exception {

	private static final long serialVersionUID = 6682567015759392643L;

	public VisitorException() {
		super();
	}

	public VisitorException(String msg) {
		super(msg);
	}

	public VisitorException(String msg, Throwable t) {
		super(msg, t);
	}

	public VisitorException(Throwable t) {
		super(t);
	}
}
