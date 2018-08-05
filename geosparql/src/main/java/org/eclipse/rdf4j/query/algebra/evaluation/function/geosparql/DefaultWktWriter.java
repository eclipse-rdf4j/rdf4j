/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.function.geosparql;

import java.io.IOException;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.ShapeIO;
import org.locationtech.spatial4j.io.ShapeWriter;
import org.locationtech.spatial4j.shape.Shape;

final class DefaultWktWriter implements WktWriter {
	
	private final ShapeWriter wktWriter;

	public DefaultWktWriter(SpatialContext context) {
		wktWriter = context.getFormats().getWriter(ShapeIO.WKT);
	}

	@Override
	public String toWkt(Shape shape)
		throws IOException
	{
		return wktWriter.toString(shape);
	}
}