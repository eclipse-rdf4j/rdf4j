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
 * @see http://www.opengeospatial.org/standards/geosparql
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
		ValueFactory factory = SimpleValueFactory.getInstance();
		DISTANCE = factory.createIRI(NAMESPACE, "distance");
		BUFFER = factory.createIRI(NAMESPACE, "buffer");
		CONVEX_HULL = factory.createIRI(NAMESPACE, "convexHull");
		INTERSECTION = factory.createIRI(NAMESPACE, "intersection");
		UNION = factory.createIRI(NAMESPACE, "union");
		DIFFERENCE = factory.createIRI(NAMESPACE, "difference");
		SYM_DIFFERENCE = factory.createIRI(NAMESPACE, "symDifference");
		ENVELOPE = factory.createIRI(NAMESPACE, "envelope");
		BOUNDARY = factory.createIRI(NAMESPACE, "boundary");
		GET_SRID = factory.createIRI(NAMESPACE, "getSRID");

		RELATE = factory.createIRI(NAMESPACE, "relate");

		SF_EQUALS = factory.createIRI(NAMESPACE, "sfEquals");
		SF_DISJOINT = factory.createIRI(NAMESPACE, "sfDisjoint");
		SF_INTERSECTS = factory.createIRI(NAMESPACE, "sfIntersects");
		SF_TOUCHES = factory.createIRI(NAMESPACE, "sfTouches");
		SF_CROSSES = factory.createIRI(NAMESPACE, "sfCrosses");
		SF_WITHIN = factory.createIRI(NAMESPACE, "sfWithin");
		SF_CONTAINS = factory.createIRI(NAMESPACE, "sfContains");
		SF_OVERLAPS = factory.createIRI(NAMESPACE, "sfOverlaps");

		EH_EQUALS = factory.createIRI(NAMESPACE, "ehEquals");
		EH_DISJOINT = factory.createIRI(NAMESPACE, "ehDisjoint");
		EH_MEET = factory.createIRI(NAMESPACE, "ehMeet");
		EH_OVERLAP = factory.createIRI(NAMESPACE, "ehOverlap");
		EH_COVERS = factory.createIRI(NAMESPACE, "ehCovers");
		EH_COVERED_BY = factory.createIRI(NAMESPACE, "ehCoveredBy");
		EH_INSIDE = factory.createIRI(NAMESPACE, "ehInside");
		EH_CONTAINS = factory.createIRI(NAMESPACE, "ehContains");

		RCC8_EQ = factory.createIRI(NAMESPACE, "rcc8eq");
		RCC8_DC = factory.createIRI(NAMESPACE, "rcc8dc");
		RCC8_EC = factory.createIRI(NAMESPACE, "rcc8ec");
		RCC8_PO = factory.createIRI(NAMESPACE, "rcc8po");
		RCC8_TPPI = factory.createIRI(NAMESPACE, "rcc8tppi");
		RCC8_TPP = factory.createIRI(NAMESPACE, "rcc8tpp");
		RCC8_NTPP = factory.createIRI(NAMESPACE, "rcc8ntpp");
		RCC8_NTPPI = factory.createIRI(NAMESPACE, "rcc8ntppi");

		UOM_DEGREE = factory.createIRI(UOM_NAMESPACE, "degree");
		UOM_RADIAN = factory.createIRI(UOM_NAMESPACE, "radian");
		UOM_UNITY = factory.createIRI(UOM_NAMESPACE, "unity");
		UOM_METRE = factory.createIRI(UOM_NAMESPACE, "metre");
	}
}
