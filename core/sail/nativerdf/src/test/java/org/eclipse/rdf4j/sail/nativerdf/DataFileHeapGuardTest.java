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
import java.nio.file.Path;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.datastore.DataFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that DataFile enforces a large-allocation guard during reads when the (test-only) simulation property is
 * enabled. This ensures the guard lives in DataFile rather than in low-level NIO helpers.
 */
public class DataFileHeapGuardTest {

	@TempDir
	File tmp;

	@BeforeEach
	public void setUp() {
		// Ensure soft-fail is disabled so the guard throws instead of capping silently
		System.clearProperty("org.eclipse.rdf4j.sail.nativerdf.softFailOnCorruptDataAndRepairIndexes");
	}

	@Test
	public void getData_largeLength_triggersGuardIOException() throws Exception {
		Path p = tmp.toPath().resolve("guard.dat");

		// Initialize a valid DataFile (writes header)
		try (DataFile df = new DataFileWithSimulatedLowHeap(p.toFile())) {
			// nothing else
		}

		// Manually write a record length > 128MB at the first record offset (immediately after the 4-byte header)
		try (NioFile nf = new NioFile(p.toFile())) {
			long headerOffset = 4; // MAGIC_NUMBER(3) + version(1)
			int huge = 129 * 1024 * 1024; // 129MB
			nf.writeInt(huge, headerOffset);
		}

		// With simulated low heap, DataFile should throw IOException before attempting huge allocation
		try (DataFile df = new DataFileWithSimulatedLowHeap(p.toFile())) {
			assertThrows(IOException.class, () -> df.getData(4));
		}
	}

	static class DataFileWithSimulatedLowHeap extends DataFile {
		public DataFileWithSimulatedLowHeap(File file) throws IOException {
			super(file);
		}

		@Override
		public long getFreeMemory(Runtime rt) {
			return 128 * 1024 * 1024; // 128MB
		}
	}
}
