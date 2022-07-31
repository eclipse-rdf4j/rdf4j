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
package org.eclipse.rdf4j.sail.lucene.util;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.locationtech.spatial4j.distance.DistanceUtils;

public final class GeoUnits {

	private GeoUnits() {
	}

	public static final double toMiles(double distance, IRI units) {
		final double miles;
		if (GEOF.UOM_METRE.equals(units)) {
			miles = DistanceUtils.KM_TO_MILES * distance / 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			miles = DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_MI);
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			miles = DistanceUtils.radians2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_MI);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			miles = distance * Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_MI;
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return miles;
	}

	public static final double fromMiles(double miles, IRI units) {
		double dist;
		if (GEOF.UOM_METRE.equals(units)) {
			dist = DistanceUtils.MILES_TO_KM * miles * 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			dist = DistanceUtils.dist2Degrees(miles, DistanceUtils.EARTH_MEAN_RADIUS_MI);
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			dist = DistanceUtils.dist2Radians(miles, DistanceUtils.EARTH_MEAN_RADIUS_MI);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			dist = miles / (Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_MI);
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return dist;
	}

	public static final double toKilometres(double distance, IRI units) {
		final double kms;
		if (GEOF.UOM_METRE.equals(units)) {
			kms = distance / 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			kms = DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			kms = DistanceUtils.radians2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			kms = distance * Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM;
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return kms;
	}

	public static final double fromKilometres(double kms, IRI units) {
		double dist;
		if (GEOF.UOM_METRE.equals(units)) {
			dist = kms * 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			dist = DistanceUtils.dist2Degrees(kms, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			dist = DistanceUtils.dist2Radians(kms, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			dist = kms / (Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return dist;
	}

	public static final double toDegrees(double distance, IRI units) {
		final double degs;
		if (GEOF.UOM_METRE.equals(units)) {
			degs = DistanceUtils.dist2Degrees(distance / 1000.0, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			degs = distance;
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			degs = DistanceUtils.RADIANS_TO_DEGREES * distance;
		} else if (GEOF.UOM_UNITY.equals(units)) {
			degs = distance * 180.0;
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return degs;
	}

	public static final double fromDegrees(double degs, IRI units) {
		double dist;
		if (GEOF.UOM_METRE.equals(units)) {
			dist = DistanceUtils.degrees2Dist(degs, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 1000.0;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			dist = degs;
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			dist = DistanceUtils.DEGREES_TO_RADIANS * degs;
		} else if (GEOF.UOM_UNITY.equals(units)) {
			dist = degs / 180.0;
		} else {
			throw new IllegalArgumentException("Unsupported units: " + units);
		}
		return dist;
	}
}
