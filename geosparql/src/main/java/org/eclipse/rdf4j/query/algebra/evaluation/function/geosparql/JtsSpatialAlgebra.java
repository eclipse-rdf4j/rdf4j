/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsShapeFactory;

/**
 * JTS-enabled implementation of spatial algebra, with full support for polygon-related geospatial functions
 * 
 */
public class JtsSpatialAlgebra implements SpatialAlgebra {

	private final JtsShapeFactory shapeFactory;

	public JtsSpatialAlgebra(JtsSpatialContext context) {
		this.shapeFactory = context.getShapeFactory();
	}

	public Shape convexHull(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).convexHull());
	}

	public Shape boundary(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).getBoundary());
	}

	public Shape envelope(Shape s) {
		return shapeFactory.makeShapeFromGeometry(shapeFactory.getGeometryFrom(s).getEnvelope());
	}

	public Shape union(Shape s1, Shape s2) {
		return shapeFactory.makeShapeFromGeometry(
				shapeFactory.getGeometryFrom(s1).union(shapeFactory.getGeometryFrom(s2)));
	}

	public Shape intersection(Shape s1, Shape s2) {
		Geometry intersection = shapeFactory.getGeometryFrom(s1).intersection(shapeFactory.getGeometryFrom(s2));
		if (intersection.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(intersection);
	}

	public Shape symDifference(Shape s1, Shape s2) {
		Geometry symDiff = shapeFactory.getGeometryFrom(s1).symDifference(shapeFactory.getGeometryFrom(s2));
		if (symDiff.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(symDiff);
	}

	public Shape difference(Shape s1, Shape s2) {
		Geometry difference = shapeFactory.getGeometryFrom(s1).difference(shapeFactory.getGeometryFrom(s2));
		if (difference.isEmpty()) {
			return shapeFactory.pointXY(Double.NaN, Double.NaN);
		}
		return shapeFactory.makeShapeFromGeometry(difference);
	}

	public boolean relate(Shape s1, Shape s2, String intersectionPattern) {
		return shapeFactory.getGeometryFrom(s1).relate(shapeFactory.getGeometryFrom(s2), intersectionPattern);
	}

	public boolean equals(Shape s1, Shape s2) {
		return shapeFactory.getGeometryFrom(s1).equalsNorm(shapeFactory.getGeometryFrom(s2));
	}

	public boolean sfDisjoint(Shape s1, Shape s2) {
		return relate(s1, s2, "FF*FF****");
	}

	public boolean sfIntersects(Shape s1, Shape s2) {
		return relate(s1, s2, "T********") || relate(s1, s2, "*T*******") || relate(s1, s2, "***T*****")
				|| relate(s1, s2, "****T****");
	}

	public boolean sfTouches(Shape s1, Shape s2) {
		return relate(s1, s2, "FT*******") || relate(s1, s2, "F**T*****") || relate(s1, s2, "F***T****");
	}

	public boolean sfCrosses(Shape s1, Shape s2) {
		Geometry g1 = shapeFactory.getGeometryFrom(s1);
		Geometry g2 = shapeFactory.getGeometryFrom(s2);
		int d1 = g1.getDimension();
		int d2 = g2.getDimension();
		if ((d1 == 0 && d2 == 1) || (d1 == 0 && d2 == 2) || (d1 == 1 && d2 == 2)) {
			return g1.relate(g2, "T*T***T**");
		}
		else if (d1 == 1 && d2 == 1) {
			return g1.relate(g2, "0*T***T**");
		}
		else {
			return false;
		}
	}

	public boolean sfWithin(Shape s1, Shape s2) {
		return relate(s1, s2, "T*F**F***");
	}

	public boolean sfContains(Shape s1, Shape s2) {
		return relate(s1, s2, "T*****FF*");
	}

	public boolean sfOverlaps(Shape s1, Shape s2) {
		Geometry g1 = shapeFactory.getGeometryFrom(s1);
		Geometry g2 = shapeFactory.getGeometryFrom(s2);
		int d1 = g1.getDimension();
		int d2 = g2.getDimension();
		if ((d1 == 2 && d2 == 2) || (d1 == 0 && d2 == 0)) {
			return g1.relate(g2, "T*T***T**");
		}
		else if (d1 == 1 && d2 == 1) {
			return g1.relate(g2, "1*T***T**");
		}
		else {
			return false;
		}
	}

	public boolean ehDisjoint(Shape s1, Shape s2) {
		return relate(s1, s2, "FF*FF****");
	}

	public boolean ehMeet(Shape s1, Shape s2) {
		return relate(s1, s2, "FT*******") || relate(s1, s2, "F**T*****") || relate(s1, s2, "F***T****");
	}

	public boolean ehOverlap(Shape s1, Shape s2) {
		return relate(s1, s2, "T*T***T**");
	}

	public boolean ehCovers(Shape s1, Shape s2) {
		return relate(s1, s2, "T*TFT*FF*");
	}

	public boolean ehCoveredBy(Shape s1, Shape s2) {
		return relate(s1, s2, "TFF*TFT**");
	}

	public boolean ehInside(Shape s1, Shape s2) {
		return relate(s1, s2, "TFF*FFT**");
	}

	public boolean ehContains(Shape s1, Shape s2) {
		return relate(s1, s2, "T*TFF*FF*");
	}

	public boolean rcc8dc(Shape s1, Shape s2) {
		return relate(s1, s2, "FFTFFTTTT");
	}

	public boolean rcc8ec(Shape s1, Shape s2) {
		return relate(s1, s2, "FFTFTTTTT");
	}

	public boolean rcc8po(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTTTTTTT");
	}

	public boolean rcc8tppi(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTFTTFFT");
	}

	public boolean rcc8tpp(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFTTFTTT");
	}

	public boolean rcc8ntpp(Shape s1, Shape s2) {
		return relate(s1, s2, "TFFTFFTTT");
	}

	public boolean rcc8ntppi(Shape s1, Shape s2) {
		return relate(s1, s2, "TTTFFTFFT");
	}

}
