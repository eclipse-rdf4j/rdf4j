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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;

class ElasticsearchIndexUtilTest {

	@Test
	void encodeDecodeFieldNameRoundtrip() {
		String original = "ex.name.with.dots";
		String encoded = ElasticsearchIndex.encodeFieldName(original);
		assertEquals("ex^name^with^dots", encoded);
		String decoded = ElasticsearchIndex.decodeFieldName(encoded);
		assertEquals(original, decoded);
	}

	@Test
	void propertyAndGeoFieldNames() {
		String prop = "ex.name";
		String field = ElasticsearchIndex.toPropertyFieldName(prop);
		assertTrue(field.startsWith(ElasticsearchIndex.PROPERTY_FIELD_PREFIX));
		assertEquals(prop, ElasticsearchIndex.toPropertyName(field));

		String gp = ElasticsearchIndex.toGeoPointFieldName(prop);
		assertTrue(gp.startsWith(ElasticsearchIndex.GEOPOINT_FIELD_PREFIX));
		String gs = ElasticsearchIndex.toGeoShapeFieldName(prop);
		assertTrue(gs.startsWith(ElasticsearchIndex.GEOSHAPE_FIELD_PREFIX));
	}

	@Test
	void createSpatialContextMapperReturnsConstant() {
		class Exposed extends ElasticsearchIndex {
			public java.util.function.Function<String, SpatialContext> expose(Map<String, String> p) {
				return (java.util.function.Function<String, SpatialContext>) createSpatialContextMapper(p);
			}
		}

		Exposed idx = new Exposed();
		Map<String, String> params = new HashMap<>();
		var fn = idx.expose(params);
		assertNotNull(fn);
		SpatialContext a = fn.apply("a");
		SpatialContext b = fn.apply("b");
		assertSame(a, b, "mapper should be constant regardless of input property name");
	}

	@Test
	void searchNumDocsGuard() {
		ElasticsearchIndex idx = new ElasticsearchIndex();
		assertThrows(IllegalArgumentException.class, () -> idx.search(null, null, -2));
	}
}
