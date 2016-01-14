/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import org.elasticsearch.common.geo.GeoDistance.FixedSourceDistance;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.search.SearchHit;

import com.google.common.base.Function;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;

public class ElasticsearchDocumentDistance extends ElasticsearchDocumentResult implements DocumentDistance {

	private final String geoPointField;

	private final URI units;

	private final FixedSourceDistance srcDistance;

	private final DistanceUnit unit;

	public ElasticsearchDocumentDistance(SearchHit hit, Function<? super String,? extends SpatialContext> geoContextMapper, String geoPointField,
			URI units, FixedSourceDistance srcDistance, DistanceUnit unit)
	{
		super(hit, geoContextMapper);
		this.geoPointField = geoPointField;
		this.units = units;
		this.srcDistance = srcDistance;
		this.unit = unit;
	}

	@Override
	public double getDistance() {
		String geohash = (String)((ElasticsearchDocument)getDocument()).getSource().get(geoPointField);
		GeoPoint point = GeoHashUtils.decode(geohash);
		double unitDist = srcDistance.calculate(point.getLat(), point.getLon());
		double distance;
		if (GEOF.UOM_METRE.equals(units)) {
			distance = unit.toMeters(unitDist);
		}
		else if (GEOF.UOM_DEGREE.equals(units)) {
			distance = unitDist / unit.getDistancePerDegree();
		}
		else if (GEOF.UOM_RADIAN.equals(units)) {
			distance = DistanceUtils.dist2Radians(unit.convert(unitDist, DistanceUnit.KILOMETERS),
					DistanceUtils.EARTH_MEAN_RADIUS_KM);
		}
		else if (GEOF.UOM_UNITY.equals(units)) {
			distance = unit.convert(unitDist, DistanceUnit.KILOMETERS)
					/ (Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM);
		}
		else {
			throw new UnsupportedOperationException("Unsupported units: " + units);
		}
		return distance;
	}
}
