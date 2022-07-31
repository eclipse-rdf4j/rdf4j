/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsShapeFactory;

/**
 * JTS-enabled implementation of spatial algebra, with full support for polygon-related geospatial functions
 */
public class JtsSpatialAlgebra implements SpatialAlgebra {

	private final JtsShapeFactory shapeFactory;

	public JtsSpatialAlgebra(JtsSpatialContext context) {
		this.shapeFactory = context.getShapeFactory();
	}

	@Override
	public Shape buffer(Shape s, double distance) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).buffer(distance));
	}

	@Override
	public Shape convexHull(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).convexHull());
	}

	@Override
	public Shape boundary(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).getBoundary());
	}

	@Override
	public Shape envelope(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).getEnvelope());
	}

	@Override
	public Shape union(Shape s1, Shape s2) {
		return shapeFactory
				.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s1).union(shapeFactory.getGeometryFrom(s2)));
	}

	@Override
	public Shape intersection(Shape s1, Shape s2) {
		Geometry intersection = shapeFactory.getGeometryFrom(s1).intersection(shapeFactory.getGeometryFrom(s2));
		if (intersection.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(intersection);
	}

	@Override
	public Shape symDifference(Shape s1, Shape s2) {
		Geometry symDiff = shapeFactory.getGeometryFrom(s1).symDifference(shapeFactory.getGeometryFrom(s2));
		if (symDiff.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(symDiff);
	}

	@Override
	public Shape difference(Shape s1, Shape s2) {
		Geometry difference = shapeFactory.getGeometryFrom(s1).difference(shapeFactory.getGeometryFrom(s2));
		if (difference.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(difference);
	}

	@Override
	public boolean relate(Shape s1, Shape s2, String intersectionPattern) {
		return shapeFactory.getGeometryFrom(s1).relate(shapeFactory.getGeometryFrom(s2), intersectionPattern);
	}

	@Override
	public boolean sfEquals(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFFTFFFT");
	}

	@Override
	public boolean sfDisjoint(Shape s1, Shape s2) {
		return relate(s1, s2, "FF*FF****");
	}

	@Override
	public boolean sfIntersects(Shape s1, Shape s2) {
		return relate(s1, s2, "T********") || relate(s1, s2, "*T*******") || relate(s1, s2, "***T*****")
				|| relate(s1, s2, "****T****");
	}

	@Override
	public boolean sfTouches(Shape s1, Shape s2) {
		return relate(s1, s2, "FT*******") || relate(s1, s2, "F**T*****") || relate(s1, s2, "F***T****");
	}

	@Override
	public boolean sfCrosses(Shape s1, Shape s2) {
		Geometry g1 = shapeFactory.getGeometryFrom(s1);
		Geometry g2 = shapeFactory.getGeometryFrom(s2);
		int d1 = g1.getDimension();
		int d2 = g2.getDimension();
		if ((d1 == 0 && d2 == 1) || (d1 == 0 && d2 == 2) || (d1 == 1 && d2 == 2)) {
			return g1.relate(g2, "T*T***T**");
		} else if (d1 == 1 && d2 == 1) {
			return g1.relate(g2, "0*T***T**");
		} else {
			return false;
		}
	}

	@Override
	public boolean sfWithin(Shape s1, Shape s2) {
		return relate(s1, s2, "T*F**F***");
	}

	@Override
	public boolean sfContains(Shape s1, Shape s2) {
		return relate(s1, s2, "T*****FF*");
	}

	@Override
	public boolean sfOverlaps(Shape s1, Shape s2) {
		Geometry g1 = shapeFactory.getGeometryFrom(s1);
		Geometry g2 = shapeFactory.getGeometryFrom(s2);
		int d1 = g1.getDimension();
		int d2 = g2.getDimension();
		if ((d1 == 2 && d2 == 2) || (d1 == 0 && d2 == 0)) {
			return g1.relate(g2, "T*T***T**");
		} else if (d1 == 1 && d2 == 1) {
			return g1.relate(g2, "1*T***T**");
		} else {
			return false;
		}
	}

	@Override
	public boolean ehEquals(Shape s1, Shape s2) {
		return ehInside(s1, s2) && ehContains(s1, s2);
	}

	@Override
	public boolean ehDisjoint(Shape s1, Shape s2) {
		return relate(s1, s2, "FF*FF****");
	}

	@Override
	public boolean ehMeet(Shape s1, Shape s2) {
		return relate(s1, s2, "FT*******") || relate(s1, s2, "F**T*****") || relate(s1, s2, "F***T****");
	}

	@Override
	public boolean ehOverlap(Shape s1, Shape s2) {
		return relate(s1, s2, "T*T***T**");
	}

	@Override
	public boolean ehCovers(Shape s1, Shape s2) {
		return relate(s1, s2, "T*TFT*FF*");
	}

	@Override
	public boolean ehCoveredBy(Shape s1, Shape s2) {
		return relate(s1, s2, "TFF*TFT**");
	}

	@Override
	public boolean ehInside(Shape s1, Shape s2) {
		return relate(s1, s2, "TFF*FFT**");
	}

	@Override
	public boolean ehContains(Shape s1, Shape s2) {
		return relate(s1, s2, "T*TFF*FF*");
	}

	@Override
	public boolean rcc8dc(Shape s1, Shape s2) {
		return relate(s1, s2, "FFTFFTTTT");
	}

	@Override
	public boolean rcc8ec(Shape s1, Shape s2) {
		return relate(s1, s2, "FFTFTTTTT");
	}

	@Override
	public boolean rcc8po(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTTTTTTT");
	}

	@Override
	public boolean rcc8tppi(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTFTTFFT");
	}

	@Override
	public boolean rcc8tpp(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFTTFTTT");
	}

	@Override
	public boolean rcc8ntpp(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFTFFTTT");
	}

	@Override
	public boolean rcc8ntppi(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTFFTFFT");
	}

	@Override
	public boolean rcc8eq(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFFTFFFT");
	}

}
