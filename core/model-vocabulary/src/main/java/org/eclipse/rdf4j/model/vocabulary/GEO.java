/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

/**
 * @version 1.0
 * @see <a href=
 *      "http://www.opengeospatial.org/standards/geosparql">http://www.opengeospatial.org/standards/geosparql</a>
 */
public class GEO {

	public static final String NAMESPACE = "http://www.opengis.net/ont/geosparql#";

	public static final IRI AS_WKT;

	public static final IRI WKT_LITERAL;

	public static final String DEFAULT_SRID = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

	static {
		AS_WKT = Vocabularies.createIRI(NAMESPACE, "asWKT");
		WKT_LITERAL = Vocabularies.createIRI(NAMESPACE, "wktLiteral");
	}
}
