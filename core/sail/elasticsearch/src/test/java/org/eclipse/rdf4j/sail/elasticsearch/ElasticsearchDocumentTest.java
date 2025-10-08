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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;

import com.google.common.base.Function;

class ElasticsearchDocumentTest {

	private static final Function<String, SpatialContext> GEO = (s) -> SpatialContext.GEO;

	@Test
	void constructorAndBasicGetters() {
		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id1", "resource", "index1", "urn:res", null, GEO);

		assertEquals("id1", doc.getId());
		assertEquals("resource", doc.getType());
		assertEquals("index1", doc.getIndex());
		assertEquals("urn:res", doc.getResource());
		assertNull(doc.getContext());
	}

	@Test
	void addPropertyNameOnly_thenDuplicateFails() {
		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id2", "resource", "index2", "urn:r", "urn:c", GEO);

		doc.addProperty("p");

		// Adding again should fail
		assertThrows(IllegalStateException.class, () -> doc.addProperty("p"));

		assertEquals("urn:c", doc.getContext());
		assertFalse(doc.hasProperty("p", "v"));
		assertNull(doc.getProperty("p"));
	}

	@Test
	void addPropertyWithText_multipleValuesAggregated() {
		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id3", "resource", "index", "urn:res", null, GEO);

		doc.addProperty("name", "Alice");
		doc.addProperty("name", "Bob");

		List<String> values = doc.getProperty("name");
		assertNotNull(values);
		assertEquals(2, values.size());
		assertTrue(values.contains("Alice"));
		assertTrue(values.contains("Bob"));

		assertTrue(doc.hasProperty("name", "Alice"));
		assertFalse(doc.hasProperty("name", "Carol"));
	}

	@Test
	void addGeoProperty_pointWkt_addsGeohash() {
		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id4", "resource", "index", "urn:res", null, GEO);

		// SpatialContext.GEO understands simple POINT WKT
		doc.addGeoProperty("loc", "POINT (1 2)");

		String geoPointKey = ElasticsearchIndex.toGeoPointFieldName("loc");
		Object geohash = doc.getSource().get(geoPointKey);
		assertNotNull(geohash, "geopoint geohash should be present for POINT WKT");
		assertTrue(geohash instanceof String);
	}

	@Test
	void addGeoProperty_invalidWkt_ignored() {
		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id5", "resource", "index", "urn:res", null, GEO);

		// Invalid WKT results in ParseException which must be ignored
		doc.addGeoProperty("loc", "NOT_A_WKT");

		assertNull(doc.getSource().get(ElasticsearchIndex.toGeoPointFieldName("loc")));
		assertNull(doc.getSource().get(ElasticsearchIndex.toGeoShapeFieldName("loc")));
	}

	@Test
	void getPropertyNamesFromFields() {
		Map<String, Object> backing = new HashMap<>();
		// add a few property fields (with encoding of '.')
		backing.put(ElasticsearchIndex.toPropertyFieldName("name"), "A");
		backing.put(ElasticsearchIndex.toPropertyFieldName("ex.name"), "B");
		// non-property fields should be ignored
		backing.put(SearchFields.URI_FIELD_NAME, "urn:res");
		backing.put(SearchFields.TEXT_FIELD_NAME, "text");
		backing.put(SearchFields.CONTEXT_FIELD_NAME, "urn:ctx");

		ElasticsearchDocument doc = new ElasticsearchDocument(
				"id6", "resource", "index", 1L, 1L, backing, GEO);

		Set<String> names = doc.getPropertyNames();
		assertEquals(new HashSet<>(Set.of("name", "ex.name")), names);
	}
}
