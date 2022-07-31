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

import java.io.IOException;
import java.text.ParseException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.io.ShapeReader;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

class FunctionArguments {

	/**
	 * Empty constructor
	 */
	private FunctionArguments() {
	}

	/**
	 * Get the double value
	 *
	 * @param func function
	 * @param v    value
	 * @return double
	 * @throws ValueExprEvaluationException
	 */
	public static double getDouble(Function func, Value v) throws ValueExprEvaluationException {
		if (!(v instanceof Literal)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + v);
		}

		try {
			return ((Literal) v).doubleValue();
		} catch (NumberFormatException e) {
			throw new ValueExprEvaluationException(e);
		}

	}

	/**
	 * Get the string value
	 *
	 * @param func function
	 * @param v    value
	 * @return string
	 * @throws ValueExprEvaluationException
	 */
	public static String getString(Function func, Value v) throws ValueExprEvaluationException {
		Literal l = getLiteral(func, v, XSD.STRING);
		return l.stringValue();
	}

	/**
	 * Get the geo shape
	 *
	 * @param func    function
	 * @param v       value
	 * @param context
	 * @return shape
	 * @throws ValueExprEvaluationException
	 */
	public static Shape getShape(Function func, Value v, SpatialContext context) throws ValueExprEvaluationException {
		Literal wktLiteral = getLiteral(func, v, GEO.WKT_LITERAL);
		try {
			ShapeReader reader = context.getFormats().getWktReader();
			return reader.read(wktLiteral.getLabel());
		} catch (IOException | InvalidShapeException | ParseException e) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + wktLiteral, e);
		}
	}

	/**
	 * Get the geo point
	 *
	 * @param func       function
	 * @param v          value
	 * @param geoContext
	 * @return point
	 * @throws ValueExprEvaluationException
	 */
	public static Point getPoint(Function func, Value v, SpatialContext geoContext)
			throws ValueExprEvaluationException {
		Shape p = FunctionArguments.getShape(func, v, geoContext);
		if (!(p instanceof Point)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + " (not a point): " + v);
		}
		return (Point) p;
	}

	/**
	 * Get the literal of a specific data type
	 *
	 * @param func             function
	 * @param v                value
	 * @param expectedDatatype
	 * @return literal
	 * @throws ValueExprEvaluationException
	 */
	public static Literal getLiteral(Function func, Value v, IRI expectedDatatype) throws ValueExprEvaluationException {
		if (!(v instanceof Literal)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + v);
		}
		Literal lit = (Literal) v;
		if (!expectedDatatype.equals(lit.getDatatype())) {
			throw new ValueExprEvaluationException(
					"Invalid datatype " + lit.getDatatype() + " for " + func.getURI() + ": " + v);
		}
		return lit;
	}

	/**
	 * Get the UoM IRI of the unit
	 *
	 * @param func function
	 * @param v    value
	 * @return UoM IRI
	 * @throws ValueExprEvaluationException
	 */
	public static IRI getUnits(Function func, Value v) throws ValueExprEvaluationException {
		if (!(v instanceof IRI)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + v);
		}
		IRI unitUri = (IRI) v;
		if (!unitUri.getNamespace().equals(GEOF.UOM_NAMESPACE)) {
			throw new ValueExprEvaluationException("Invalid unit of measurement URI for " + func.getURI() + ": " + v);
		}
		return unitUri;
	}

	/**
	 * Convert degrees to another unit
	 *
	 * @param degs  degrees
	 * @param units UoM IRI of the unit to convert to
	 * @return converted value as a double
	 * @throws ValueExprEvaluationException
	 */
	public static double convertFromDegrees(double degs, IRI units) throws ValueExprEvaluationException {
		double v;

		if (GEOF.UOM_DEGREE.equals(units)) {
			v = degs;
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			v = DistanceUtils.toRadians(degs);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			v = degs / 180.0;
		} else if (GEOF.UOM_METRE.equals(units)) {
			v = DistanceUtils.degrees2Dist(degs, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 1000.0;
		} else {
			throw new ValueExprEvaluationException("Invalid unit of measurement: " + units);
		}
		return v;
	}

	/**
	 * Convert a value to degrees
	 *
	 * @param v     value
	 * @param units UoM IRI of the unit
	 * @return degrees as a double
	 * @throws ValueExprEvaluationException
	 */
	public static double convertToDegrees(double v, IRI units) throws ValueExprEvaluationException {
		double degs;

		if (GEOF.UOM_DEGREE.equals(units)) {
			degs = v;
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			degs = DistanceUtils.toDegrees(v);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			degs = v * 180.0;
		} else if (GEOF.UOM_METRE.equals(units)) {
			degs = DistanceUtils.dist2Degrees(v / 1000.0, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else {
			throw new ValueExprEvaluationException("Invalid unit of measurement: " + units);
		}
		return degs;
	}
}
