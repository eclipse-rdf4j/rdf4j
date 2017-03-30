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
 * The GeoSPARQL {@link Function} geof:sfTouches, as defined in
 * <a href="http://www.opengeospatial.org/standards/geosparql">OGC GeoSPARQL - A Geographic Query Language for
 * RDF Data</a>.
 */
public class SfTouches extends GeometricRelationFunction {

	@Override
	public String getURI() {
		return GEOF.SF_TOUCHES.stringValue();
	}

	@Override
	protected boolean relation(Shape s1, Shape s2) {
		return SpatialSupport.getSpatialAlgebra().sfTouches(s1, s2);
	}
}
