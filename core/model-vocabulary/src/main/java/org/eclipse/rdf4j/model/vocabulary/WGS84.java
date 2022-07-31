/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
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

/**
 * Constants for the Basic Geo (WGS84 lat/long) Vocabulary.
 *
 * @author Alessandro Bollini
 * @see <a href="https://www.w3.org/2003/01/geo/">Basic Geo (WGS84 lat/long) Vocabulary</a>
 */
public class WGS84 {

	/**
	 * The WGS84 namespace ({@value}).
	 */
	public static final String NAMESPACE = "http://www.w3.org/2003/01/geo/wgs84_pos#";

	/**
	 * Recommended prefix for the RDF Schema namespace ({@value}).
	 */
	public static final String PREFIX = "wgs84";

	/**
	 * An immutable {@link Namespace} constant that represents the WGS84 namespace.
	 */
	public static final Namespace NS = Vocabularies.createNamespace(PREFIX, NAMESPACE);

	/** The {@code wgs84:SpatialThing} class. */
	public static final IRI SPATIAL_THING;

	/** The {@code wgs84:TemporalThing} class. */
	public static final IRI TEMPORAL_THING;

	/** The {@code wgs84:Event} class. */
	public static final IRI EVENT;

	/** The {@code wgs84:Point} relation. */
	public static final IRI POINT;

	/** The {@code wgs84:location} relation. */
	public static final IRI LOCATION;

	/** The {@code wgs84:lat} relation. */
	public static final IRI LAT;

	/** The {@code wgs84:long} relation. */
	public static final IRI LONG;

	/** The {@code wgs84:alt} relation. */
	public static final IRI ALT;

	/** The {@code wgs84:lat_long} relation. */
	public static final IRI LAT_LONG;

	static {

		SPATIAL_THING = Vocabularies.createIRI(NAMESPACE, "SpatialThing");
		TEMPORAL_THING = Vocabularies.createIRI(NAMESPACE, "TemporalThing");
		EVENT = Vocabularies.createIRI(NAMESPACE, "Event");

		POINT = Vocabularies.createIRI(NAMESPACE, "Point");
		LOCATION = Vocabularies.createIRI(NAMESPACE, "location");
		LAT = Vocabularies.createIRI(NAMESPACE, "lat");
		LONG = Vocabularies.createIRI(NAMESPACE, "long");
		ALT = Vocabularies.createIRI(NAMESPACE, "alt");
		LAT_LONG = Vocabularies.createIRI(NAMESPACE, "lat_long");

	}

}
