/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

/**
 * Constants for the Eclipse RDF4J SHACL Extensions.
 */
public class RSX {

	/** The namespace (<var>http://rdf4j.org/shacl-extensions#</var>). */
	public static final String NAMESPACE = "http://rdf4j.org/shacl-extensions#";

	/**
	 * Recommended prefix
	 */
	public static final String PREFIX = "rsx";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/*
	 * Primitive datatypes
	 */

	/** <var>http://rdf4j.org/shacl-extensions#targetShape</var> */
	public final static IRI targetShape = create("targetShape");
	public final static IRI dataGraph = create("dataGraph");
	public final static IRI shapesGraph = create("shapesGraph");

	private static IRI create(String localName) {
		return Vocabularies.createIRI(RSX.NAMESPACE, localName);
	}
}
