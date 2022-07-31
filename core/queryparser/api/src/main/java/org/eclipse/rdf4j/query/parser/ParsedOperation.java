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
package org.eclipse.rdf4j.query.parser;

/**
 * Abstract superclass of all operations that can be formulated in a query language and parsed by the query parser.
 *
 * @author Jeen Broekstra
 */
public abstract class ParsedOperation {

	/**
	 * The source string (e.g. SPARQL query) that produced this operation.
	 */
	private final String sourceString;

	protected ParsedOperation() {
		this(null);
	}

	protected ParsedOperation(String sourceString) {
		super();
		this.sourceString = sourceString;
	}

	/**
	 * @return Returns the sourceString.
	 */
	public String getSourceString() {
		return sourceString;
	}

}
