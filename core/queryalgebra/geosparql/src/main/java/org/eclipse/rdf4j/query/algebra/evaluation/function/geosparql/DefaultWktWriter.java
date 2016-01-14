/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.io.IOException;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.ShapeCollection;
import com.spatial4j.core.shape.impl.BufferedLineString;

final class DefaultWktWriter implements WktWriter {

	private String notSupported(Shape s) {
		throw new UnsupportedOperationException(
				"This shape is not supported due to licensing issues. Feel free to provide your own implementation by using something like JTS: "
						+ s.getClass().getName());
	}

	@Override
	public String toWkt(Shape shape)
		throws IOException
	{
		if (shape instanceof Point) {
			Point p = (Point)shape;
			return "POINT " + toCoords(p);
		}
		else if (shape instanceof ShapeCollection<?>) {
			ShapeCollection<?> col = (ShapeCollection<?>)shape;
			if (col.isEmpty()) {
				return "GEOMETRYCOLLECTION EMPTY";
			}
			Class<?> elementType = null;
			StringBuilder buf = new StringBuilder(" (");
			String sep = "";
			for (Shape s : col) {
				if (elementType == null) {
					elementType = s.getClass();
				}
				else if (!elementType.equals(s.getClass())) {
					elementType = Shape.class;
				}
				buf.append(sep).append(toCoords(s));
				sep = ", ";
			}
			buf.append(")");
			if (Point.class.isAssignableFrom(elementType)) {
				buf.insert(0, "MULTIPOINT");
			}
			else if (elementType == Shape.class) {
				buf.insert(0, "GEOMETRYCOLLECTION");
			}
			else {
				return notSupported(shape);
			}
			return buf.toString();
		}
		else if (shape instanceof BufferedLineString) {
			BufferedLineString ls = (BufferedLineString)shape;
			return "LINESTRING " + toCoords(ls);
		}
		return notSupported(shape);
	}

	private String toCoords(Shape shape)
		throws IOException
	{
		if (shape instanceof Point) {
			Point p = (Point)shape;
			return toCoords(p);
		}
		else if (shape instanceof BufferedLineString) {
			BufferedLineString ls = (BufferedLineString)shape;
			return toCoords(ls);
		}
		return notSupported(shape);
	}

	private String toCoords(Point p)
		throws IOException
	{
		if (p.isEmpty()) {
			return "EMPTY";
		}
		else {
			return "(" + p.getX() + " " + p.getY() + ")";
		}
	}

	private String toCoords(BufferedLineString shape)
		throws IOException
	{
		double buffer = shape.getBuf();
		if (buffer != 0.0) {
			return notSupported(shape);
		}
		StringBuilder buf = new StringBuilder("(");
		String sep = "";
		for (Point p : shape.getPoints()) {
			buf.append(sep);
			buf.append(p.getX()).append(" ").append(p.getY());
			sep = ", ";
		}
		buf.append(")");
		return buf.toString();
	}
}