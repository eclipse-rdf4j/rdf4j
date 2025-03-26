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
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

public class LuceneGeoTest {
	@Test
	public void geoFailTest() {
		MemoryStore store = new MemoryStore();
		LuceneSail lucene = new LuceneSail();
		lucene.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
		lucene.setParameter(LuceneSail.WKT_FIELDS, "https://example.org/#location");
		lucene.setBaseSail(store);
		SailRepository repo = new SailRepository(lucene);
		try {
			repo.init();

			Repositories.consume(repo, conn -> {
				try {
					conn.add(new StringReader(
							"@prefix ex: <https://example.org/#> ."
									// point in
									+ "ex:s ex:location \"POINT(9.6929555 45.6762274)\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> ."
					// point out
									+ "ex:s ex:location \"POINT(9.18457 45.466873)\"^^<http://www.opengis.net/ont/geosparql#wktLiteral> ."
					), "https://example.org/#", RDFFormat.TURTLE);
				} catch (IOException e) {
					throw new AssertionError(e);
				}
			});

			lucene.reindex();

			// a random polygon of Milan
			// POLYGON((9.000892639160158 45.3796432523812,9.381294250488283 45.3796432523812,9.381294250488283
			// 45.55420812072298,9.000892639160158 45.55420812072298,9.000892639160158 45.3796432523812))
			Repositories.consumeNoTransaction(repo, conn -> {
				try (TupleQueryResult res = conn.prepareTupleQuery(
						"PREFIX ex: <https://example.org/#> "
								+ "SELECT * { "
								+ "  ?s ex:location ?loc "
								+ "  FILTER (<http://www.opengis.net/def/function/geosparql/ehContains>(\"POLYGON((9.000892639160158 45.3796432523812,9.381294250488283 45.3796432523812,9.381294250488283 45.55420812072298,9.000892639160158 45.55420812072298,9.000892639160158 45.3796432523812))\"^^<http://www.opengis.net/ont/geosparql#wktLiteral>, ?loc)) "
								+ "} "
				).evaluate()) {
					assertTrue(res.hasNext(), "missing good value");
					BindingSet next = res.next();
					assertEquals(Values.iri("https://example.org/#s"), next.getValue("s"));
					assertEquals(Values.literal("POINT(9.18457 45.466873)", CoreDatatype.GEO.WKT_LITERAL),
							next.getValue("loc"));
					assertFalse(res.hasNext(), "more value(s) :"
							+ res.stream().map(Object::toString).collect(Collectors.joining("\n", "\n", "")));
				}
			});

		} finally {
			repo.shutDown();
		}

	}
}
