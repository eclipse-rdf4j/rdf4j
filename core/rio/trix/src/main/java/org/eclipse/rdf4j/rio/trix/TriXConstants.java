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
package org.eclipse.rdf4j.rio.trix;

/**
 * Interface defining a number of constants for the TriX document format.
 */
public interface TriXConstants {

	/** The TriX namespace. */
	String NAMESPACE = "http://www.w3.org/2004/03/trix/trix-1/";

	/** The root tag. */
	String ROOT_TAG = "TriX";

	/** The tag that starts a new context/graph. */
	String CONTEXT_TAG = "graph";

	/** The tag that starts a new triple. */
	String TRIPLE_TAG = "triple";

	/** The tag for URI values. */
	String URI_TAG = "uri";

	/** The tag for BNode values. */
	String BNODE_TAG = "id";

	/** The tag for plain literal values. */
	String PLAIN_LITERAL_TAG = "plainLiteral";

	/** The tag for typed literal values. */
	String TYPED_LITERAL_TAG = "typedLiteral";

	/** The attribute for language tags of plain literal. */
	String LANGUAGE_ATT = "xml:lang";

	/** The attribute for datatypes of typed literal. */
	String DATATYPE_ATT = "datatype";
}
