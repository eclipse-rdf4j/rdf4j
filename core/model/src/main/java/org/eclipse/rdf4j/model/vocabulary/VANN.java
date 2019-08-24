/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
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
 * Constants for <a href="http://purl.org/vocab/vann/">a vocabulary for annotating vocabulary descriptions</a> (VANN).
 *
 * @see <a href="http://purl.org/vocab/vann/">A vocabulary for annotating vocabulary descriptions (VANN)</a>
 */
public class VANN {

	/** The VANN namespace: http://purl.org/vocab/vann/ */
	public static final String NAMESPACE = "http://purl.org/vocab/vann/";

	/** Recommended prefix for the VANN namespace: "vann" */
	public static final String PREFIX = "vann";

	/**
	 * An immutable {@link Namespace} constant that represents the VANN namespace.
	 */
	public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

	/**
	 * A reference to a resource that describes changes between this version of a vocabulary and the previous.
	 */
	public final static IRI CHANGES;

	/** A reference to a resource that provides an example of how this resource can be used. */
	public final static IRI EXAMPLE;

	/** The preferred namespace prefix to ue when using terms from this vocabulary in an XML document. */
	public final static IRI PREFERRED_NAMESPACE_PREFIX;

	/** The preferred namespace URI to use when using terms from this vocabulary in an XML document. */
	public final static IRI PREFERRED_NAMESPACE_URI;

	/** A group of related terms in a vocabulary. */
	public final static IRI TERM_GROUP;

	/** A reference to a resource that provides information on how this resource is to be used. */
	public final static IRI USAGE_NOTE;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		CHANGES = factory.createIRI(VANN.NAMESPACE, "changes");
		EXAMPLE = factory.createIRI(VANN.NAMESPACE, "example");
		PREFERRED_NAMESPACE_PREFIX = factory.createIRI(VANN.NAMESPACE, "preferredNamespacePrefix");
		PREFERRED_NAMESPACE_URI = factory.createIRI(VANN.NAMESPACE, "preferredNamespaceUri");
		TERM_GROUP = factory.createIRI(VANN.NAMESPACE, "termGroup");
		USAGE_NOTE = factory.createIRI(VANN.NAMESPACE, "usageNote");
	}
}
