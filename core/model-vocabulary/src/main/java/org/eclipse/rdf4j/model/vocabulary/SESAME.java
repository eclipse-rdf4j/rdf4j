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

/**
 * Defines constants for the Sesame schema namespace.
 */
public class SESAME {

	/**
	 * The Sesame Schema namespace ( <tt>http://www.openrdf.org/schema/sesame#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/schema/sesame#";

	/**
	 * Recommended prefix for the Sesame Schema namespace: "sesame"
	 */
	public static final String PREFIX = "sesame";

	/**
	 * An immutable {@link Namespace} constant that represents the Sesame Schema namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/** <tt>http://www.openrdf.org/schema/sesame#directSubClassOf</tt> */
	public final static IRI DIRECTSUBCLASSOF;

	/** <tt>http://www.openrdf.org/schema/sesame#directSubPropertyOf</tt> */
	public final static IRI DIRECTSUBPROPERTYOF;

	/** <tt>http://www.openrdf.org/schema/sesame#directType</tt> */
	public final static IRI DIRECTTYPE;

	/**
	 * The SPARQL null context identifier ( <tt>http://www.openrdf.org/schema/sesame#nil</tt>)
	 *
	 * @deprecated since 3.3.2 - use {@link RDF4J#NIL} instead
	 */
	@Deprecated
	public final static IRI NIL;

	/**
	 * <tt>http://www.openrdf.org/schema/sesame#wildcard</tt>
	 */
	public final static IRI WILDCARD;

	static {
		DIRECTSUBCLASSOF = Vocabularies.createIRI(SESAME.NAMESPACE, "directSubClassOf");
		DIRECTSUBPROPERTYOF = Vocabularies.createIRI(SESAME.NAMESPACE, "directSubPropertyOf");
		DIRECTTYPE = Vocabularies.createIRI(SESAME.NAMESPACE, "directType");

		NIL = Vocabularies.createIRI(NAMESPACE, "nil");

		WILDCARD = Vocabularies.createIRI(NAMESPACE, "wildcard");
	}
}
