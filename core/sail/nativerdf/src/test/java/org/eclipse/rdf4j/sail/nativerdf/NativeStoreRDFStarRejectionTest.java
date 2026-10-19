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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reproduces a reported issue where attempting to add Turtle-star data to a NativeStore throws, and subsequently the
 * repository becomes unusable for normal operations. After the rejection, the repository should remain usable.
 */
public class NativeStoreRDFStarRejectionTest {

	@TempDir
	public File dataDir;

	@Test
	public void nativeStoreRejectsTurtleStarButRemainsUsable() {
		String data = "@prefix ex: <http://example.org/> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"# Basic triple\n" +
				"ex:JohnDoe ex:worksAt ex:CompanyX .\n" +
				"\n" +
				"# RDF* triple (unsupported by NativeStore)\n" +
				"<<ex:JohnDoe ex:worksAt ex:CompanyX>> ex:since \"2022-01-01\"^^xsd:date .\n";

		Repository repo = new SailRepository(new NativeStore(dataDir));

		// First: attempt to add data that includes RDF*-star. Expect an exception (rejection).
		try (RepositoryConnection conn = repo.getConnection()) {
			try {
				conn.add(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), null, RDFFormat.TURTLE);
			} catch (Exception expected) {
				// Expected: Turtle-star should be rejected by NativeStore
			}
		}

		// Then: repository should still be usable. Getting statements must not throw.
		assertDoesNotThrow(() -> {
			try (RepositoryConnection conn = repo.getConnection();
					RepositoryResult<Statement> result = conn.getStatements(null, null, null)) {
				// iterate to fully exercise the result set
				while (result.hasNext()) {
					result.next();
				}
			}
		}, "Repository became unusable after rejecting Turtle-star input");
	}

	@Test
	public void nativeStoreRejectsTurtleStarButRemainsUsableSnapshot() {
		String data = "@prefix ex: <http://example.org/> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
				"\n" +
				"# Basic triple\n" +
				"ex:JohnDoe ex:worksAt ex:CompanyX .\n" +
				"\n" +
				"# RDF* triple (unsupported by NativeStore)\n" +
				"<<ex:JohnDoe ex:worksAt ex:CompanyX>> ex:since \"2022-01-01\"^^xsd:date .\n";

		Repository repo = new SailRepository(new NativeStore(dataDir));

		// First: attempt to add data that includes RDF*-star. Expect an exception (rejection).
		try (RepositoryConnection conn = repo.getConnection()) {
			try {
				conn.begin(IsolationLevels.SNAPSHOT);
				conn.add(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), null, RDFFormat.TURTLE);
				conn.commit();
			} catch (Exception expected) {
				// Expected: Turtle-star should be rejected by NativeStore
			}
		}

		// Then: repository should still be usable. Getting statements must not throw.
		assertDoesNotThrow(() -> {
			try (RepositoryConnection conn = repo.getConnection();
					RepositoryResult<Statement> result = conn.getStatements(null, null, null)) {
				// iterate to fully exercise the result set
				while (result.hasNext()) {
					result.next();
				}
			}
		}, "Repository became unusable after rejecting Turtle-star input");
	}
}
