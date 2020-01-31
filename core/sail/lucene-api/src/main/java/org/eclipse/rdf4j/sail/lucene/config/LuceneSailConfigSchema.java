/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;

/**
 * Defines constants for the LuceneSail schema which is used by {@link LuceneSailFactory}s to initialize
 * {@link LuceneSail}s.
 */
public class LuceneSailConfigSchema {

	/**
	 * The LuceneSail schema namespace ( <tt>http://www.openrdf.org/config/sail/lucene#</tt>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/sail/lucene#";

	public static final IRI INDEX_DIR;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		INDEX_DIR = factory.createIRI(NAMESPACE, "indexDir");
	}
}
