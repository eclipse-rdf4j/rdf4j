/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the Sesame schema namespace.
 */
public class SESAME {

	/**
	 * The Sesame Schema namespace (
	 * <tt>http://www.openrdf.org/schema/sesame#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/schema/sesame#";

	/**
	 * Recommended prefix for the Sesame Schema namespace: "sesame"
	 */
	public static final String PREFIX = "sesame";

	/**
	 * An immutable {@link Namespace} constant that represents the Sesame Schema
	 * namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/** <tt>http://www.openrdf.org/schema/sesame#directSubClassOf</tt> */
	public final static IRI DIRECTSUBCLASSOF;

	/** <tt>http://www.openrdf.org/schema/sesame#directSubPropertyOf</tt> */
	public final static IRI DIRECTSUBPROPERTYOF;

	/** <tt>http://www.openrdf.org/schema/sesame#directType</tt> */
	public final static IRI DIRECTTYPE;

	/**
	 * The SPARQL null context identifier (
	 * <tt>http://www.openrdf.org/schema/sesame#nil</tt>)
	 */
	public final static IRI NIL;

	/**
	 * <tt>http://www.openrdf.org/schema/sesame#wildcard</tt>
	 */
	public final static IRI WILDCARD;
	
	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		DIRECTSUBCLASSOF = factory.createIRI(SESAME.NAMESPACE, "directSubClassOf");
		DIRECTSUBPROPERTYOF = factory.createIRI(SESAME.NAMESPACE, "directSubPropertyOf");
		DIRECTTYPE = factory.createIRI(SESAME.NAMESPACE, "directType");

		NIL = factory.createIRI(NAMESPACE, "nil");
		
		WILDCARD = factory.createIRI(NAMESPACE, "wildcard");
	}
}
