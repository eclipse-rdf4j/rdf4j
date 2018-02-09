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

	public static final IRI STR_SPLIT;

	public static final IRI CONCAT;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		STR_SPLIT = factory.createIRI(NAMESPACE, "strSplit");
		CONCAT = factory.createIRI(NAMESPACE, "concat");
	}
}
