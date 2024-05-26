/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
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
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * @version 1.1
 * @see <a href=
 *      "http://www.opengeospatial.org/standards/geosparql">http://www.opengeospatial.org/standards/geosparql</a>
 */
public class GEO {

	/**
	 * The GEO namespace: http://www.opengis.net/ont/geosparql#
	 */
	public static final String NAMESPACE = CoreDatatype.GEO.NAMESPACE;

	/**
	 * The recommended prefix for the GEO namespace: "geo"
	 */
	public static final String PREFIX = "geo";

	/**
	 * An immutable {@link Namespace} constant that represents the GEO namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	// classes

	/**
	 * The geo:Feature class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#Feature">The geo:Feature Class</a>
	 */
	public static final IRI Feature = createIRI("Feature");

	/**
	 * The geo:FeatureCollection class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#FeatureCollection">The geo:FeatureCollection Class</a>
	 */
	public static final IRI FeatureCollection = createIRI("FeatureCollection");

	/**
	 * The geo:Geometry class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#Geometry">The geo:Geometry Class</a>
	 */
	public static final IRI Geometry = createIRI("Geometry");

	/**
	 * The geo:GeometryCollection class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#GeometryCollection">The geo:GeometryCollection Class</a>
	 */
	public static final IRI GeometryCollection = createIRI("GeometryCollection");

	/**
	 * The geo:SpatialObject class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#SpatialObject">The geo:SpatialObject Class</a>
	 */
	public static final IRI SpatialObject = createIRI("SpatialObject");

	/**
	 * The geo:SpatialObjectCollection class
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#SpatialObjectCollection">The geo:SpatialObjectCollection
	 *      Class</a>
	 */
	public static final IRI SpatialObjectCollection = createIRI("SpatialObjectCollection");

	// Object Properties

	/**
	 * The geo:defaultGeometry property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#defaultGeometry">The geo:defaultGeometry property</a>
	 */
	public static final IRI defaultGeometry = createIRI("defaultGeometry");

	/**
	 * The geo:ehContains property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehContains">The geo:ehContains property</a>
	 */
	public static final IRI ehContains = createIRI("ehContains");

	/**
	 * The geo:ehCoveredBy property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehCoveredBy">The geo:ehCoveredBy property</a>
	 */
	public static final IRI ehCoveredBy = createIRI("ehCoveredBy");

	/**
	 * The geo:ehCovers property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehCovers">The geo:ehCovers property</a>
	 */
	public static final IRI ehCovers = createIRI("ehCovers");

	/**
	 * The geo:ehDisjoint property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehDisjoint">The geo:ehDisjoint property</a>
	 */
	public static final IRI ehDisjoint = createIRI("ehDisjoint");

	/**
	 * The geo:ehEquals property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehEquals">The geo:ehEquals property</a>
	 */
	public static final IRI ehEquals = createIRI("ehEquals");

	/**
	 * The geo:ehInside property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehInside">The geo:ehInside property</a>
	 */
	public static final IRI ehInside = createIRI("ehInside");

	/**
	 * The geo:ehMeet property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehMeet">The geo:ehMeet property</a>
	 */
	public static final IRI ehMeet = createIRI("ehMeet");

	/**
	 * The geo:ehOverlap property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#ehOverlap">The geo:ehOverlap property</a>
	 */
	public static final IRI ehOverlap = createIRI("ehOverlap");

	/**
	 * The geo:hasArea property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasArea">The geo:hasArea property</a>
	 */
	public static final IRI hasArea = createIRI("hasArea");

	/**
	 * The geo:hasBoundingBox property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasBoundingBox">The geo:hasBoundingBox property</a>
	 */
	public static final IRI hasBoundingBox = createIRI("hasBoundingBox");

	/**
	 * The geo:hasCentroid property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasCentroid">The geo:hasCentroid property</a>
	 */
	public static final IRI hasCentroid = createIRI("hasCentroid");

	/**
	 * The geo:hasDefaultGeometry property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasDefaultGeometry">The geo:hasDefaultGeometry property</a>
	 */
	public static final IRI hasDefaultGeometry = createIRI("hasDefaultGeometry");

