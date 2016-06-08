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
 * http://jena.hpl.hp.com/ARQ/list#.
 */
public final class LIST {

	private LIST() {
	}

	/**
	 * http://jena.hpl.hp.com/ARQ/list
	 */
	public static final String NAMESPACE = "http://jena.hpl.hp.com/ARQ/list#";

	public static final String PREFIX = "list";

	public static final IRI MEMBER;

	public static final IRI INDEX;

	public static final IRI LENGTH;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		MEMBER = factory.createIRI(NAMESPACE, "member");
		INDEX = factory.createIRI(NAMESPACE, "index");
		LENGTH = factory.createIRI(NAMESPACE, "length");
	}
}
