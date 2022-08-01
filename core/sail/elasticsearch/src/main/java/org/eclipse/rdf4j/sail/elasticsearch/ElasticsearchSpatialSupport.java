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
package org.eclipse.rdf4j.sail.elasticsearch;

import java.util.Map;

import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.locationtech.spatial4j.shape.Shape;

/**
 * This class will try to load a subclass of itself called
 * "org.eclipse.rdf4j.sail.elasticsearch.ElasticsearchSpatialSupportInitializer". This is not provided, and is primarily
 * intended as a way to inject JTS support. If this fails a fall-back is used that doesn't support any shapes.
 */
abstract class ElasticsearchSpatialSupport {

	private static final ElasticsearchSpatialSupport support;

	static {
		ElasticsearchSpatialSupport spatialSupport;
		try {
			Class<?> cls = Class.forName("org.eclipse.rdf4j.sail.elasticsearch.ElasticsearchSpatialSupportInitializer",
					true, Thread.currentThread().getContextClassLoader());
			spatialSupport = (ElasticsearchSpatialSupport) cls.newInstance();
		} catch (Exception e) {
			spatialSupport = new DefaultElasticsearchSpatialSupport();
		}
		support = spatialSupport;
	}

	static ElasticsearchSpatialSupport getSpatialSupport() {
		return support;
	}

	protected abstract ShapeBuilder toShapeBuilder(Shape s);

	protected abstract Map<String, Object> toGeoJSON(Shape s);

	private static final class DefaultElasticsearchSpatialSupport extends ElasticsearchSpatialSupport {

		@Override
		protected ShapeBuilder toShapeBuilder(Shape s) {
			throw new UnsupportedOperationException(
					"This shape is not supported due to licensing issues. Feel free to provide your own implementation by using something like JTS: "
							+ s.getClass().getName());
		}

		@Override
		protected Map<String, Object> toGeoJSON(Shape s) {
			throw new UnsupportedOperationException(
					"This shape is not supported due to licensing issues. Feel free to provide your own implementation by using something like JTS: "
							+ s.getClass().getName());
		}
	}
}
