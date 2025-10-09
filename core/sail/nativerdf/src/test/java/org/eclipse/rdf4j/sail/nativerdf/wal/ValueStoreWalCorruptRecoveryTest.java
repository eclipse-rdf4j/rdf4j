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

package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.eclipse.rdf4j.sail.nativerdf.datastore.IDFile;
import org.eclipse.rdf4j.sail.nativerdf.model.CorruptValue;
import org.eclipse.rdf4j.sail.nativerdf.model.NativeValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalCorruptRecoveryTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	private boolean previousSoftFail;

	@BeforeEach
	void setUp() {
		previousSoftFail = NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES;
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = true;
	}

	@AfterEach
	void tearDown() {
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = previousSoftFail;
	}

	@Test
	void corruptValueIsRecoveredFromWal() throws Exception {
		Path walDir = tempDir.resolve("wal");
		Files.createDirectories(walDir);
		WalConfig config = WalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.build();

		File valueDir = tempDir.resolve("values").toFile();
		Files.createDirectories(valueDir.toPath());

		String label = "recover-me";
		int id;
		try (ValueStoreWAL wal = ValueStoreWAL.open(config)) {
			try (ValueStore store = new ValueStore(valueDir, false,
					ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
					ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
				Literal lit = VF.createLiteral(label);
				id = store.storeValue(lit);
				var lsn = store.drainPendingWalHighWaterMark();
				assertThat(lsn).isPresent();
				wal.awaitDurable(lsn.getAsLong());
			}
		}

		// Corrupt the first byte (type marker) of the value record in values.dat for this id
		File idFile = new File(valueDir, "values.id");
		File datFile = new File(valueDir, "values.dat");
		try (IDFile ids = new IDFile(idFile)) {
			long offset = ids.getOffset(id);
			try (RandomAccessFile raf = new RandomAccessFile(datFile, "rw")) {
				// skip length (4 bytes), then flip type marker to invalid 0
				raf.seek(offset + 4);
				raf.writeByte(0); // invalid type -> triggers CorruptUnknownValue path
			}
		}

		// Reopen store with WAL enabled and retrieve the value; it should be a CorruptValue with a recovered value
		// attached
		try (ValueStore store = new ValueStore(valueDir, false,
				ValueStore.VALUE_CACHE_SIZE, ValueStore.VALUE_ID_CACHE_SIZE,
				ValueStore.NAMESPACE_CACHE_SIZE, ValueStore.NAMESPACE_ID_CACHE_SIZE,
				ValueStoreWAL.open(config))) {
			NativeValue v = store.getValue(id);
			assertThat(v).isInstanceOf(CorruptValue.class);
			CorruptValue cv = (CorruptValue) v;
			assertThat(cv.getRecovered()).isNotNull();
			assertThat(cv.getRecovered().stringValue()).isEqualTo(label);
		}
	}
}
