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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reproduces a corrupt ValueStore by tampering with the values.dat type byte and verifies that reading statements fails
 * when soft-fail is disabled.
 */
public class NativeStoreValueStoreCorruptionReproductionTest {

	@TempDir
	File tempFolder;

	private SailRepository repo;

	private File dataDir;

	private final ValueFactory F = SimpleValueFactory.getInstance();

	@BeforeEach
	public void setup() {
		dataDir = new File(tempFolder, "dbmodel");
		dataDir.mkdir();
		repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();

		// Insert the same base dataset used by NativeSailStoreCorruptionTest to ensure stable file layout
		IRI CTX_1 = F.createIRI("urn:one");
		IRI CTX_2 = F.createIRI("urn:two");

		Statement S0 = F.createStatement(F.createIRI("http://example.org/a0"), RDFS.LABEL, F.createLiteral("zero"));
		Statement S1 = F.createStatement(F.createIRI("http://example.org/b1"), RDFS.LABEL, F.createLiteral("one"));
		Statement S2 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL, F.createLiteral("two"));
		Statement S3 = F.createStatement(Values.bnode(), RDF.TYPE, Values.bnode());
		Statement S4 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL,
				F.createLiteral("two", "en"));
		Statement S5 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL, F.createLiteral(1.2));

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(S0);
			conn.add(S1, CTX_1);
			conn.add(S2, CTX_2);
			conn.add(S2, CTX_2);
			conn.add(S3, CTX_2);
			conn.add(S4, CTX_2);
			conn.add(S5, CTX_2);
		}
	}

	@AfterEach
	public void tearDown() {
		repo.shutDown();
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	public void corruptValuesDatInvalidTypeShouldBreakReads() throws IOException {
		// Disable soft-fail to surface corruption as an exception
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;

		// Close repo to release files for mutation
		repo.shutDown();

		// Flip a byte in values.dat at a position that maps to a value type marker
		// This offset mirrors NativeSailStoreCorruptionTest.testCorruptValuesDatFileInvalidTypeError
		File valuesFile = new File(dataDir, "values.dat");
		overwriteByte(valuesFile, 174, 0x0);

		// Reopen; attempting to read statements should now throw a RepositoryException
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			assertThrows(RepositoryException.class, () -> {
				conn.getStatements(null, null, null, false).forEachRemaining(s -> {
					// Force materialization of all statements
				});
			});
		}
	}

	private static void overwriteByte(File file, long pos, int newVal) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			long fileLength = raf.length();
			if (pos >= fileLength) {
				throw new IOException(
						"Attempt to write outside the existing file bounds: " + pos + " >= " + fileLength);
			}
			raf.seek(pos);
			raf.writeByte(newVal);
		}
	}
}
