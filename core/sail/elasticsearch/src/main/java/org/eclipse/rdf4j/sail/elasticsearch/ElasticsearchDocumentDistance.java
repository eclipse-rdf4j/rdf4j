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
package org.eclipse.rdf4j.sail.elasticsearch;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.search.SearchHit;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;

import com.google.common.base.Function;

public class ElasticsearchDocumentDistance extends ElasticsearchDocumentResult implements DocumentDistance {

	private final String geoPointField;

	private final IRI units;

	private final GeoPoint srcPoint;

	private final DistanceUnit unit;

	public ElasticsearchDocumentDistance(SearchHit hit,
			Function<? super String, ? extends SpatialContext> geoContextMapper, String geoPointField, IRI units,
			GeoPoint srcPoint, DistanceUnit unit) {
		super(hit, geoContextMapper);
		this.geoPointField = geoPointField;
		this.units = units;
		this.srcPoint = srcPoint;
		this.unit = unit;
	}

	@Override
	public double getDistance() {
		String geohash = (String) ((ElasticsearchDocument) getDocument()).getSource().get(geoPointField);
		GeoPoint dstPoint = GeoPoint.fromGeohash(geohash);

		double unitDist = GeoDistance.ARC.calculate(srcPoint.getLat(), srcPoint.getLon(), dstPoint.getLat(),
				dstPoint.getLon(), unit);
		double distance;
		if (GEOF.UOM_METRE.equals(units)) {
			distance = unit.toMeters(unitDist);
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			distance = unitDist / unit.getDistancePerDegree();
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			distance = DistanceUtils.dist2Radians(unit.convert(unitDist, DistanceUnit.KILOMETERS),
					DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			distance = unit.convert(unitDist, DistanceUnit.KILOMETERS) / (Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else {
			throw new UnsupportedOperationException("Unsupported units: " + units);
		}
		return distance;
	}
}
