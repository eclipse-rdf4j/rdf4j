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
 * @see http://www.opengeospatial.org/standards/geosparql
 */
public class GEOF {

	public static final String NAMESPACE = "http://www.opengis.net/def/function/geosparql/";

	public static final URI DISTANCE;
	public static final URI BUFFER;
	public static final URI CONVEX_HULL;
	public static final URI INTERSECTION;
	public static final URI UNION;
	public static final URI DIFFERENCE;
	public static final URI SYM_DIFFERENCE;
	public static final URI ENVELOPE;
	public static final URI BOUNDARY;
	public static final URI GET_SRID;

	public static final URI RELATE;

	public static final URI SF_EQUALS;
	public static final URI SF_DISJOINT;
	public static final URI SF_INTERSECTS;
	public static final URI SF_TOUCHES;
	public static final URI SF_CROSSES;
	public static final URI SF_WITHIN;
	public static final URI SF_CONTAINS;
	public static final URI SF_OVERLAPS;

	public static final URI EH_EQUALS;
	public static final URI EH_DISJOINT;
	public static final URI EH_MEET;
	public static final URI EH_OVERLAP;
	public static final URI EH_COVERS;
	public static final URI EH_COVERED_BY;
	public static final URI EH_INSIDE;
	public static final URI EH_CONTAINS;

	public static final URI RCC8_EQ;
	public static final URI RCC8_DC;
	public static final URI RCC8_EC;
	public static final URI RCC8_PO;
	public static final URI RCC8_TPPI;
	public static final URI RCC8_TPP;
	public static final URI RCC8_NTPP;
	public static final URI RCC8_NTPPI;

	public static final String UOM_NAMESPACE = "http://www.opengis.net/def/uom/OGC/1.0/";
	public static final URI UOM_DEGREE;
	public static final URI UOM_RADIAN;
	public static final URI UOM_UNITY;
	public static final URI UOM_METRE;

	static {
		ValueFactory factory = ValueFactoryImpl.getInstance();
		DISTANCE = factory.createURI(NAMESPACE, "distance");
		BUFFER = factory.createURI(NAMESPACE, "buffer");
		CONVEX_HULL = factory.createURI(NAMESPACE, "convexHull");
		INTERSECTION = factory.createURI(NAMESPACE, "intersection");
		UNION = factory.createURI(NAMESPACE, "union");
		DIFFERENCE = factory.createURI(NAMESPACE, "difference");
		SYM_DIFFERENCE = factory.createURI(NAMESPACE, "symDifference");
		ENVELOPE = factory.createURI(NAMESPACE, "envelope");
		BOUNDARY = factory.createURI(NAMESPACE, "boundary");
		GET_SRID = factory.createURI(NAMESPACE, "getSRID");

		RELATE = factory.createURI(NAMESPACE, "relate");

		SF_EQUALS = factory.createURI(NAMESPACE, "sfEquals");
		SF_DISJOINT = factory.createURI(NAMESPACE, "sfDisjoint");
		SF_INTERSECTS = factory.createURI(NAMESPACE, "sfIntersects");
		SF_TOUCHES = factory.createURI(NAMESPACE, "sfTouches");
		SF_CROSSES = factory.createURI(NAMESPACE, "sfCrosses");
		SF_WITHIN = factory.createURI(NAMESPACE, "sfWithin");
		SF_CONTAINS = factory.createURI(NAMESPACE, "sfContains");
		SF_OVERLAPS = factory.createURI(NAMESPACE, "sfOverlaps");

		EH_EQUALS = factory.createURI(NAMESPACE, "ehEquals");
		EH_DISJOINT = factory.createURI(NAMESPACE, "ehDisjoint");
		EH_MEET = factory.createURI(NAMESPACE, "ehMeet");
		EH_OVERLAP = factory.createURI(NAMESPACE, "ehOverlap");
		EH_COVERS = factory.createURI(NAMESPACE, "ehCovers");
		EH_COVERED_BY = factory.createURI(NAMESPACE, "ehCoveredBy");
		EH_INSIDE = factory.createURI(NAMESPACE, "ehInside");
		EH_CONTAINS = factory.createURI(NAMESPACE, "ehContains");

		RCC8_EQ = factory.createURI(NAMESPACE, "rcc8eq");
		RCC8_DC = factory.createURI(NAMESPACE, "rcc8dc");
		RCC8_EC = factory.createURI(NAMESPACE, "rcc8ec");
		RCC8_PO = factory.createURI(NAMESPACE, "rcc8po");
		RCC8_TPPI = factory.createURI(NAMESPACE, "rcc8tppi");
		RCC8_TPP = factory.createURI(NAMESPACE, "rcc8tpp");
		RCC8_NTPP = factory.createURI(NAMESPACE, "rcc8ntpp");
		RCC8_NTPPI = factory.createURI(NAMESPACE, "rcc8ntppi");

		UOM_DEGREE = factory.createURI(UOM_NAMESPACE, "degree");
		UOM_RADIAN = factory.createURI(UOM_NAMESPACE, "radian");
		UOM_UNITY = factory.createURI(UOM_NAMESPACE, "unity");
		UOM_METRE = factory.createURI(UOM_NAMESPACE, "metre");
	}
}
