/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import org.locationtech.spatial4j.shape.Shape;

/**
 * The GeoSPARQL {@link Function} geof:boundary, as defined in
 * <a href="http://www.opengeospatial.org/standards/geosparql">OGC GeoSPARQL - A Geographic Query Language for
 * RDF Data</a>.
 */
public class Boundary extends GeometricUnaryFunction {

	@Override
	public String getURI() {
		return GEOF.BOUNDARY.stringValue();
	}

	@Override
	protected Shape operation(Shape s) {
		return SpatialSupport.getSpatialAlgebra().boundary(s);
	}
}
