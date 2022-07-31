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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

/**
 * The GeoSPARQL {@link Function} geof:distance, as defined in
 * <a href="http://www.opengeospatial.org/standards/geosparql">OGC GeoSPARQL - A Geographic Query Language for RDF
 * Data</a>.
 */
public class Distance implements Function {

	@Override
	public String getURI() {
		return GEOF.DISTANCE.stringValue();
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		if (args.length != 3) {
			throw new ValueExprEvaluationException(getURI() + " requires exactly 3 arguments, got " + args.length);
		}

		SpatialContext geoContext = SpatialSupport.getSpatialContext();
		Point p1 = FunctionArguments.getPoint(this, args[0], geoContext);
		Point p2 = FunctionArguments.getPoint(this, args[1], geoContext);
		IRI units = FunctionArguments.getUnits(this, args[2]);

		double distDegs = geoContext.calcDistance(p1, p2);
		double distUom = FunctionArguments.convertFromDegrees(distDegs, units);

		return valueFactory.createLiteral(distUom);
	}
}
