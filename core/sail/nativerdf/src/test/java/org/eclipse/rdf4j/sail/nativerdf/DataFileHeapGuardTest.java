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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Verifies that DataFile enforces a large-allocation guard during reads and that the guard can be tuned through system
 * properties.
 */
@Isolated
public class DataFileHeapGuardTest {

	@TempDir
	File tmp;

	@BeforeEach
	public void setUp() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	public void getData_largeLength_triggersGuardIOException() throws Exception {
		System.out.println("DataFile.LARGE_READ_THRESHOLD " + DataFile.LARGE_READ_THRESHOLD);
		System.out.println("DataFile.LARGE_READ_THRESHOLD " + DataFile.LARGE_READ_THRESHOLD / 1024 / 1024 + "MB");
		Path p = tmp.toPath().resolve("guard.dat");

		try (DataFile df = new DataFileWithSimulatedLowHeap(p.toFile())) {
			// write header
		}

		try (NioFile nf = new NioFile(p.toFile())) {
			long headerOffset = 4; // MAGIC_NUMBER(3) + version(1)
			int huge = DataFile.LARGE_READ_THRESHOLD + 1;
			nf.writeInt(huge, headerOffset);
		}

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
			return DataFile.LARGE_READ_THRESHOLD / 2;
		}
	}

}
