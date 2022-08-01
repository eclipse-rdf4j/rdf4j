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
package org.eclipse.rdf4j.http.protocol.transaction;

/**
 * Interface defining tags and attribute names for the XML serialization of transactions.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
interface TransactionXMLConstants {

	String TRANSACTION_TAG = "transaction";

	String ADD_STATEMENT_TAG = "add";

	String REMOVE_STATEMENTS_TAG = "remove";

	String REMOVE_NAMED_CONTEXT_STATEMENTS_TAG = "removeFromNamedContext";

	String CLEAR_TAG = "clear";

	String NULL_TAG = "null";

	String TRIPLE_TAG = "triple";

	String URI_TAG = "uri";

	String BNODE_TAG = "bnode";

	String LITERAL_TAG = "literal";

	String ENCODING_ATT = "encoding";

	String LANG_ATT = "xml:lang";

	String DATATYPE_ATT = "datatype";

	String SET_NAMESPACE_TAG = "setNamespace";

	String REMOVE_NAMESPACE_TAG = "removeNamespace";

	String PREFIX_ATT = "prefix";

	String NAME_ATT = "name";

	String CLEAR_NAMESPACES_TAG = "clearNamespaces";

	String CONTEXTS_TAG = "contexts";

	/**
	 */
	String SPARQL_UPDATE_TAG = "sparql";

	/**
	 */
	String UPDATE_STRING_TAG = "updateString";

	/**
	 */
	String BASE_URI_ATT = "baseURI";

	/**
	 */
	String INCLUDE_INFERRED_ATT = "includeInferred";

	/**
	 */
	String DATASET_TAG = "dataset";

	/**
	 */
	String GRAPH_TAG = "graph";

	/**
	 */
	String DEFAULT_GRAPHS_TAG = "defaultGraphs";

	/**
	 */
	String NAMED_GRAPHS_TAG = "namedGraphs";

	/**
	 */
	String DEFAULT_REMOVE_GRAPHS_TAG = "defaultRemoveGraphs";

	/**
	 */
	String DEFAULT_INSERT_GRAPH = "defaultInsertGraph";

	String BINDINGS = "bindings";

	String BINDING_URI = "binding_uri";

	String BINDING_BNODE = "binding_bnode";

	String BINDING_LITERAL = "binding_literal";

	String LANGUAGE_ATT = "language";

	String DATA_TYPE_ATT = "dataType";
}
