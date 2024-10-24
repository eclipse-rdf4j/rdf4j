/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests how the NativeStore handles corruption in the data files.
 */
public class NativeSailStoreCorruptionTest {

	private static final Logger logger = LoggerFactory.getLogger(NativeSailStoreCorruptionTest.class);

	@TempDir
	File tempFolder;

	protected Repository repo;

	protected final ValueFactory F = SimpleValueFactory.getInstance();

	private File dataDir;

	@BeforeEach
	public void before() throws IOException {
		this.dataDir = new File(tempFolder, "dbmodel");
		dataDir.mkdir();
		repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();

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
		backupFile(dataDir, "values.dat");
		backupFile(dataDir, "values.id");
		backupFile(dataDir, "values.hash");
		backupFile(dataDir, "namespaces.dat");
		backupFile(dataDir, "contexts.dat");
		backupFile(dataDir, "triples-posc.alloc");
		backupFile(dataDir, "triples-posc.dat");
		backupFile(dataDir, "triples-spoc.alloc");
		backupFile(dataDir, "triples-spoc.dat");

		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;

	}

	public static void overwriteByteInFile(File valuesFile, long pos, int newVal) throws IOException {

		// Use RandomAccessFile in "rw" mode to read and write to the file
		try (RandomAccessFile raf = new RandomAccessFile(valuesFile, "rw")) {
			// Get the length of the file
			long fileLength = raf.length();

			// Check if the position is within the file bounds
			if (pos >= fileLength) {
				throw new IOException(
						"Attempt to write outside the existing file bounds: " + pos + " >= " + fileLength);
			}

			// Move the file pointer to byte position 32
			raf.seek(pos);

			// Write the byte value 0x0 at the current position
			raf.writeByte(newVal);
		}
	}

	public static void backupFile(File dataDir, String s) throws IOException {
		File valuesFile = new File(dataDir, s);
		File backupFile = new File(dataDir, s + ".bak");

		if (!valuesFile.exists()) {
			throw new IOException(s + " does not exist and cannot be backed up.");
		}

		// Copy values.dat to values.dat.bak
		Files.copy(valuesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	public static void restoreFile(File dataDir, String s) throws IOException {
		File valuesFile = new File(dataDir, s);
		File backupFile = new File(dataDir, s + ".bak");

		if (!backupFile.exists()) {
			throw new IOException("Backup file " + s + ".bak does not exist.");
		}

		// Copy values.dat.bak back to values.dat
		Files.copy(backupFile.toPath(), valuesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	@Test
	public void testCorruptValuesDatFileNamespace() throws IOException {
		repo.shutDown();

		overwriteByteInFile(new File(dataDir, "values.dat"), 12, 0x0);

		repo.init();

		List<Statement> list = getStatements();
		assertEquals(6, list.size());
	}

	@Test
	public void testCorruptValuesDatFileNamespaceDatatype() throws IOException {
		repo.shutDown();

		overwriteByteInFile(new File(dataDir, "values.dat"), 96, 0x0);

		repo.init();

		List<Statement> list = getStatements();
		assertEquals(6, list.size());
	}

	@Test
	public void testCorruptValuesDatFileEmptyDataArrayError() throws IOException {
		repo.shutDown();

		overwriteByteInFile(new File(dataDir, "values.dat"), 173, 0x0);

		repo.init();

		List<Statement> list = getStatements();
		assertEquals(6, list.size());
	}

	@Test
	public void testCorruptValuesDatFileInvalidTypeError() throws IOException {
		repo.shutDown();

		overwriteByteInFile(new File(dataDir, "values.dat"), 174, 0x0);

		repo.init();

		List<Statement> list = getStatements();
		assertEquals(6, list.size());
	}

	@Test
	public void testCorruptValuesDatFileEntireValuesDatFile() throws IOException {
		for (int i = 4; i < 437; i++) {
			logger.debug("Corrupting byte at position " + i);
			repo.shutDown();
			restoreFile(dataDir, "values.dat");

			overwriteByteInFile(new File(dataDir, "values.dat"), i, 0x0);

			repo.init();

			List<Statement> list = getStatements();
			assertEquals(6, list.size());
		}
	}

	@Test
	public void testCorruptLastByteOfValuesDatFile() throws IOException {
		repo.shutDown();
		File valuesFile = new File(dataDir, "values.dat");
		long fileSize = valuesFile.length();

		overwriteByteInFile(valuesFile, fileSize - 1, 0x0);

		repo.init();

		List<Statement> list = getStatements();
		assertEquals(6, list.size());
	}

	@Test
	public void testCorruptValuesIdFile() throws IOException {
		repo.shutDown();
		File valuesIdFile = new File(dataDir, "values.id");
		long fileSize = valuesIdFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, "values.id");
			overwriteByteInFile(valuesIdFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesHashFile() throws IOException {
		repo.shutDown();
		String file = "values.hash";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesNamespacesFile() throws IOException {
		repo.shutDown();
		String file = "namespaces.dat";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesContextsFile() throws IOException {
		repo.shutDown();
		String file = "contexts.dat";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesPoscAllocFile() throws IOException {
		repo.shutDown();
		String file = "triples-posc.alloc";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesPoscDataFile() throws IOException {
		repo.shutDown();
		String file = "triples-posc.dat";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesSpocAllocFile() throws IOException {
		repo.shutDown();
		String file = "triples-spoc.alloc";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			List<Statement> list = getStatements();
			assertEquals(6, list.size(), "Failed at byte position " + i);
			repo.shutDown();
		}
	}

	@Test
	public void testCorruptValuesSpocDataFile() throws IOException {
		repo.shutDown();
		String file = "triples-spoc.dat";
		File nativeStoreFile = new File(dataDir, file);
		long fileSize = nativeStoreFile.length();

		for (long i = 4; i < fileSize; i++) {
			restoreFile(dataDir, file);
			overwriteByteInFile(nativeStoreFile, i, 0x0);
			repo.init();
			try {
				List<Statement> list = getStatements();
				assertEquals(6, list.size(), "Failed at byte position " + i);
			} catch (Throwable ignored) {
				repo.shutDown();
				nativeStoreFile.delete();
				repo.init();
				List<Statement> list = getStatements();
				assertEquals(6, list.size(), "Failed at byte position " + i);
			}

			repo.shutDown();
		}
	}

	@NotNull
	private List<Statement> getStatements() {
		List<Statement> list = new ArrayList<>();

		try (RepositoryConnection conn = repo.getConnection()) {
			StringWriter stringWriter = new StringWriter();
			RDFWriter writer = Rio.createWriter(RDFFormat.NQUADS, stringWriter);
			conn.export(writer);
			logger.debug(stringWriter.toString());
			try (RepositoryResult<Statement> statements = conn.getStatements(null, null, null, false)) {
				while (statements.hasNext()) {
					Statement next = statements.next();
					list.add(next);
					logger.debug(next.toString());
				}
			}
			return list;
		}
	}

	@AfterEach
	public void after() throws IOException {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		repo.shutDown();
	}
}
