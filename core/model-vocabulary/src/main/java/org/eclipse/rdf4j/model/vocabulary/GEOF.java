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
package org.eclipse.rdf4j.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

/**
 * @see <a href=
 *      "http://www.opengeospatial.org/standards/geosparql">http://www.opengeospatial.org/standards/geosparql</a>
 */
public class GEOF {

	public static final String NAMESPACE = "http://www.opengis.net/def/function/geosparql/";

	public static final IRI DISTANCE;

	public static final IRI BUFFER;

	public static final IRI CONVEX_HULL;

	public static final IRI INTERSECTION;

	public static final IRI UNION;

	public static final IRI DIFFERENCE;

	public static final IRI SYM_DIFFERENCE;

	public static final IRI ENVELOPE;

	public static final IRI BOUNDARY;

	public static final IRI GET_SRID;

	public static final IRI RELATE;

	public static final IRI SF_EQUALS;

	public static final IRI SF_DISJOINT;

	public static final IRI SF_INTERSECTS;

	public static final IRI SF_TOUCHES;

	public static final IRI SF_CROSSES;

	public static final IRI SF_WITHIN;

	public static final IRI SF_CONTAINS;

	public static final IRI SF_OVERLAPS;

	public static final IRI EH_EQUALS;

	public static final IRI EH_DISJOINT;

	public static final IRI EH_MEET;

	public static final IRI EH_OVERLAP;

	public static final IRI EH_COVERS;

	public static final IRI EH_COVERED_BY;

	public static final IRI EH_INSIDE;

	public static final IRI EH_CONTAINS;

	public static final IRI RCC8_EQ;

	public static final IRI RCC8_DC;

	public static final IRI RCC8_EC;

	public static final IRI RCC8_PO;

	public static final IRI RCC8_TPPI;

	public static final IRI RCC8_TPP;

	public static final IRI RCC8_NTPP;

	public static final IRI RCC8_NTPPI;

	public static final String UOM_NAMESPACE = "http://www.opengis.net/def/uom/OGC/1.0/";

	public static final IRI UOM_DEGREE;

	public static final IRI UOM_RADIAN;

	public static final IRI UOM_UNITY;

	public static final IRI UOM_METRE;

	static {

		DISTANCE = Vocabularies.createIRI(NAMESPACE, "distance");
		BUFFER = Vocabularies.createIRI(NAMESPACE, "buffer");
		CONVEX_HULL = Vocabularies.createIRI(NAMESPACE, "convexHull");
		INTERSECTION = Vocabularies.createIRI(NAMESPACE, "intersection");
		UNION = Vocabularies.createIRI(NAMESPACE, "union");
		DIFFERENCE = Vocabularies.createIRI(NAMESPACE, "difference");
		SYM_DIFFERENCE = Vocabularies.createIRI(NAMESPACE, "symDifference");
		ENVELOPE = Vocabularies.createIRI(NAMESPACE, "envelope");
		BOUNDARY = Vocabularies.createIRI(NAMESPACE, "boundary");
		GET_SRID = Vocabularies.createIRI(NAMESPACE, "getSRID");

		RELATE = Vocabularies.createIRI(NAMESPACE, "relate");

		SF_EQUALS = Vocabularies.createIRI(NAMESPACE, "sfEquals");
		SF_DISJOINT = Vocabularies.createIRI(NAMESPACE, "sfDisjoint");
		SF_INTERSECTS = Vocabularies.createIRI(NAMESPACE, "sfIntersects");
		SF_TOUCHES = Vocabularies.createIRI(NAMESPACE, "sfTouches");
		SF_CROSSES = Vocabularies.createIRI(NAMESPACE, "sfCrosses");
		SF_WITHIN = Vocabularies.createIRI(NAMESPACE, "sfWithin");
		SF_CONTAINS = Vocabularies.createIRI(NAMESPACE, "sfContains");
		SF_OVERLAPS = Vocabularies.createIRI(NAMESPACE, "sfOverlaps");

		EH_EQUALS = Vocabularies.createIRI(NAMESPACE, "ehEquals");
		EH_DISJOINT = Vocabularies.createIRI(NAMESPACE, "ehDisjoint");
		EH_MEET = Vocabularies.createIRI(NAMESPACE, "ehMeet");
		EH_OVERLAP = Vocabularies.createIRI(NAMESPACE, "ehOverlap");
		EH_COVERS = Vocabularies.createIRI(NAMESPACE, "ehCovers");
		EH_COVERED_BY = Vocabularies.createIRI(NAMESPACE, "ehCoveredBy");
		EH_INSIDE = Vocabularies.createIRI(NAMESPACE, "ehInside");
		EH_CONTAINS = Vocabularies.createIRI(NAMESPACE, "ehContains");

		RCC8_EQ = Vocabularies.createIRI(NAMESPACE, "rcc8eq");
		RCC8_DC = Vocabularies.createIRI(NAMESPACE, "rcc8dc");
		RCC8_EC = Vocabularies.createIRI(NAMESPACE, "rcc8ec");
		RCC8_PO = Vocabularies.createIRI(NAMESPACE, "rcc8po");
		RCC8_TPPI = Vocabularies.createIRI(NAMESPACE, "rcc8tppi");
		RCC8_TPP = Vocabularies.createIRI(NAMESPACE, "rcc8tpp");
		RCC8_NTPP = Vocabularies.createIRI(NAMESPACE, "rcc8ntpp");
		RCC8_NTPPI = Vocabularies.createIRI(NAMESPACE, "rcc8ntppi");

		UOM_DEGREE = Vocabularies.createIRI(UOM_NAMESPACE, "degree");
		UOM_RADIAN = Vocabularies.createIRI(UOM_NAMESPACE, "radian");
		UOM_UNITY = Vocabularies.createIRI(UOM_NAMESPACE, "unity");
		UOM_METRE = Vocabularies.createIRI(UOM_NAMESPACE, "metre");
	}
}
