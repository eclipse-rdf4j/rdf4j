/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.protocol.transaction;

/**
 * Interface defining tags and attribute names for the XML serialization of transactions.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
interface TransactionXMLConstants {

	public static final String TRANSACTION_TAG = "transaction";

	public static final String ADD_STATEMENT_TAG = "add";

	public static final String REMOVE_STATEMENTS_TAG = "remove";

	public static final String REMOVE_NAMED_CONTEXT_STATEMENTS_TAG = "removeFromNamedContext";

	public static final String CLEAR_TAG = "clear";

	public static final String NULL_TAG = "null";

	public static final String TRIPLE_TAG = "triple";

	public static final String URI_TAG = "uri";

	public static final String BNODE_TAG = "bnode";

	public static final String LITERAL_TAG = "literal";

	public static final String ENCODING_ATT = "encoding";

	public static final String LANG_ATT = "xml:lang";

	public static final String DATATYPE_ATT = "datatype";

	public static final String SET_NAMESPACE_TAG = "setNamespace";

	public static final String REMOVE_NAMESPACE_TAG = "removeNamespace";

	public static final String PREFIX_ATT = "prefix";

	public static final String NAME_ATT = "name";

	public static final String CLEAR_NAMESPACES_TAG = "clearNamespaces";

	public static final String CONTEXTS_TAG = "contexts";

	/**
	 */
	public static final String SPARQL_UPDATE_TAG = "sparql";

	/**
	 */
	public static final String UPDATE_STRING_TAG = "updateString";

	/**
	 */
	public static final String BASE_URI_ATT = "baseURI";

	/**
	 */
	public static final String INCLUDE_INFERRED_ATT = "includeInferred";

	/**
	 */
	public static final String DATASET_TAG = "dataset";

	/**
	 */
	public static final String GRAPH_TAG = "graph";

	/**
	 */
	public static final String DEFAULT_GRAPHS_TAG = "defaultGraphs";

	/**
	 */
	public static final String NAMED_GRAPHS_TAG = "namedGraphs";

	/**
	 */
	public static final String DEFAULT_REMOVE_GRAPHS_TAG = "defaultRemoveGraphs";

	/**
	 */
	public static final String DEFAULT_INSERT_GRAPH = "defaultInsertGraph";

	public static final String BINDINGS = "bindings";

	public static final String BINDING_URI = "binding_uri";

	public static final String BINDING_BNODE = "binding_bnode";

	public static final String BINDING_LITERAL = "binding_literal";

	public static final String LANGUAGE_ATT = "language";

	public static final String DATA_TYPE_ATT = "dataType";
}
