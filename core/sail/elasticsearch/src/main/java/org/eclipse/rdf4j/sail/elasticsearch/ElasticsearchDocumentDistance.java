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

import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;

import com.google.common.base.Function;

import co.elastic.clients.elasticsearch.core.search.Hit;

public class ElasticsearchDocumentDistance extends ElasticsearchDocumentResult implements DocumentDistance {

	private final String geoPointField;

	private final IRI units;

	private final double srcLat;

	private final double srcLon;

	public ElasticsearchDocumentDistance(Hit<Map<String, Object>> hit,
			Function<? super String, ? extends SpatialContext> geoContextMapper, String geoPointField, IRI units,
			double srcLat, double srcLon) {
		super(hit, geoContextMapper);
		this.geoPointField = geoPointField;
		this.units = units;
		this.srcLat = srcLat;
		this.srcLon = srcLon;
	}

	@Override
	public double getDistance() {
		Object pointValue = ((ElasticsearchDocument) getDocument()).getSource().get(geoPointField);
		if (!(pointValue instanceof Map)) {
			return 0;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> point = (Map<String, Object>) pointValue;
		Double dstLat = asDouble(point.get("lat"));
		Double dstLon = asDouble(point.get("lon"));
		if (dstLat == null || dstLon == null) {
			return 0;
		}

		double distRad = DistanceUtils.distHaversineRAD(srcLat, srcLon, dstLat, dstLon);
		double distKm = DistanceUtils.radians2Dist(distRad, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		double distance;
		if (GEOF.UOM_METRE.equals(units)) {
			distance = distKm * 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			distance = distKm / DistanceUtils.EARTH_MEAN_RADIUS_KM * (180.0 / Math.PI);
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			distance = distRad;
		} else if (GEOF.UOM_UNITY.equals(units)) {
			distance = distKm / (Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else {
			throw new UnsupportedOperationException("Unsupported units: " + units);
		}
		return distance;
	}

	private Double asDouble(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		return null;
	}
}
