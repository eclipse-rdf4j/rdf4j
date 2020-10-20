/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.vocabulary;

import static org.eclipse.rdf4j.model.base.AbstractIRI.createIRI;

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

		DISTANCE = createIRI(NAMESPACE, "distance");
		BUFFER = createIRI(NAMESPACE, "buffer");
		CONVEX_HULL = createIRI(NAMESPACE, "convexHull");
		INTERSECTION = createIRI(NAMESPACE, "intersection");
		UNION = createIRI(NAMESPACE, "union");
		DIFFERENCE = createIRI(NAMESPACE, "difference");
		SYM_DIFFERENCE = createIRI(NAMESPACE, "symDifference");
		ENVELOPE = createIRI(NAMESPACE, "envelope");
		BOUNDARY = createIRI(NAMESPACE, "boundary");
		GET_SRID = createIRI(NAMESPACE, "getSRID");

		RELATE = createIRI(NAMESPACE, "relate");

		SF_EQUALS = createIRI(NAMESPACE, "sfEquals");
		SF_DISJOINT = createIRI(NAMESPACE, "sfDisjoint");
		SF_INTERSECTS = createIRI(NAMESPACE, "sfIntersects");
		SF_TOUCHES = createIRI(NAMESPACE, "sfTouches");
		SF_CROSSES = createIRI(NAMESPACE, "sfCrosses");
		SF_WITHIN = createIRI(NAMESPACE, "sfWithin");
		SF_CONTAINS = createIRI(NAMESPACE, "sfContains");
		SF_OVERLAPS = createIRI(NAMESPACE, "sfOverlaps");

		EH_EQUALS = createIRI(NAMESPACE, "ehEquals");
		EH_DISJOINT = createIRI(NAMESPACE, "ehDisjoint");
		EH_MEET = createIRI(NAMESPACE, "ehMeet");
		EH_OVERLAP = createIRI(NAMESPACE, "ehOverlap");
		EH_COVERS = createIRI(NAMESPACE, "ehCovers");
		EH_COVERED_BY = createIRI(NAMESPACE, "ehCoveredBy");
		EH_INSIDE = createIRI(NAMESPACE, "ehInside");
		EH_CONTAINS = createIRI(NAMESPACE, "ehContains");

		RCC8_EQ = createIRI(NAMESPACE, "rcc8eq");
		RCC8_DC = createIRI(NAMESPACE, "rcc8dc");
		RCC8_EC = createIRI(NAMESPACE, "rcc8ec");
		RCC8_PO = createIRI(NAMESPACE, "rcc8po");
		RCC8_TPPI = createIRI(NAMESPACE, "rcc8tppi");
		RCC8_TPP = createIRI(NAMESPACE, "rcc8tpp");
		RCC8_NTPP = createIRI(NAMESPACE, "rcc8ntpp");
		RCC8_NTPPI = createIRI(NAMESPACE, "rcc8ntppi");

		UOM_DEGREE = createIRI(UOM_NAMESPACE, "degree");
		UOM_RADIAN = createIRI(UOM_NAMESPACE, "radian");
		UOM_UNITY = createIRI(UOM_NAMESPACE, "unity");
		UOM_METRE = createIRI(UOM_NAMESPACE, "metre");
	}
}
