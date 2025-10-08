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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.File;
import java.io.RandomAccessFile;

import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Tests recovery in DataStore.getData when the stored data length is zero but neighboring ID offsets exist. The
 * recovery uses the next ID's offset to infer the correct data length.
 */
@Isolated
public class DataStoreRecoveryTest {

	@TempDir
	File tempDir;

	@BeforeEach
	public void setup() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;
	}

	@AfterEach
	public void teardown() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	public void recoversDataUsingNextOffsetWhenLengthIsZero() throws Exception {
		DataStore ds = new DataStore(tempDir, "values");

		byte[] d1 = new byte[] { 1, 2, 3, 4, 5 };
		byte[] d2 = new byte[] { 9, 8, 7 };

		int id1 = ds.storeData(d1);
		int id2 = ds.storeData(d2);
		ds.sync();

		// Corrupt the first record's length to zero
		IDFile idFile = new IDFile(new File(tempDir, "values.id"));
		long off1 = idFile.getOffset(id1);
		try (RandomAccessFile raf = new RandomAccessFile(new File(tempDir, "values.dat"), "rw")) {
			raf.seek(off1);
			raf.write(new byte[] { 0, 0, 0, 0 });
		}

		// Now ds.getData(id1) should throw with recovered data
		try {
			ds.getData(id1);
		} catch (RecoveredDataException rde) {
			assertArrayEquals(d1, rde.getData(), "Recovered data should match original bytes");
			return;
		}
		throw new AssertionError("Expected RecoveredDataException to be thrown");
	}
}
