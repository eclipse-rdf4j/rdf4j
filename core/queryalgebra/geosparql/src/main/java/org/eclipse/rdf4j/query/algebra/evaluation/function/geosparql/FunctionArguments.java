/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.text.ParseException;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.URI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

class FunctionArguments {

	private FunctionArguments() {
	}

	public static double getDouble(Function func, Value v)
		throws ValueExprEvaluationException
	{
		Literal l = getLiteral(func, v, XMLSchema.DOUBLE);
		return l.doubleValue();
	}

	public static String getString(Function func, Value v)
		throws ValueExprEvaluationException
	{
		Literal l = getLiteral(func, v, XMLSchema.STRING);
		return l.stringValue();
	}

	public static Shape getShape(Function func, Value v, SpatialContext context)
		throws ValueExprEvaluationException
	{
		Literal wktLiteral = getLiteral(func, v, GEO.WKT_LITERAL);
		try {
			return context.readShapeFromWkt(wktLiteral.getLabel());
		}
		catch (ParseException e) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + wktLiteral,
					e);
		}
	}

	public static Point getPoint(Function func, Value v, SpatialContext geoContext)
		throws ValueExprEvaluationException
	{
		Shape p = FunctionArguments.getShape(func, v, geoContext);
		if (!(p instanceof Point)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + " (not a point): "
					+ v);
		}
		return (Point)p;
	}

	public static Literal getLiteral(Function func, Value v, URI expectedDatatype)
		throws ValueExprEvaluationException
	{
		if (!(v instanceof Literal)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + v);
		}
		Literal lit = (Literal)v;
		if (!expectedDatatype.equals(lit.getDatatype())) {
			throw new ValueExprEvaluationException("Invalid datatype " + lit.getDatatype() + " for "
					+ func.getURI() + ": " + v);
		}
		return lit;
	}

	public static URI getUnits(Function func, Value v)
		throws ValueExprEvaluationException
	{
		if (!(v instanceof URI)) {
			throw new ValueExprEvaluationException("Invalid argument for " + func.getURI() + ": " + v);
		}
		URI unitUri = (URI)v;
		if (!unitUri.getNamespace().equals(GEOF.UOM_NAMESPACE)) {
			throw new ValueExprEvaluationException("Invalid unit of measurement URI for " + func.getURI() + ": "
					+ v);
		}
		return unitUri;
	}

	public static double convertFromDegrees(double degs, URI units)
		throws ValueExprEvaluationException
	{
		double v;
		if (GEOF.UOM_DEGREE.equals(units)) {
			v = degs;
		}
		else if (GEOF.UOM_RADIAN.equals(units)) {
			v = DistanceUtils.toRadians(degs);
		}
		else if (GEOF.UOM_UNITY.equals(units)) {
			v = degs / 180.0;
		}
		else if (GEOF.UOM_METRE.equals(units)) {
			v = DistanceUtils.degrees2Dist(degs, DistanceUtils.EARTH_MEAN_RADIUS_KM) * 1000.0;
		}
		else {
			throw new ValueExprEvaluationException("Invalid unit of measurement: " + units);
		}
		return v;
	}

	public static double convertToDegrees(double v, URI units)
		throws ValueExprEvaluationException
	{
		double degs;
		if (GEOF.UOM_DEGREE.equals(units)) {
			degs = v;
		}
		else if (GEOF.UOM_RADIAN.equals(units)) {
			degs = DistanceUtils.toDegrees(v);
		}
		else if (GEOF.UOM_UNITY.equals(units)) {
			degs = v * 180.0;
		}
		else if (GEOF.UOM_METRE.equals(units)) {
			degs = DistanceUtils.dist2Degrees(v / 1000.0, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		}
		else {
			throw new ValueExprEvaluationException("Invalid unit of measurement: " + units);
		}
		return degs;
	}
}
