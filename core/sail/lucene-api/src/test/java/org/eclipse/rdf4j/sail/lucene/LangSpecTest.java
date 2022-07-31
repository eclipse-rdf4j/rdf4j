/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;

public class LangSpecTest {
	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	protected static class SearchIndexImpl extends AbstractSearchIndex {

		@Override
		protected SpatialContext getSpatialContext(String property) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected SearchDocument getDocument(String id) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected Iterable<? extends SearchDocument> getDocuments(String resourceId) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected SearchDocument newDocument(String id, String resourceId, String context) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected SearchDocument copyDocument(SearchDocument doc) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected void addDocument(SearchDocument doc) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected void updateDocument(SearchDocument doc) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected void deleteDocument(SearchDocument doc) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected Iterable<? extends DocumentScore> query(Resource subject, String q, IRI property, boolean highlight) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected Iterable<? extends DocumentDistance> geoQuery(IRI geoProperty, Point p, IRI units, double distance,
				String distanceVar, Var context) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty, String wkt,
				Var context) {
			throw new RuntimeException("not implemented");
		}

		@Override
		protected BulkUpdater newBulkUpdate() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void shutDown() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void begin() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void commit() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void rollback() {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void clearContexts(Resource... contexts) {
			throw new RuntimeException("not implemented");
		}

		@Override
		public void clear() {
			throw new RuntimeException("not implemented");
		}
	}

	SearchIndex index;
	Properties prop;

	@Before
	public void setup() {
		index = new SearchIndexImpl();
		prop = new Properties();
	}

	@Test
	public void noLangTest() throws Exception {
		index.initialize(prop);

		// test that without setting the LuceneSail.INDEXEDLANG property, the accept method is still
		// working as intended
		Assert.assertTrue(index.accept(VF.createLiteral("my literal")));
		Assert.assertTrue(index.accept(VF.createLiteral("my literal", "en")));
		Assert.assertTrue(index.accept(VF.createLiteral("mon litteral", "fr")));
	}

	@Test
	public void langTest() throws Exception {
		prop.setProperty(LuceneSail.INDEXEDLANG, "fr");
		index.initialize(prop);

		// test that with the LuceneSail.INDEXEDLANG property, we are only accepting literals of the right language
		Assert.assertFalse(index.accept(VF.createLiteral("my literal")));
		Assert.assertFalse(index.accept(VF.createLiteral("my literal", "en")));
		Assert.assertTrue(index.accept(VF.createLiteral("mon litteral", "fr")));
	}

	@Test
	public void multipleLangTest() throws Exception {
		prop.setProperty(LuceneSail.INDEXEDLANG, "fr  en");
		index.initialize(prop);

		// test that with the LuceneSail.INDEXEDLANG property, we are only accepting literals of the right languages
		Assert.assertFalse(index.accept(VF.createLiteral("my literal")));
		Assert.assertTrue(index.accept(VF.createLiteral("my literal", "en")));
		Assert.assertTrue(index.accept(VF.createLiteral("mon litteral", "fr")));
		Assert.assertFalse(index.accept(VF.createLiteral("mi literal", "es")));
	}
}
