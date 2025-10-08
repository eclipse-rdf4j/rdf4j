/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

class ElasticsearchSpatialSupportTest {

	@Test
	void defaultSpatialSupportThrowsForShapes() {
		ElasticsearchSpatialSupport support = invokeGetSpatialSupport();
		assertNotNull(support);

		Shape s = SpatialContext.GEO.makePoint(1, 2);
		assertTrue(s instanceof Point);

		assertThrows(UnsupportedOperationException.class, () -> callToGeoJSON(support, s));
		assertThrows(UnsupportedOperationException.class, () -> callToShapeBuilder(support, s));
	}

	// Access package-private/static methods via helpers in same package
	private static ElasticsearchSpatialSupport invokeGetSpatialSupport() {
		return ElasticsearchSpatialSupport.getSpatialSupport();
	}

	private static void callToShapeBuilder(ElasticsearchSpatialSupport support, Shape s) {
		support.toShapeBuilder(s);
	}

	private static void callToGeoJSON(ElasticsearchSpatialSupport support, Shape s) {
		support.toGeoJSON(s);
	}
}
