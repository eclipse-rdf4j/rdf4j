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
 * http://jena.hpl.hp.com/ARQ/property#.
 */
public final class APF {

	private APF() {
	}

	/**
	 * http://jena.hpl.hp.com/ARQ/property
	 */
	public static final String NAMESPACE = "http://jena.hpl.hp.com/ARQ/property#";

	public static final String PREFIX = "apf";

	public static final URI STR_SPLIT;

	public static final URI CONCAT;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		STR_SPLIT = factory.createURI(NAMESPACE, "strSplit");
		CONCAT = factory.createURI(NAMESPACE, "concat");
	}
}
