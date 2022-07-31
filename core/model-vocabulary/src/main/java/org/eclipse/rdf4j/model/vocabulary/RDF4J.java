/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Defines constants for the RDF4J namespace. These constants include things like implementation-specific named graph
 * identifiers or properties.
 *
 * @author Jeen Broekstra
 */
public class RDF4J {

	/**
	 * The RDF4J namespace ( <var>http://rdf4j.org/schema/rdf4j#</var>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/schema/rdf4j#";

	/**
	 * Recommended prefix for the RDF4J namespace: "rdf4j"
	 */
	public static final String PREFIX = "rdf4j";

	/**
	 * An immutable {@link Namespace} constant that represents the RDF4J namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/**
	 * Context identifier for persisting SHACL shape data in the {@link org.eclipse.rdf4j.sail.shacl.ShaclSail} .
	 * <var>http://rdf4j.org/schema/rdf4j#SHACLShapeGraph</var>
	 */
	public final static IRI SHACL_SHAPE_GRAPH = create("SHACLShapeGraph");
	public final static IRI TRUNCATED = create("truncated");

	/**
	 * The SPARQL default context identifier ( <var>http://rdf4j.org/schema/rdf4j#nil</var>)
	 *
	 * @since 3.3.2
	 */
	public final static IRI NIL = create("nil");

	private static IRI create(String localName) {
		return Vocabularies.createIRI(NAMESPACE, localName);
	}
}
