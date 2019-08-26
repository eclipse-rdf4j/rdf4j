/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the RDF4J namespace. These constants include things like implementation-specific named graph
 * identifiers or properties.
 * 
 * @author Jeen Broekstra
 */
public class RDF4J {

	/**
	 * The RDF4J namespace ( <tt>http://rdf4j.org/schema/rdf4j#</tt>).
	 */
	public static final String NAMESPACE = "http://rdf4j.org/schema/rdf4j#";

	/**
	 * Recommended prefix for the RDF4J namespace: "rdf4j"
	 */
	public static final String PREFIX = "rdf4j";

	/**
	 * An immutable {@link Namespace} constant that represents the RDF4J namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/**
	 * Context identifier for persisting SHACL shape data in the {@link ShaclSail}.
	 * <tt>http://rdf4j.org/schema/rdf4j#SHACLShapeGraph</tt>
	 */
	public final static IRI SHACL_SHAPE_GRAPH = SimpleValueFactory.getInstance()
			.createIRI(NAMESPACE, "SHACLShapeGraph");
}
