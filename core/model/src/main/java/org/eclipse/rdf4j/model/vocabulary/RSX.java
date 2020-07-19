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
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the Eclipse RDF4J SHACL Extensions.
 *
 */
public class RSX {

	/** The namespace (<tt>http://rdf4j.org/shacl-extensions#</tt>). */
	public static final String NAMESPACE = "http://rdf4j.org/shacl-extensions#";

	/**
	 * Recommended prefix
	 */
	public static final String PREFIX = "rsx";

	/**
	 * An immutable {@link Namespace} constant that represents the namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/*
	 * Primitive datatypes
	 */

	/** <tt>http://rdf4j.org/shacl-extensions#targetShape</tt> */
	public final static IRI targetShape = create("targetShape");

	private static IRI create(String localName) {
		return SimpleValueFactory.getInstance().createIRI(RSX.NAMESPACE, localName);
	}
}