	/**
	 * The geo:hasGeometry property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasGeometry">The geo:hasGeometry property</a>
	 */
	public static final IRI hasGeometry = createIRI("hasGeometry");

	/**
	 * The geo:hasLength property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasLength">The geo:hasLength property</a>
	 */
	public static final IRI hasLength = createIRI("hasLength");

	/**
	 * The geo:hasPerimeterLength property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasPerimeterLength">The geo:hasPerimeterLength property</a>
	 */
	public static final IRI hasPerimeterLength = createIRI("hasPerimeterLength");

	/**
	 * The geo:hasSize property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasSize">The geo:hasSize property</a>
	 */
	public static final IRI hasSize = createIRI("hasSize");

	/**
	 * The geo:hasSpatialAccuracy property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasSpatialAccuracy">The geo:hasSpatialAccuracy property</a>
	 */
	public static final IRI hasSpatialAccuracy = createIRI("hasSpatialAccuracy");

	/**
	 * The geo:hasSpatialResolution property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasSpatialResolution">The geo:hasSpatialResolution
	 *      property</a>
	 */
	public static final IRI hasSpatialResolution = createIRI("hasSpatialResolution");

	/**
	 * The geo:hasVolume property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasVolume">The geo:hasVolume property</a>
	 */
	public static final IRI hasVolume = createIRI("hasVolume");

	/**
	 * The geo:rcc8dc property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8dc">The geo:rcc8dc property</a>
	 */
	public static final IRI rcc8dc = createIRI("rcc8dc");

	/**
	 * The geo:rcc8ec property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8ec">The geo:rcc8ec property</a>
	 */
	public static final IRI rcc8ec = createIRI("rcc8ec");

	/**
	 * The geo:rcc8eq property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8eq">The geo:rcc8eq property</a>
	 */
	public static final IRI rcc8eq = createIRI("rcc8eq");

	/**
	 * The geo:rcc8ntpp property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8ntpp">The geo:rcc8ntpp property</a>
	 */
	public static final IRI rcc8ntpp = createIRI("rcc8ntpp");

	/**
	 * The geo:rcc8ntppi property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8ntppi">The geo:rcc8ntppi property</a>
	 */
	public static final IRI rcc8ntppi = createIRI("rcc8ntppi");

	/**
	 * The geo:rcc8po property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8po">The geo:rcc8po property</a>
	 */
	public static final IRI rcc8po = createIRI("rcc8po");

	/**
	 * The geo:rcc8tpp property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8tpp">The geo:rcc8tpp property</a>
	 */
	public static final IRI rcc8tpp = createIRI("rcc8tpp");

	/**
	 * The geo:rcc8tppi property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#rcc8tppi">The geo:rcc8tppi property</a>
	 */
	public static final IRI rcc8tppi = createIRI("rcc8tppi");

	/**
	 * The geo:sfContains property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfContains">The geo:sfContains property</a>
	 */
	public static final IRI sfContains = createIRI("sfContains");

	/**
	 * The geo:sfCrosses property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfCrosses">The geo:sfCrosses property</a>
	 */
	public static final IRI sfCrosses = createIRI("sfCrosses");

	/**
	 * The geo:sfDisjoint property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfDisjoint">The geo:sfDisjoint property</a>
	 */
	public static final IRI sfDisjoint = createIRI("sfDisjoint");

	/**
	 * The geo:sfEquals property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfEquals">The geo:sfEquals property</a>
	 */
	public static final IRI sfEquals = createIRI("sfEquals");

	/**
	 * The geo:sfIntersects property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfIntersects>The geo:sfIntersects property</a>
	 */
	public static final IRI sfIntersects = createIRI("sfIntersects");

	/**
	 * The geo:sfOverlaps property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfOverlaps">The geo:sfOverlaps property</a>
	 */
	public static final IRI sfOverlaps = createIRI("sfOverlaps");

	/**
	 * The geo:sfTouchest property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfTouches">The geo:sfTouches property</a>
	 */
	public static final IRI sfTouches = createIRI("sfTouches");

