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
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.util.Arrays;
import java.util.Collections;

import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeCollection;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.impl.BufferedLineString;

/**
 * Default implementation of Spatial Algebra for use in situations where JTS support is not available.
 *
 * @deprecated use {@link JtsSpatialAlgebra} instead.
 */
@Deprecated
final class DefaultSpatialAlgebra implements SpatialAlgebra {

	private <T> T notSupported() {
		throw new UnsupportedOperationException(
				"Not supported due to licensing issues. Feel free to provide your own implementation by using something like JTS.");
	}

	private Shape createEmptyPoint() {
		return SpatialSupport.getSpatialContext().makePoint(Double.NaN, Double.NaN);
	}

	private Shape createEmptyGeometry() {
		return new ShapeCollection<>(Collections.<Shape>emptyList(), SpatialSupport.getSpatialContext());
	}

	@Override
	public Shape convexHull(Shape s) {
		if (s instanceof Point) {
			return s;
		} else if (s instanceof ShapeCollection<?>) {
			return new BufferedLineString((ShapeCollection<Point>) s, 0.0, SpatialSupport.getSpatialContext());
		}
		return notSupported();
	}

	@Override
	public Shape boundary(Shape s) {
		if (s instanceof Point) {
			// points have no boundary so return empty shape
			return createEmptyGeometry();
		} else if (s instanceof ShapeCollection<?>) {
			ShapeCollection<?> col = (ShapeCollection<?>) s;
			if (col.isEmpty()) {
				return createEmptyGeometry();
			}
			for (Shape p : col) {
				if (!(p instanceof Point)) {
					return notSupported();
				}
			}
			return createEmptyGeometry();
		}
		return notSupported();
	}

	@Override
	public Shape envelope(Shape s) {
		if (s instanceof Point) {
			return s;
		}
		return notSupported();
	}

	@Override
	public Shape union(Shape s1, Shape s2) {
		if (s1 instanceof Point && s2 instanceof Point) {
			Point p1 = (Point) s1;
			Point p2 = (Point) s2;
			int diff = compare(p2, p1);
			if (diff == 0) {
				return s1;
			} else if (diff < 0) {
				p1 = p2;
				p2 = (Point) s1;
			}
			return new ShapeCollection<>(Arrays.asList(p1, p2), SpatialSupport.getSpatialContext());
		}
		return notSupported();
	}

	private int compare(Point p1, Point p2) {
		int diff = Double.compare(p1.getX(), p2.getX());
		if (diff == 0) {
			diff = Double.compare(p1.getY(), p2.getY());
		}
		return diff;
	}

	@Override
	public Shape intersection(Shape s1, Shape s2) {
		if (s1 instanceof Point && s2 instanceof Point) {
			Point p1 = (Point) s1;
			Point p2 = (Point) s2;
			int diff = compare(p2, p1);
			if (diff == 0) {
				return s1;
			} else {
				return createEmptyPoint();
			}
		}
		return notSupported();
	}

	@Override
	public Shape symDifference(Shape s1, Shape s2) {
		if (s1 instanceof Point && s2 instanceof Point) {
			Point p1 = (Point) s1;
			Point p2 = (Point) s2;
			int diff = compare(p2, p1);
			if (diff == 0) {
				return createEmptyPoint();
			} else if (diff < 0) {
				p1 = p2;
				p2 = (Point) s1;
			}
			return new ShapeCollection<>(Arrays.asList(p1, p2), SpatialSupport.getSpatialContext());
		}
		return notSupported();
	}

	@Override
	public Shape difference(Shape s1, Shape s2) {
		if (s1 instanceof Point && s2 instanceof Point) {
			Point p1 = (Point) s1;
			Point p2 = (Point) s2;
			int diff = compare(p2, p1);
			if (diff == 0) {
				return createEmptyPoint();
			}
			return s1;
		}
		return notSupported();
	}

	@Override
	public boolean relate(Shape s1, Shape s2, String intersectionPattern) {
		return notSupported();
	}

	@Override
	public boolean sfEquals(Shape s1, Shape s2) {
		return s1.equals(s2);
	}

	@Override
	public boolean sfDisjoint(Shape s1, Shape s2) {
		return SpatialRelation.DISJOINT == s1.relate(s2);
	}

	@Override
	public boolean sfIntersects(Shape s1, Shape s2) {
		return SpatialRelation.INTERSECTS == s1.relate(s2);
	}

	@Override
	public boolean sfTouches(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean sfCrosses(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean sfWithin(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean sfContains(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean sfOverlaps(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean ehDisjoint(Shape s1, Shape s2) {
		return SpatialRelation.DISJOINT == s1.relate(s2);
	}

	@Override
	public boolean ehMeet(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean ehOverlap(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean ehCovers(Shape s1, Shape s2) {
		return SpatialRelation.CONTAINS == s1.relate(s2);
	}

	@Override
	public boolean ehCoveredBy(Shape s1, Shape s2) {
		return SpatialRelation.WITHIN == s1.relate(s2);
	}

	@Override
	public boolean ehInside(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean ehContains(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8dc(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8ec(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8po(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8tppi(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8tpp(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8ntpp(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8ntppi(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public Shape buffer(Shape s, double distance) {
		return s.getBuffered(distance, SpatialSupport.getSpatialContext());
	}

	@Override
	public boolean ehEquals(Shape s1, Shape s2) {
		return notSupported();
	}

	@Override
	public boolean rcc8eq(Shape s1, Shape s2) {
		return notSupported();
	}
}
