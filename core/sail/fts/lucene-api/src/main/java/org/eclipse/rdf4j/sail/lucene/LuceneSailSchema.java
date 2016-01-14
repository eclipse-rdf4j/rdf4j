/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * LuceneSailSchema defines predicates that can be used for expressing a Lucene
 * query in a RDF query.
 */
public class LuceneSailSchema {

	public static final String NAMESPACE = "http://www.openrdf.org/contrib/lucenesail#";

	public static final IRI LUCENE_QUERY;

	public static final IRI SCORE;

	public static final IRI QUERY;

	public static final IRI PROPERTY;

	public static final IRI SNIPPET;

	public static final IRI MATCHES;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance(); // compatible with beta4:
																		// creating a new factory
		LUCENE_QUERY = factory.createIRI(NAMESPACE + "LuceneQuery");
		SCORE = factory.createIRI(NAMESPACE + "score");
		QUERY = factory.createIRI(NAMESPACE + "query");
		PROPERTY = factory.createIRI(NAMESPACE + "property");
		SNIPPET = factory.createIRI(NAMESPACE + "snippet");
		MATCHES = factory.createIRI(NAMESPACE + "matches");
	}
}
