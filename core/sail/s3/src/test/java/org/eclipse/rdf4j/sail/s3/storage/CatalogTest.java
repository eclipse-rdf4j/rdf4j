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
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link Catalog} v3 — flat file list with per-file predicate statistics.
 */
class CatalogTest {

	@TempDir
	Path tempDir;

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void newCatalog_version3() {
		Catalog catalog = new Catalog();
		assertEquals(3, catalog.getVersion());
	}

	@Test
	void addFile_appearsInFileList() {
		Catalog catalog = new Catalog();
		Catalog.ParquetFileInfo info = makeFileInfo("data/L0-00001-spoc.parquet", "spoc", 0, 1);
		catalog.addFile(info);

		assertEquals(1, catalog.getFiles().size());
		assertEquals("data/L0-00001-spoc.parquet", catalog.getFiles().get(0).getS3Key());
	}

	@Test
	void removeFiles_removesMatchingKeys() {
		Catalog catalog = new Catalog();
		catalog.addFile(makeFileInfo("data/L0-00001-spoc.parquet", "spoc", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00001-opsc.parquet", "opsc", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00002-spoc.parquet", "spoc", 0, 2));

		catalog.removeFiles(Set.of("data/L0-00001-spoc.parquet", "data/L0-00001-opsc.parquet"));

		assertEquals(1, catalog.getFiles().size());
		assertEquals("data/L0-00002-spoc.parquet", catalog.getFiles().get(0).getS3Key());
	}

	@Test
	void getFilesForSortOrder_filtersCorrectly() {
		Catalog catalog = new Catalog();
		catalog.addFile(makeFileInfo("data/L0-00001-spoc.parquet", "spoc", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00001-opsc.parquet", "opsc", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00001-cspo.parquet", "cspo", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00002-spoc.parquet", "spoc", 0, 2));

		List<Catalog.ParquetFileInfo> spocFiles = catalog.getFilesForSortOrder("spoc");
		assertEquals(2, spocFiles.size());

		List<Catalog.ParquetFileInfo> opscFiles = catalog.getFilesForSortOrder("opsc");
		assertEquals(1, opscFiles.size());

		List<Catalog.ParquetFileInfo> cspoFiles = catalog.getFilesForSortOrder("cspo");
		assertEquals(1, cspoFiles.size());
	}

	@Test
	void saveAndLoad_roundTrip() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		Catalog catalog = new Catalog();
		catalog.setNextValueId(42);
		catalog.addFile(makeFileInfo("data/L0-00001-spoc.parquet", "spoc", 0, 1));
		catalog.addFile(makeFileInfo("data/L0-00001-opsc.parquet", "opsc", 0, 1));
		catalog.save(store, mapper, 5);

		Catalog loaded = Catalog.load(store, mapper);

		assertEquals(3, loaded.getVersion());
		assertEquals(5, loaded.getEpoch());
		assertEquals(42, loaded.getNextValueId());
		assertEquals(2, loaded.getFiles().size());
	}

	@Test
	void parquetFileInfo_predicateStats() {
		Catalog.ParquetFileInfo info = new Catalog.ParquetFileInfo(
				"data/L0-00001-spoc.parquet", 0, "spoc", 100, 1, 4096,
				1, 50, // subject
				10, 20, // predicate
				5, 40, // object
				0, 99 // context
		);

		assertEquals(10, info.getMinPredicate());
		assertEquals(20, info.getMaxPredicate());
		assertEquals(1, info.getMinSubject());
		assertEquals(50, info.getMaxSubject());
	}

	@Test
	void saveAndLoad_preservesPredicateStats() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		Catalog catalog = new Catalog();
		catalog.addFile(new Catalog.ParquetFileInfo(
				"data/L0-00001-spoc.parquet", 0, "spoc", 100, 1, 4096,
				1, 50, 10, 20, 5, 40, 0, 99));
		catalog.save(store, mapper, 1);

		Catalog loaded = Catalog.load(store, mapper);
		Catalog.ParquetFileInfo info = loaded.getFiles().get(0);

		assertEquals(10, info.getMinPredicate());
		assertEquals(20, info.getMaxPredicate());
		assertEquals(1, info.getMinSubject());
		assertEquals(50, info.getMaxSubject());
		assertEquals(5, info.getMinObject());
		assertEquals(40, info.getMaxObject());
		assertEquals(0, info.getMinContext());
		assertEquals(99, info.getMaxContext());
	}

	@Test
	void loadEmpty_returnsDefaultCatalog() {
		FileSystemObjectStore store = new FileSystemObjectStore(tempDir);
		Catalog loaded = Catalog.load(store, mapper);

		assertEquals(3, loaded.getVersion());
		assertEquals(0, loaded.getEpoch());
		assertTrue(loaded.getFiles().isEmpty());
	}

	private Catalog.ParquetFileInfo makeFileInfo(String s3Key, String sortOrder, int level, long epoch) {
		return new Catalog.ParquetFileInfo(s3Key, level, sortOrder, 10, epoch, 1024,
				1, 100, 1, 100, 1, 100, 0, 100);
	}
}
