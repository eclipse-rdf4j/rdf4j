/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql.federation;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SparqlFederatedServiceIntegrationTest {

	@Test
	@Disabled("manual test to demonstrate the original issue of GH-5358")
	public void testValues_Wikidata() {
		Repository repo = new SailRepository(new MemoryStore());
		try (var conn = repo.getConnection()) {

			var tq = conn.prepareTupleQuery("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "PREFIX sh: <http://www.w3.org/ns/shacl#>\n"
					+ "SELECT * WHERE {\n"
					+ "  SERVICE <https://query.wikidata.org/sparql> {\n"
					+ "   VALUES ?resource {\n"
					+ "     <http://www.wikidata.org/entity/Q7455975>\n"
					+ "   }\n"
					+ "   ?resource <http://www.wikidata.org/prop/direct/P856> ?website\n"
					+ "  }\n"
					+ "}");

			try (var tqr = tq.evaluate()) {
				tqr.stream().forEach(bs -> System.out.println(bs));
			}
		}
	}
}