	/**
	 * The geo:sfWithin property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#sfWithin">The geo:sfWithin property</a>
	 */
	public static final IRI sfWithin = createIRI("sfWithin");

	// Datatype Properties

	/**
	 * The geo:asDGGS property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#asDGGS">The geo:asDGGST property</a>
	 */
	public static final IRI asDGGS = createIRI("asDGGS");

	/**
	 * The geo:asGML property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#asGML">The geo:asGML property</a>
	 */
	public static final IRI asGML = createIRI("asGML");

	/**
	 * The geo:asGeoJSON property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#asGeoJSON">The geo:asGeoJSON property</a>
	 */
	public static final IRI asGeoJSON = createIRI("asGeoJSON");

	/**
	 * The geo:asKML property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#asKML">The geo:asKML property</a>
	 */
	public static final IRI asKML = createIRI("asKML");

	/**
	 * The geo:coordinateDimension property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#coordinateDimension">The geo:coordinateDimension property</a>
	 */
	public static final IRI coordinateDimension = createIRI("coordinateDimension");

	/**
	 * The geo:dimension property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#dimension">The geo:dimension property</a>
	 */
	public static final IRI dimension = createIRI("dimension");

	/**
	 * The geo:hasMetricArea property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricArea">The geo:hasMetricArea property</a>
	 */
	public static final IRI hasMetricArea = createIRI("hasMetricArea");

	/**
	 * The geo:hasMetricLength property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricLength">The geo:hasMetricLength property</a>
	 */
	public static final IRI hasMetricLength = createIRI("hasMetricLength");

	/**
	 * The geo:hasMetricPerimeterLength property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricPerimeterLength">The geo:hasMetricPerimeterLength
	 *      property</a>
	 */
	public static final IRI hasMetricPerimeterLength = createIRI("hasMetricPerimeterLength");

	/**
	 * The geo:hasMetricSize property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricSize">The geo:hasMetricSize property</a>
	 */
	public static final IRI hasMetricSize = createIRI("hasMetricSize");

	/**
	 * The geo:hasMetricSpatialAccuracy property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricSpatialAccuracyT">The geo:hasMetricSpatialAccuracyT
	 *      property</a>
	 */
	public static final IRI hasMetricSpatialAccuracy = createIRI("hasMetricSpatialAccuracy");

	/**
	 * The geo:hasMetricSpatialResolution property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricSpatialResolution">The geo:hasMetricSpatialResolution
	 *      property</a>
	 */
	public static final IRI hasMetricSpatialResolution = createIRI("hasMetricSpatialResolution");

	/**
	 * The geo:hasMetricVolume property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasMetricVolume">The geo:hasMetricVolume property</a>
	 */
	public static final IRI hasMetricVolume = createIRI("hasMetricVolume");

	/**
	 * The geo:hasSerialization property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#hasSerialization">The geo:hasSerialization property</a>
	 */
	public static final IRI hasSerialization = createIRI("hasSerialization");

	/**
	 * The geo:isEmpty property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#isEmpty">The geo:isEmpty property</a>
	 */
	public static final IRI isEmpty = createIRI("isEmpty");

	/**
	 * The geo:isSimple property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#isSimple">The geo:isSimple property</a>
	 */
	public static final IRI isSimple = createIRI("isSimple");

	/**
	 * The geo:spatialDimension property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#spatialDimension">The geo:spatialDimension property</a>
	 */
	public static final IRI spatialDimension = createIRI("spatialDimension");

	// leaving the following constants as it is

	/**
	 * The geo:asWKT property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#asWKT">The geo:asWKT property</a>
	 */
	public static final IRI AS_WKT = createIRI("asWKT");

	/**
	 * The geo:wktLiteral property
	 *
	 * @see <a href="http://www.opengis.net/ont/geosparql#wktLiteral">The geo:wktLiteral property</a>
	 */
	public static final IRI WKT_LITERAL = createIRI("wktLiteral");

	public static final String DEFAULT_SRID = "http://www.opengis.net/def/crs/OGC/1.3/CRS84";

	private static IRI createIRI(String localName) {
		return Vocabularies.createIRI(NAMESPACE, localName);
	}
}
