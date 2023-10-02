/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DirectTypeHierarchyInferencerTest {

	@BeforeAll
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterAll
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	@Test
	public void testDirectTypeHierarchyInference() throws IOException {
		SailRepository rep = new SailRepository(new DirectTypeHierarchyInferencer(new MemoryStore()));
		try {
			rep.init();

			Model inputModel = Rio.parse(this.getClass().getResourceAsStream("direct-type-hierarchy-test-in.nt"),
					RDFFormat.NTRIPLES);
			Model expectedEntailedStatements = Rio.parse(
					this.getClass().getResourceAsStream("direct-type-hierarchy-test-out.nt"),
					RDFFormat.NTRIPLES);

			try (RepositoryConnection con = rep.getConnection()) {
				con.begin();
				con.add(inputModel);
				con.commit();
			}

			Model entailedStatements;
			try (RepositoryConnection con = rep.getConnection()) {
				entailedStatements = QueryResults.asModel(con.getStatements(null, null, null,
						true));
				entailedStatements.removeAll(inputModel);
			}

			assertThat(entailedStatements).hasSameElementsAs(expectedEntailedStatements);

		} finally {
			rep.shutDown();
		}
	}
}
