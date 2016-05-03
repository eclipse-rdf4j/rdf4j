/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.ValueFactoryImpl;

/**
 * http://jena.hpl.hp.com/ARQ/list#.
 */
public final class LIST {

	private LIST() {}

	/**
	 * http://jena.hpl.hp.com/ARQ/list
	 */
	public static final String NAMESPACE = "http://jena.hpl.hp.com/ARQ/list#";

	public static final String PREFIX = "list";

	public static final URI MEMBER;
	public static final URI INDEX;
	public static final URI LENGTH;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		MEMBER = factory.createURI(NAMESPACE, "member");
		INDEX = factory.createURI(NAMESPACE, "index");
		LENGTH = factory.createURI(NAMESPACE, "length");
	}
}
