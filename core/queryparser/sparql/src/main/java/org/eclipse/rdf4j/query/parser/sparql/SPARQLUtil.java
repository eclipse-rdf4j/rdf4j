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
package org.eclipse.rdf4j.query.parser.sparql;

/**
 * SPARQL-related utility methods.
 *
 * @author Arjohn Kampman
 *
 * @deprecated since 3.6.0 Use {@link SPARQLQueries} instead.
 */
@Deprecated
public class SPARQLUtil extends SPARQLQueries {

	/**
	 * @deprecated since 3.6.0. Use {@link SPARQLQueries#escape(String)} instead.
	 */
	@Deprecated
	public static String encodeString(String string) {
		return escape(string);
	}

	/**
	 * @deprecated since 3.6.0. Use {@link SPARQLQueries#unescape(String)} instead.
	 */
	@Deprecated
	public static String decodeString(String string) {
		return unescape(string);
	}
}
