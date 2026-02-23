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
package org.eclipse.rdf4j.sail.s3.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class ManifestTest {

	@TempDir
	Path tempDir;

	@Test
	void roundTrip() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);

		Manifest manifest = new Manifest();
		manifest.setNextValueId(42);
		List<Manifest.SSTableInfo> infos = new ArrayList<>();
		infos.add(new Manifest.SSTableInfo("sstables/L0-1-spoc.sst", 0, "spoc", "0102", "0304", 10, 1));
		infos.add(new Manifest.SSTableInfo("sstables/L0-1-posc.sst", 0, "posc", "0506", "0708", 10, 1));
		manifest.setSstables(infos);

		manifest.save(store, mapper, 1);

		Manifest loaded = Manifest.load(store, mapper);
		assertEquals(1, loaded.getVersion());
		assertEquals(42, loaded.getNextValueId());
		assertEquals(2, loaded.getSstables().size());
		assertEquals("sstables/L0-1-spoc.sst", loaded.getSstables().get(0).getS3Key());
		assertEquals("spoc", loaded.getSstables().get(0).getIndexName());
		assertEquals(10, loaded.getSstables().get(0).getEntryCount());
		assertEquals(1, loaded.getSstables().get(0).getEpoch());
	}

	@Test
	void loadReturnsEmptyManifestWhenNoneExists() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		ObjectMapper mapper = new ObjectMapper();

		Manifest loaded = Manifest.load(store, mapper);
		assertNotNull(loaded);
		assertEquals(0, loaded.getSstables().size());
		assertEquals(0, loaded.getNextValueId());
	}

	@Test
	void multipleVersions() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);

		// Save version 1
		Manifest m1 = new Manifest();
		m1.setNextValueId(10);
		m1.save(store, mapper, 1);

		// Save version 2
		Manifest m2 = new Manifest();
		m2.setNextValueId(20);
		List<Manifest.SSTableInfo> infos = new ArrayList<>();
		infos.add(new Manifest.SSTableInfo("sstables/L0-2-spoc.sst", 0, "spoc", "01", "02", 5, 2));
		m2.setSstables(infos);
		m2.save(store, mapper, 2);

		// Load should return the latest (version 2)
		Manifest loaded = Manifest.load(store, mapper);
		assertEquals(20, loaded.getNextValueId());
		assertEquals(1, loaded.getSstables().size());
	}
}
