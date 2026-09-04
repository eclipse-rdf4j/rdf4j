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
package org.eclipse.rdf4j.query.resultio.sparqljson;

import org.eclipse.rdf4j.model.TripleTerm;

/**
 * Constants for the SPARQL triple terms in the JSON format. The format handles
 * {@link org.eclipse.rdf4j.query.TupleQueryResult} only. For Boolean results, the SPARQL JSON format is used.
 *
 * @author Pavel Mihaylov
 */
final class SPARQLTripleTermResultsJSONConstants {

	/**
	 * Type string for serialized {@link TripleTerm} value.
	 */
	static final String TRIPLE_TERM = "triple";

	/**
	 * Key name of the JSON object for the triple's subject.
	 */
	static final String SUBJECT = "subject";

	/**
	 * Key name of the JSON object for the triple's predicate.
	 */
	static final String PREDICATE = "predicate";

	/**
	 * Key name of the JSON object for the triple's object.
	 */
	static final String OBJECT = "object";

	/**
	 * Type string for serialized {@link TripleTerm} value - Stardog dialect
	 */
	final static String TRIPLE_STARDOG = "statement";
}
