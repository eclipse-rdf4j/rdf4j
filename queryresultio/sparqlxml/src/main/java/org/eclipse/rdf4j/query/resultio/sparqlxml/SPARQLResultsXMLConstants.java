/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
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

	public static final String NAMESPACE = "http://www.w3.org/2005/sparql-results#";

	public static final String ROOT_TAG = "sparql";

	public static final String HEAD_TAG = "head";

	public static final String LINK_TAG = "link";

	public static final String VAR_TAG = "variable";

	public static final String VAR_NAME_ATT = "name";

	public static final String HREF_ATT = "href";

	public static final String BOOLEAN_TAG = "boolean";

	public static final String BOOLEAN_TRUE = "true";

	public static final String BOOLEAN_FALSE = "false";

	public static final String RESULT_SET_TAG = "results";

	public static final String RESULT_TAG = "result";

	public static final String BINDING_TAG = "binding";

	public static final String BINDING_NAME_ATT = "name";

	public static final String URI_TAG = "uri";

	public static final String BNODE_TAG = "bnode";

	public static final String LITERAL_TAG = "literal";

	public static final String LITERAL_LANG_ATT = "xml:lang";

	public static final String LITERAL_DATATYPE_ATT = "datatype";

	public static final String UNBOUND_TAG = "unbound";

	public static final String QNAME = "q:qname";
}
