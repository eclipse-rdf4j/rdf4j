/*******************************************************************************
 * Copyright (c) 2016, 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.examples.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for the 'http://example.org/' namespace.
 * It's a good idea to always create a vocabulary class such as this one when you program with RDF4J. It makes
 * it far easier to reuse certain resources and properties in various places in your code.
 */
public class EX {

	/**
	 * The full namespace: "http://example.org/".
	 */
	public static final String NAMESPACE = "http://example.org/";

	/**
	 * The prefix usually used for this vocabulary: 'ex'.
	 */
	public static final String PREFIX = "ex";

	/**
	 * The <code>ex:creatorOf</code> property.
	 */
	public static final IRI CREATOR_OF = getIRI("creatorOf");

	/**
	 * The <code>ex:Artist</code> class.
	 */
	public static final IRI ARTIST = getIRI("Artist");

	/**
	 * Creates a new {@link IRI} with this vocabulary's namespace for the given local name.
	 *
	 * @param localName a local name of an IRI, e.g. 'creatorOf', 'name', 'Artist', etc.
	 * @return an IRI using the http://example.org/ namespace and the given local name.
	 */
	private static IRI getIRI(String localName) {
		return SimpleValueFactory.getInstance().createIRI(NAMESPACE, localName);
	}

}
