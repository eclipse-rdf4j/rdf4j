/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * LuceneSailSchema defines predicates that can be used for expressing a Lucene query in a RDF query.
 */
public class LuceneSailSchema {

	public static final String NAMESPACE = "http://www.openrdf.org/contrib/lucenesail#";

	public static final IRI LUCENE_QUERY;

	public static final IRI SCORE;

	public static final IRI QUERY;

	public static final IRI PROPERTY;

	public static final IRI SNIPPET;

	public static final IRI MATCHES;

	public static final IRI INDEXID;

	/**
	 * "Magic property" (TupleFunction) IRI.
	 */
	public static final IRI SEARCH;

	public static final IRI ALL_MATCHES;

	public static final IRI ALL_PROPERTIES;

	public static final IRI WITHIN_DISTANCE;

	public static final IRI DISTANCE;

	public static final IRI CONTEXT;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance(); // compatible with beta4:
		// creating a new factory
		LUCENE_QUERY = factory.createIRI(NAMESPACE + "LuceneQuery");
		SCORE = factory.createIRI(NAMESPACE + "score");
		QUERY = factory.createIRI(NAMESPACE + "query");
		PROPERTY = factory.createIRI(NAMESPACE + "property");
		SNIPPET = factory.createIRI(NAMESPACE + "snippet");
		MATCHES = factory.createIRI(NAMESPACE + "matches");

		INDEXID = factory.createIRI(NAMESPACE + "indexid");

		SEARCH = factory.createIRI(NAMESPACE + "search");
		ALL_MATCHES = factory.createIRI(NAMESPACE + "allMatches");
		ALL_PROPERTIES = factory.createIRI(NAMESPACE + "allProperties");

		WITHIN_DISTANCE = factory.createIRI(NAMESPACE + "withinDistance");
		DISTANCE = factory.createIRI(NAMESPACE + "distance");
		CONTEXT = factory.createIRI(NAMESPACE + "context");
	}
}
