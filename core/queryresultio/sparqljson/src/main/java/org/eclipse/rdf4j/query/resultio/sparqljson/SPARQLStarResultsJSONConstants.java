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

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Constants for the SPARQL-star JSON format. The format handles {@link org.eclipse.rdf4j.query.TupleQueryResult} only.
 * For Boolean results, the SPARQL JSON format is used.
 * <p>
 * The format introduces a new type, triple, whose value is an object consisting of three elements:
 *
 * <ul>
 * <li>s - the triple's subject</li>
 * <li>p - the triple's predicate</li>
 * <li>o - the triple's object</li>
 * </ul>
 * <p>
 * Each of the three elements s, p and o is another object identical in structure to the value for each binding.
 * <p>
 * For example:
 * <p>
 *
 * <pre>
 * 	"b" : {
 * 		"type" : "triple",
 * 		"value" : {
 * 			"s" : {
 * 				"type" : "uri",
 * 				"value" : "urn:a"
 *            },
 * 			"p" : {
 * 				"type" : "uri",
 * 				"value" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
 *        },
 * 			"o" : {
 * 				"type" : "uri",
 * 				"value" : "urn:b"
 *          }
 *        }
 *  }
 * </pre>
 *
 * @author Pavel Mihaylov
 * @see <a href="https://w3c.github.io/rdf-star/cg-spec/">RDF-star and SPARQL-star Draft Community Group Report</a>
 */
final class SPARQLStarResultsJSONConstants {
	static TupleQueryResultFormat QUERY_RESULT_FORMAT = TupleQueryResultFormat.JSON_STAR;

	/**
	 * Type string for serialized {@link org.eclipse.rdf4j.model.Triple} value.
	 */
	static final String TRIPLE = "triple";

	/**
	 * Key name of the JSON object for the triple's subject.
	 */
	static final String SUBJECT = "s";

	/**
	 * Key name of the JSON object for the triple's predicate.
	 */
	static final String PREDICATE = "p";

	/**
	 * Key name of the JSON object for the triple's object.
	 */
	static final String OBJECT = "o";

	/**
	 * Type string for serialized {@link org.eclipse.rdf4j.model.Triple} value - Stardog dialect
	 */
	final static String TRIPLE_STARDOG = "statement";

	/**
	 * Key name of the JSON object for the triple's subject - Apache Jena dialect
	 */
	final static String SUBJECT_JENA = "subject";

	/**
	 * Key name of the JSON object for the triple's predicate - Apache Jena dialect
	 */
	final static String PREDICATE_JENA = "predicate";

	/**
	 * Key name of the JSON object for the triple's object - Apache Jena dialect
	 */
	final static String OBJECT_JENA = "object";

}
