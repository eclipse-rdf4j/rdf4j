/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.binary;

/**
 * A type-safe enumeration for query error types.
 *
 * @author Arjohn Kampman
 */
public enum QueryErrorType {

	/**
	 * Constant used for identifying a malformed query error.
	 */
	MALFORMED_QUERY_ERROR,

	/**
	 * Constant used for identifying a query evaluation error.
	 */
	QUERY_EVALUATION_ERROR
}
