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
package org.eclipse.rdf4j.query.resultio.sparqlxml;

/**
 * Interface defining tags and attribute names that are used in SPARQL Results Documents. See
 * <a href="http://www.w3.org/TR/rdf-sparql-XMLres/">SPARQL Query Results XML Format</a> for the definition of this
 * format.
 *
 * @author Arjohn Kampman
 */
interface SPARQLResultsXMLConstants {

	String NAMESPACE = "http://www.w3.org/2005/sparql-results#";

	String ROOT_TAG = "sparql";

	String HEAD_TAG = "head";

	String LINK_TAG = "link";

	String VAR_TAG = "variable";

	String VAR_NAME_ATT = "name";

	String HREF_ATT = "href";

	String BOOLEAN_TAG = "boolean";

	String BOOLEAN_TRUE = "true";

	String BOOLEAN_FALSE = "false";

	String RESULT_SET_TAG = "results";

	String RESULT_TAG = "result";

	String BINDING_TAG = "binding";

	String BINDING_NAME_ATT = "name";

	String URI_TAG = "uri";

	String BNODE_TAG = "bnode";

	String LITERAL_TAG = "literal";

	String LITERAL_LANG_ATT = "xml:lang";

	String LITERAL_DATATYPE_ATT = "datatype";

	String UNBOUND_TAG = "unbound";

	String QNAME = "q:qname";

	/* tag constants for serialization of RDF-star values in results */

	String TRIPLE_TAG = "triple";

	/* Stardog variant */
	String STATEMENT_TAG = "statement";

	String SUBJECT_TAG = "subject";
	/* Stardog variant */
	String S_TAG = "s";

	String PREDICATE_TAG = "predicate";
	/* Stardog variant */
	String P_TAG = "p";

	String OBJECT_TAG = "object";
	/* Stardog variant */
	String O_TAG = "o";
}
