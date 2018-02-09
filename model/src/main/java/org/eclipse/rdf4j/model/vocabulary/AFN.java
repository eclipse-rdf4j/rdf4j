/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * http://jena.hpl.hp.com/ARQ/function#.
 */
public final class AFN {

	private AFN() {
	}

	/**
	 * http://jena.hpl.hp.com/ARQ/function
	 */
	public static final String NAMESPACE = "http://jena.hpl.hp.com/ARQ/function#";

	public static final String PREFIX = "afn";

	/**
	 * http://jena.hpl.hp.com/ARQ/function#localname The LocalName QueryModelNode as a SPARQL function.
	 */
	public static final IRI LOCALNAME;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		LOCALNAME = factory.createIRI(NAMESPACE, "localname");
	}
}
