/**
 * ******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * ******************************************************************************
 */
package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reproduces the scenario described in the issue: a GRAPH clause containing an UNDEF binding should not cause a
 * NullPointerException during evaluation when joined with another pattern. The query should evaluate without error and
 * produce one solution binding for mappingProp.
 */
public class MemoryStoreUndefBindingQueryTest {

	private SailRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new SailRepository(new MemoryStore());
		repository.init();
	}

	@AfterEach
	public void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	public void testGraphBindUndefDoesNotThrowAndBindsMappingProp() {
		try (SailRepositoryConnection conn = repository.getConnection()) {
			SimpleNamespace NS1 = new SimpleNamespace("ex1", "http://example.org/");
			SimpleNamespace NS2 = new SimpleNamespace("ex2", "http://example.org/2/");

			// Add a statement so that the named graph exists
			conn.add(Values.iri(NS1, "A1"), OWL.EQUIVALENTCLASS, Values.iri(NS2, "A2"),
					Values.iri("http://example.org/"));

			String sparql = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
					+ "SELECT * {\n"
					+ "  ?mappingProp rdfs:subPropertyOf* owl:equivalentClass .\n"
					+ "  GRAPH <http://example.org/> {\n"
					+ "    BIND(?unbound as ?myVar)\n"
					+ "    # Also reproduces with: VALUES(?myVar) { (UNDEF) }\n"
					+ "  }\n"
					+ "}";

			var query = conn.prepareTupleQuery(sparql);

			int count = 0;
			try (var res = query.evaluate()) {
				while (res.hasNext()) {
					BindingSet bs = res.next();
					assertTrue(bs.hasBinding("mappingProp"), "Expected mappingProp binding");
					assertEquals(OWL.EQUIVALENTCLASS, bs.getValue("mappingProp"));
					count++;
				}
			}
			assertEquals(1, count, "Expected exactly one result row");
		}
	}
}
